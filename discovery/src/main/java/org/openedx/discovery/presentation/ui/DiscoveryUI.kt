package org.openedx.discovery.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.openedx.core.AppUpdateState
import org.openedx.core.domain.model.Media
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRecommendedBox
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.StaticSearchBar
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.data.model.Organization
import org.openedx.discovery.presentation.DiscoveryUIState
import org.openedx.discovery.presentation.NativeDiscoveryFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discovery.presentation.component.OrganizationFilterBottomSheet
import org.openedx.foundation.extension.toImageLink
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as CoreR

@Composable
fun ImageHeader(
    modifier: Modifier,
    apiHostUrl: String,
    courseImage: String?,
    courseName: String,
) {
    val configuration = LocalConfiguration.current
    val windowSize = rememberWindowSize()
    val contentScale =
        if (!windowSize.isTablet && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(courseImage?.toImageLink(apiHostUrl))
                .error(CoreR.drawable.core_no_image_course)
                .placeholder(CoreR.drawable.core_no_image_course)
                .build(),
            contentDescription = stringResource(
                id = CoreR.string.core_accessibility_header_image_for,
                courseName
            ),
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.appShapes.cardShape)
        )
    }
}

@Composable
fun DiscoveryCourseItem(
    apiHostUrl: String,
    course: Course,
    windowSize: WindowSize,
    onClick: (String) -> Unit,
) {
    val imageWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = 170.dp,
                compact = 105.dp
            )
        )
    }

    // Workaround: Tibetan script line wrapping issue in Jetpack Compose.
    // Adding a trailing newline forces proper height calculation and prevents clipping.
    val adjustedCourseTitle = course.name + "\n"

    val durationText = if (course.duration.isBlank()) {
        stringResource(id = R.string.discovery_course_duration_unspecified)
    } else {
        stringResource(id = R.string.discovery_course_duration_specified, course.duration)
    }

    // Height for course title
    val lineHeightSp = MaterialTheme.appTypography.titleSmall.lineHeight
