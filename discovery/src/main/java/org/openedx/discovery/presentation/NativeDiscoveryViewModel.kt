package org.openedx.discovery.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.discovery.data.repository.DiscoveryRepository
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.domain.model.Organization
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager

class NativeDiscoveryViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DiscoveryInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscoveryAnalytics,
    private val corePreferences: CorePreferences,
    private val repository: DiscoveryRepository,
) : BaseViewModel(resourceManager) {

    val apiHostUrl get() = config.getApiHostURL()
    val isUserLoggedIn get() = corePreferences.user != null
    val canShowBackButton get() = config.isPreLoginExperienceEnabled() && !isUserLoggedIn
    val isRegistrationEnabled: Boolean get() = config.isRegistrationEnabled()

    private val _organizations = MutableLiveData<List<Organization>>(emptyList())
    val organizations: LiveData<List<Organization>>
        get() = _organizations

    private val _selectedOrganization = MutableLiveData<Organization?>(null)
    val selectedOrganization: LiveData<Organization?>
        get() = _selectedOrganization

    private val _uiState = MutableLiveData<DiscoveryUIState>(DiscoveryUIState.Loading)
    val uiState: LiveData<DiscoveryUIState>
        get() = _uiState

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private var page = 1
    private val coursesList = mutableListOf<Course>()
    private var isLoading = false
    private var currentOrganization: String? = null

    init {
        getCoursesList()
        fetchOrganizations()
    }

    private fun loadCoursesInternal(
        username: String? = null,
        organization: String? = null
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getCoursesList(username, organization, page)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _canLoadMore.value = true
                        page++
                    } else {
                        _canLoadMore.value = false
                        page = -1
                    }
                    coursesList.addAll(response.results)
                } else {
                    val cachedList = interactor.getCoursesListFromCache()
                    _canLoadMore.value = false
                    page = -1
                    coursesList.addAll(cachedList)
                }
                _uiState.value = DiscoveryUIState.Courses(ArrayList(coursesList))
            } catch (e: Exception) {
                handleErrorUiMessage(
                    throwable = e,
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun getCoursesList(
        username: String? = null,
        organization: String? = null
    ) {
        currentOrganization = organization
        _uiState.value = DiscoveryUIState.Loading
        coursesList.clear()
        loadCoursesInternal(username, organization)
    }

    fun updateData(
        username: String? = null,
        organization: String? = null
    ) {
        viewModelScope.launch {
            try {
                val requestedOrganization = organization ?: currentOrganization
                _isUpdating.value = true
                isLoading = true
                page = 1
                val response = interactor.getCoursesList(username, requestedOrganization, page)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.results)
                _uiState.value = DiscoveryUIState.Courses(ArrayList(coursesList))
            } catch (e: Exception) {
                handleErrorUiMessage(
                    throwable = e,
                )
            } finally {
                isLoading = false
                _isUpdating.value = false
            }
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            loadCoursesInternal(organization = currentOrganization)
        }
    }

    fun discoverySearchBarClickedEvent() {
        analytics.discoverySearchBarClickedEvent()
    }

    fun discoveryCourseClicked(courseId: String, courseName: String) {
        analytics.discoveryCourseClickedEvent(courseId, courseName)
    }

    fun courseDetailClickedEvent(courseId: String, courseTitle: String) {
        val event = DiscoveryAnalyticsEvent.COURSE_INFO
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
                put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
                put(DiscoveryAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(DiscoveryAnalyticsKey.CATEGORY.key, DiscoveryAnalyticsKey.DISCOVERY.key)
            }
        )
    }

    fun fetchOrganizations() {
        viewModelScope.launch {
            try {
                _organizations.value = repository.getOrganizations()
            } catch (e: Exception) {
                handleErrorUiMessage(
                    throwable = e,
                )
            }
        }
    }

    fun searchCoursesByOrganization(organization: Organization?) {
        _selectedOrganization.value = organization
        currentOrganization = organization?.organization
        page = 1
        coursesList.clear()
        getCoursesList(organization = currentOrganization)
    }

    fun refreshCourses() {
        updateData(organization = currentOrganization)
    }
}
