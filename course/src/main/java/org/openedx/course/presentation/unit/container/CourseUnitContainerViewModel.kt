package org.openedx.course.presentation.unit.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openedx.core.BlockType
import org.openedx.core.config.Config
import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSectionChanged
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.foundation.extension.clearAndAddAll
import org.openedx.foundation.extension.indexOfFirstFromIndex
import org.openedx.foundation.presentation.BaseViewModel

class CourseUnitContainerViewModel(
    val courseId: String,
    val unitId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
    private val networkConnection: NetworkConnection,
) : BaseViewModel() {

    private val blocks = ArrayList<Block>()

    private var isRefreshing = false

    // Track when we're viewing locked content due to prerequisites
    private var isViewingLockedContent = false
    private var trackedPrereqId: String? = null
    private var wasPrereqIncomplete = false

    val isCourseExpandableSectionsEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    val isCourseUnitProgressEnabled get() = config.getCourseUIConfig().isCourseUnitProgressEnabled

    private var currentIndex = 0
    private var currentVerticalIndex = 0
    private var currentSectionIndex = -1

    val isFirstIndexInContainer: Boolean
        get() {
            return _descendantsBlocks.value.firstOrNull() == _descendantsBlocks.value.getOrNull(currentIndex)
        }

    val isLastIndexInContainer: Boolean
        get() {
            return _descendantsBlocks.value.lastOrNull() == _descendantsBlocks.value.getOrNull(currentIndex)
        }

    private val _verticalBlockCounts = MutableLiveData<Int>()
    val verticalBlockCounts: LiveData<Int>
        get() = _verticalBlockCounts

    private val _indexInContainer = MutableLiveData<Int>()
    val indexInContainer: LiveData<Int>
        get() = _indexInContainer

    private val _unitsListShowed = MutableLiveData<Boolean>()
    val unitsListShowed: LiveData<Boolean>
        get() = _unitsListShowed

    private val _subSectionUnitBlocks = MutableStateFlow<List<Block>>(listOf())
    val subSectionUnitBlocks = _subSectionUnitBlocks.asStateFlow()

    var nextButtonText = ""
    var hasNextBlock = false

    private var currentMode: CourseViewMode? = null
    val mode: CourseViewMode
        get() = currentMode ?: CourseViewMode.FULL

    private var currentComponentId = ""
    private var courseName = ""

    private val _descendantsBlocks = MutableStateFlow<List<Block>>(listOf())
    val descendantsBlocks = _descendantsBlocks.asStateFlow()

    val hasNetworkConnection: Boolean
        get() = networkConnection.isOnline()

    fun loadBlocks(mode: CourseViewMode, componentId: String = "", forceRefresh: Boolean = false) {
        currentMode = mode
        viewModelScope.launch {
            try {
                // First, check if we need to force refresh for prerequisite-gated content
                var shouldForceRefresh = forceRefresh

                if (!forceRefresh) {
                    // Check if the unit we're loading has gatedContent (prerequisite-related)
                    // If so, always refresh to get the latest lock status
                    val preliminaryStructure = when (mode) {
                        CourseViewMode.FULL -> interactor.getCourseStructure(courseId, isNeedRefresh = false)
                        CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos(courseId)
                    }

                    val targetBlock = preliminaryStructure.blockData.firstOrNull { it.id == unitId }

                    // Check if this block or its first descendant has prerequisite gatedContent
                    val hasGatedContent = targetBlock?.gatedContent != null
                    val firstDescendant = if (targetBlock?.descendants?.isNotEmpty() == true) {
                        preliminaryStructure.blockData.firstOrNull { it.id == targetBlock.descendants.first() }
                    } else null
                    val firstDescHasGatedContent = firstDescendant?.gatedContent != null

                    if (hasGatedContent || firstDescHasGatedContent) {
                        // Force refresh to ensure we have the latest lock status
                        shouldForceRefresh = true
                    }
                }

                val courseStructure = when (mode) {
                    CourseViewMode.FULL -> interactor.getCourseStructure(courseId, isNeedRefresh = shouldForceRefresh)
                    CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos(courseId)
                }
                val blocks = courseStructure.blockData
                courseName = courseStructure.name
                this@CourseUnitContainerViewModel.blocks.clearAndAddAll(blocks)
                setupCurrentIndex(componentId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshCourseData() {
        currentMode?.let { mode ->
            // Reset the current section index so setupCurrentIndex will run properly
            isRefreshing = true
            currentSectionIndex = -1
            loadBlocks(mode, currentComponentId, forceRefresh = true)
        }
    }

    /**
     * Check if the tracked prerequisite has been completed.
     * Only performs API call if we were tracking a prerequisite that was incomplete.
     * Returns true if prerequisite completion status changed from incomplete to complete.
     */
    suspend fun shouldRefreshForPrerequisiteCompletion(): Boolean {
        // Only check if we were actually viewing locked content with a tracked prerequisite
        if (!isViewingLockedContent || trackedPrereqId == null || !wasPrereqIncomplete) {
            return false
        }

        try {
            // Fetch fresh data to check completion
            val courseStructure = when (currentMode) {
                CourseViewMode.FULL -> interactor.getCourseStructure(courseId, isNeedRefresh = true)
                CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos(courseId)
                else -> return false
            }

            // Find the prerequisite subsection in fresh data
            val prereqBlock = courseStructure.blockData.firstOrNull { it.id == trackedPrereqId }

            // Check if it's now complete (completion = 1.0 means all units done)
            val isNowComplete = prereqBlock?.completion == 1.0

            // Return true only if it changed from incomplete to complete
            return isNowComplete && wasPrereqIncomplete
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    init {
        _indexInContainer.value = 0

        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseStructureUpdated) {
                    if (event.courseId != courseId) return@collect

                    currentMode?.let { loadBlocks(it, currentComponentId) }
                    val blockId = blocks[currentVerticalIndex].id
                    _subSectionUnitBlocks.value =
                        getSubSectionUnitBlocks(blocks, getSubSectionId(blockId))
                }
            }
        }
    }

    private fun setupCurrentIndex(componentId: String = "") {
        if (currentSectionIndex != -1 && !isRefreshing) return
        isRefreshing = false
        currentComponentId = componentId

        blocks.forEachIndexed { index, block ->
            if (block.id == unitId) {
                currentVerticalIndex = index
                currentSectionIndex = blocks.indexOfFirst {
                    it.descendants.contains(blocks[currentVerticalIndex].id)
                }
                val blockGatedContent = block.gatedContent
                val isBlockGatedWithPrereq = blockGatedContent?.gated == true &&
                                             blockGatedContent.prereqId.isNotEmpty()

                // Mark if we're viewing locked content (will be used to decide if we need to check completion later)
                if (isBlockGatedWithPrereq && blockGatedContent != null) {
                    isViewingLockedContent = true
                    trackedPrereqId = blockGatedContent.prereqId
                    val prereqBlock = blocks.firstOrNull { it.id == blockGatedContent.prereqId }
                    wasPrereqIncomplete = prereqBlock?.completion != 1.0
                } else {
                    isViewingLockedContent = false
                    trackedPrereqId = null
                    wasPrereqIncomplete = false
                }

                val firstDescendant = if (!isBlockGatedWithPrereq && block.descendants.isNotEmpty()) {
                    blocks.firstOrNull { it.id == block.descendants.first() }
                } else null

                val firstDescGatedContent = firstDescendant?.gatedContent
                val firstDescGatedWithPrereq = firstDescGatedContent?.gated == true &&
                                               firstDescGatedContent.prereqId.isNotEmpty()

                // Also check first descendant for locked content
                if (firstDescGatedWithPrereq && !isViewingLockedContent && firstDescGatedContent != null) {
                    isViewingLockedContent = true
                    trackedPrereqId = firstDescGatedContent.prereqId
                    val prereqBlock = blocks.firstOrNull { it.id == firstDescGatedContent.prereqId }
                    wasPrereqIncomplete = prereqBlock?.completion != 1.0
                }

                if (block.descendants.isNotEmpty() || block.isGated()) {
                    _descendantsBlocks.value =
                        block.descendants.mapNotNull { descendant ->
                            blocks.firstOrNull { descendant == it.id }
                        }
                    _subSectionUnitBlocks.value =
                        getSubSectionUnitBlocks(blocks, getSubSectionId(unitId))

                    when {
                        _descendantsBlocks.value.isEmpty() || isBlockGatedWithPrereq -> {
                            _descendantsBlocks.value = listOf(block)
                        }
                        firstDescGatedWithPrereq -> {
                            _descendantsBlocks.value = listOfNotNull(firstDescendant)
                        }
                    }
                } else {
                    setNextVerticalIndex()
                }
                if (currentVerticalIndex != -1) {
                    _verticalBlockCounts.value = blocks[currentVerticalIndex].descendants.size
                }
                if (componentId.isNotEmpty()) {
                    currentIndex = _descendantsBlocks.value.indexOfFirst { it.id == componentId }
                    _indexInContainer.value = currentIndex
                }
                return
            }
        }
    }

    private fun getSubSectionId(blockId: String): String {
        return blocks.firstOrNull { it.descendants.contains(blockId) }?.id ?: ""
    }

    private fun getSubSectionUnitBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = blocks.first { it.id == id }

        for (descendant in selectedBlock.descendants) {
            val blockDescendant = blocks.find {
                it.id == descendant
            }
            if (blockDescendant != null) {
                if (blockDescendant.type == BlockType.VERTICAL) {
                    resultList.add(blockDescendant.copy(type = getUnitType(blockDescendant.descendants)))
                }
            } else {
                continue
            }
        }
        return resultList
    }

    private fun getUnitType(descendant: List<String>): BlockType {
        val descendantBlocks = blocks.filter { descendant.contains(it.id) }

        return when {
            descendantBlocks.any { it.isProblemBlock } -> BlockType.PROBLEM
            descendantBlocks.any { it.isVideoBlock } -> BlockType.VIDEO
            descendantBlocks.any { it.isDiscussionBlock } -> BlockType.DISCUSSION
            else -> BlockType.OTHERS
        }
    }

    private fun setNextVerticalIndex() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
    }

    fun proceedToNext() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        if (currentVerticalIndex != -1) {
            val sectionIndex = blocks.indexOfFirst {
                it.descendants.contains(blocks[currentVerticalIndex].id)
            }
            if (sectionIndex != currentSectionIndex) {
                currentSectionIndex = sectionIndex
                blocks.getOrNull(currentSectionIndex)?.id?.let {
                    sendCourseSectionChanged(it)
                }
            }
        }
    }

    fun getDownloadModelById(id: String): DownloadModel? = runBlocking(Dispatchers.IO) {
        return@runBlocking interactor.getDownloadModels().first()
            .find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED }
    }

    fun getCurrentBlock(): Block {
        return blocks[currentIndex]
    }

    fun moveToNextBlock(): Block? {
        return moveToBlock(currentIndex + 1)
    }

    fun moveToPrevBlock(): Block? {
        return moveToBlock(currentIndex - 1)
    }

    private fun moveToBlock(index: Int): Block? {
        _descendantsBlocks.value.getOrNull(index)?.let { block ->
            currentIndex = index
            if (currentVerticalIndex != -1) {
                _indexInContainer.value = currentIndex
            }
            return block
        }
        return null
    }

    private fun sendCourseSectionChanged(blockId: String) {
        viewModelScope.launch {
            notifier.send(CourseSectionChanged(blockId))
        }
    }

    fun getCurrentVerticalBlock(): Block? = blocks.getOrNull(currentVerticalIndex)

    fun getNextVerticalBlock(): Block? {
        val index = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        return blocks.getOrNull(index)
    }

    fun getUnitBlocks(): List<Block> = _descendantsBlocks.value

    fun getSubSectionBlock(unitId: String): Block {
        return blocks.first { it.descendants.contains(unitId) }
    }

    fun courseUnitContainerShowedEvent() {
        analytics.logEvent(
            CourseAnalyticsEvent.UNIT_DETAIL.eventName,
            buildMap {
                put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.UNIT_DETAIL.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseName)
                put(CourseAnalyticsKey.BLOCK_ID.key, unitId)
            }
        )
    }

    fun nextBlockClickedEvent(blockId: String, blockName: String) {
        analytics.nextBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun prevBlockClickedEvent(blockId: String, blockName: String) {
        analytics.prevBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalNextClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalNextClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalBackClickedEvent() {
        analytics.finishVerticalBackClickedEvent(courseId, courseName)
    }

    fun setUnitsListVisibility(isVisible: Boolean) {
        _unitsListShowed.value = isVisible
    }
}