//    val lineHeight = with(LocalDensity.current) { lineHeightSp.toDp() }
    val lineHeight = 22.dp

    Surface(
        modifier = Modifier
            .testTag("btn_course_card")
            .fillMaxWidth()
            .height(230.dp)
            .clickable { onClick(course.courseId) },
        shape = MaterialTheme.appShapes.cardShape,
        elevation = 4.dp,
        color = MaterialTheme.appColors.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(course.media.courseImage?.uri?.toImageLink(apiHostUrl) ?: "")
                    .error(org.openedx.core.R.drawable.core_no_image_course)
                    .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(MaterialTheme.appShapes.courseImageShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    modifier = Modifier.testTag("txt_course_org"),
                    text = course.org,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.labelMedium,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .testTag("txt_course_title")
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(lineHeight * 3),
                    text = adjustedCourseTitle,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleSmall,
                    maxLines = 3,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("txt_course_duration"),
                    text = durationText,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.labelMedium,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WarningLabel(
    painter: Painter,
    text: String
) {
    val borderColor = if (!isSystemInDarkTheme()) {
        MaterialTheme.appColors.cardViewBorder
    } else {
        MaterialTheme.appColors.surface
    }
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                0.dp,
                MaterialTheme.appShapes.material.medium
            )
            .background(
                MaterialTheme.appColors.surface,
                MaterialTheme.appShapes.material.medium
            )
            .border(
                1.dp,
                borderColor,
                MaterialTheme.appShapes.material.medium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.appColors.warning
            )
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.testTag("txt_enroll_internet_error"),
                text = text,
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.titleSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun DiscoveryScreen(
    windowSize: WindowSize,
    state: DiscoveryUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    canLoadMore: Boolean,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    canShowBackButton: Boolean,
    isUserLoggedIn: Boolean,
    isRegistrationEnabled: Boolean,
    appUpgradeParameters: AppUpdateState.AppUpgradeParameters,
    onSearchClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onReloadClick: () -> Unit,
    paginationCallback: () -> Unit,
    onItemClick: (Course) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    organizations: List<Organization>,
    selectedOrg: Organization?,
    onOrgSelected: (Organization) -> Unit,
    onClearOrgClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyGridState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = onSwipeRefresh)

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            OrganizationFilterBottomSheet(
                orgList = organizations,
                isLoading = false,
                selectedOrg = selectedOrg,
                onClose = {
                    coroutineScope.launch { sheetState.hide() }
                },
                onOrgSelected = { selected ->
                    onOrgSelected(selected)  // This calls the ViewModel's setSelectedOrg() from the Fragment
                },
            )
        }
    ) {
        Scaffold(
            topBar = {
            },
            scaffoldState = scaffoldState,
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId = true
                },
            backgroundColor = MaterialTheme.appColors.background,
            bottomBar = {
                if (!isUserLoggedIn) {
                    Box(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 32.dp,
                            )
                            .navigationBarsPadding()
                    ) {
                        AuthButtonsPanel(
                            onRegisterClick = onRegisterClick,
                            onSignInClick = onSignInClick,
                            showRegisterButton = isRegistrationEnabled
                        )
                    }
                }
            }
        ) {

            val searchTabWidth by remember(key1 = windowSize) {
                mutableStateOf(
                    windowSize.windowSizeValue(
                        expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                        compact = Modifier.fillMaxWidth()
                    )
                )
            }

            val contentWidth by remember(key1 = windowSize) {
                mutableStateOf(
                    windowSize.windowSizeValue(
                        expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                        compact = Modifier.fillMaxWidth()
                    )
                )
            }

            val contentPaddings by remember(key1 = windowSize) {
                mutableStateOf(
                    windowSize.windowSizeValue(
                        expanded = PaddingValues(
                            top = 32.dp,
                            bottom = 40.dp
                        ),
                        compact = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
                    )
                )
            }

            HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

            if (canShowBackButton) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn(
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.appColors.primary
                    ) {
                        onBackClick()
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Toolbar(
                        label = stringResource(id = R.string.discovery_Discovery),
                        canShowBackBtn = canShowBackButton,
                        canShowSettingsIcon = !canShowBackButton,
                        onBackClick = onBackClick,
                        onSettingsClick = onSettingsClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .then(searchTabWidth)
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StaticSearchBar(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                onSearchClick()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_filter),
                                contentDescription = stringResource(id = R.string.filter_courses),
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.appColors.textPrimary
                            )
                        }
                    }

                    if (selectedOrg != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Pill with Organization Name
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.appColors.primary,
                                border = BorderStroke(1.dp, MaterialTheme.colors.primary),
                                elevation = 2.dp,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .heightIn(min = 40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(start = 16.dp, end = 16.dp)
                                ) {
                                    Text(
                                        text = selectedOrg.organization,
                                        color = MaterialTheme.appColors.surface,
                                        style = MaterialTheme.appTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            // Right Pill with "Clear" Text
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.appColors.surface,
                                border = BorderStroke(1.dp, MaterialTheme.appColors.textFieldBorder),
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .heightIn(min = 40.dp)
                                    .clickable {
                                        onClearOrgClick()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        stringResource(id = R.string.clear),
                                        color = MaterialTheme.appColors.textFieldHint,
                                        style = MaterialTheme.appTypography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pullRefresh(pullRefreshState)
                    ) {
                        when (state) {
                            is DiscoveryUIState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is DiscoveryUIState.Courses -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .then(contentWidth),
                                        contentPadding = contentPaddings,
                                        state = scrollState,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item(span = { GridItemSpan(2) }) {
                                            Column {
                                                if (selectedOrg != null) {
                                                    Text(
                                                        text = pluralStringResource(
                                                            id = R.plurals.discovery_filtered_found_courses,
                                                            count = state.numCourses,
                                                            formatArgs = arrayOf(state.numCourses)
                                                        ),
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        style = MaterialTheme.appTypography.titleLarge
                                                    )
                                                } else {
                                                    Text(
                                                        modifier = Modifier.testTag("txt_discovery_new"),
                                                        text = stringResource(id = R.string.discovery_discovery_new),
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        style = MaterialTheme.appTypography.displaySmall
                                                    )
                                                    Text(
                                                        modifier = Modifier
                                                            .testTag("txt_discovery_lets_find")
                                                            .padding(top = 4.dp),
                                                        text = stringResource(id = R.string.discovery_lets_find),
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        style = MaterialTheme.appTypography.titleSmall
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                            }
                                        }
                                        items(
                                            count = state.courses.size,
                                            key = { index -> "${state.courses[index].courseId}_$index" }
                                        ) { index ->
                                            val course = state.courses[index]
                                            DiscoveryCourseItem(
                                                apiHostUrl = apiHostUrl,
                                                course = course,
                                                windowSize = windowSize,
                                                onClick = { onItemClick(course) }
                                            )
                                        }
                                        item(span = { GridItemSpan(2) }) {
                                            if (canLoadMore) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                                }
                                            }
                                        }
                                    }
                                    if (scrollState.shouldLoadMore(firstVisibleIndex, LOAD_MORE_THRESHOLD)) {
                                        paginationCallback()
                                    }
                                }
                            }
                        }
                        PullRefreshIndicator(
                            refreshing,
                            pullRefreshState,
                            Modifier.align(Alignment.TopCenter)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            when (appUpgradeParameters.appUpgradeEvent) {
                                is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                                    if (appUpgradeParameters.wasUpdateDialogClosed) {
                                        AppUpgradeRecommendedBox(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = appUpgradeParameters.onAppUpgradeRecommendedBoxClick
                                        )
                                    } else {
                                        if (!AppUpdateState.wasUpdateDialogDisplayed) {
                                            AppUpdateState.wasUpdateDialogDisplayed = true
                                            appUpgradeParameters.appUpgradeRecommendedDialog()
                                        }
                                    }
                                }

                                is AppUpgradeEvent.UpgradeRequiredEvent -> {
                                    if (!AppUpdateState.wasUpdateDialogDisplayed) {
                                        AppUpdateState.wasUpdateDialogDisplayed = true
                                        appUpgradeParameters.onAppUpgradeRequired()
                                    }
                                }

                                else -> {}
                            }
                            if (!isInternetConnectionShown && !hasInternetConnection) {
                                OfflineModeDialog(
                                    Modifier
                                        .fillMaxWidth(),
                                    onDismissCLick = {
                                        isInternetConnectionShown = true
                                    },
                                    onReloadClick = {
                                        isInternetConnectionShown = true
                                        onReloadClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme {
        DiscoveryCourseItem(
            apiHostUrl = "",
            course = mockCourse,
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscoveryScreenPreview() {
    OpenEdXTheme {
        DiscoveryScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = DiscoveryUIState.Courses(
                courses = listOf(
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                ),
                numCourses = 10
            ),
            uiMessage = null,
            apiHostUrl = "",
            onSearchClick = {},
            paginationCallback = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            canLoadMore = false,
            refreshing = false,
            hasInternetConnection = true,
            isUserLoggedIn = false,
            isRegistrationEnabled = true,
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false,
            organizations = listOf(),
            selectedOrg = null,
            onOrgSelected = {},
            onClearOrgClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun DiscoveryScreenTabletPreview() {
    OpenEdXTheme {
        DiscoveryScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            state = DiscoveryUIState.Courses(
                courses = listOf(
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                ),
                numCourses = 10
            ),
            uiMessage = null,
            apiHostUrl = "",
            onSearchClick = {},
            paginationCallback = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            canLoadMore = false,
            refreshing = false,
            hasInternetConnection = true,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false,
            organizations = listOf(),
            selectedOrg = null,
            onOrgSelected = {},
            onClearOrgClick = {},
        )
    }
}

private val mockCourse = Course(
    id = "id",
    blocksUrl = "blocksUrl",
    courseId = "courseId",
    effort = "effort",
    enrollmentStart = null,
    enrollmentEnd = null,
    hidden = false,
    invitationOnly = false,
    media = Media(),
    mobileAvailable = true,
    name = "Test course",
    number = "number",
    org = "EdX",
    pacing = "pacing",
    shortDescription = "shortDescription",
    start = "start",
    end = "end",
    startDisplay = "startDisplay",
    startType = "startType",
    overview = "",
    isEnrolled = false,
    duration = "30 Days"
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningLabelPreview() {
    OpenEdXTheme {
        WarningLabel(
            painter = painterResource(id = CoreR.drawable.core_ic_offline),
            text = stringResource(id = R.string.discovery_no_internet_label)
        )
    }
}
