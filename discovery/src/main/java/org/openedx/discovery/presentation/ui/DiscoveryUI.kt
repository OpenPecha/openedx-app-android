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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.openedx.discovery.DiscoveryMocks
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.domain.model.Organization
import org.openedx.discovery.presentation.DiscoveryUIState
import org.openedx.discovery.presentation.NativeDiscoveryFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discovery.presentation.component.OrganizationFilterBottomSheet
import org.openedx.foundation.extension.toImageLink
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as сoreR

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
                .error(сoreR.drawable.core_no_image_course)
                .placeholder(сoreR.drawable.core_no_image_course)
                .build(),
            contentDescription = stringResource(
                id = сoreR.string.core_accessibility_header_image_for,
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

    Surface(
        modifier = Modifier
            .testTag("btn_course_card")
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick(course.courseId) }
            .background(MaterialTheme.appColors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(course.media.courseImage?.uri?.toImageLink(apiHostUrl) ?: "")
                    .error(сoreR.drawable.core_no_image_course)
                    .placeholder(сoreR.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(imageWidth)
                    .height(105.dp)
                    .clip(MaterialTheme.appShapes.courseImageShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
            ) {
                Text(
                    modifier = Modifier
                        .testTag("txt_course_org")
                        .padding(top = 12.dp),
                    text = course.org,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.labelMedium
                )
                Text(
                    modifier = Modifier
                        .testTag("txt_course_title")
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = course.name,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleSmall,
                    maxLines = 3,
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
                MaterialTheme.appShapes.material3.medium
            )
            .background(
                MaterialTheme.appColors.surface,
                MaterialTheme.appShapes.material3.medium
            )
            .border(
                1.dp,
                borderColor,
                MaterialTheme.appShapes.material3.medium
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningLabelPreview() {
    OpenEdXTheme {
        WarningLabel(
            painter = painterResource(id = сoreR.drawable.core_ic_offline),
            text = stringResource(id = R.string.discovery_no_internet_label)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    selectedOrganization: Organization?,
    onOrganizationSelected: (Organization?) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showOrganizationFilter by rememberSaveable {
        mutableStateOf(false)
    }
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    if (showOrganizationFilter) {
        ModalBottomSheet(
            onDismissRequest = { showOrganizationFilter = false },
            sheetState = bottomSheetState
        ) {
            OrganizationFilterBottomSheet(
                orgList = organizations,
                isLoading = false,
                selectedOrg = selectedOrganization,
                onClose = { showOrganizationFilter = false },
                onOrgSelected = { organization ->
                    onOrganizationSelected(
                        if (organization.organization == "all") null else organization
                    )
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        containerColor = MaterialTheme.appColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        },
        contentWindowInsets = WindowInsets()
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

        HandleUIMessage(uiMessage = uiMessage, snackbarHostState = snackbarHostState)

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
                        .height(48.dp)
                        .padding(horizontal = 24.dp)
                        .then(searchTabWidth),
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
                            showOrganizationFilter = true
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
                selectedOrganization?.let { selectedOrg ->
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .then(searchTabWidth),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.appColors.primary,
                        border = BorderStroke(1.dp, MaterialTheme.appColors.primary),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                text = selectedOrg.name,
                                color = MaterialTheme.appColors.surface,
                                style = MaterialTheme.appTypography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    onOrganizationSelected(null)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.filter_courses),
                                    tint = MaterialTheme.appColors.surface,
                                    modifier = Modifier.size(20.dp)
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
                PullToRefreshBox(
                    modifier = Modifier.fillMaxWidth(),
                    state = pullToRefreshState,
                    isRefreshing = refreshing,
                    onRefresh = { onSwipeRefresh() }
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
                                LazyColumn(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth),
                                    contentPadding = contentPaddings,
                                    state = scrollState
                                ) {
                                    item {
                                        Column {
                                            if (selectedOrganization != null) {
                                                Text(
                                                    text = pluralStringResource(
                                                        id = R.plurals.discovery_filtered_found_courses,
                                                        count = state.numCourses,
                                                        state.numCourses
                                                    ),
                                                    color = MaterialTheme.appColors.textPrimary,
                                                    style = MaterialTheme.appTypography.titleMedium
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
                                    items(state.courses) { course ->
                                        DiscoveryCourseItem(
                                            apiHostUrl = apiHostUrl,
                                            course = course,
                                            windowSize = windowSize,
                                            onClick = {
                                                onItemClick(course)
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                    item {
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
                                if (scrollState.shouldLoadMore(
                                        firstVisibleIndex,
                                        LOAD_MORE_THRESHOLD
                                    )
                                ) {
                                    paginationCallback()
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme {
        DiscoveryCourseItem(
            apiHostUrl = "",
            course = DiscoveryMocks.course,
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
                courses = DiscoveryMocks.courses(1),
                numCourses = 1
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
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false,
            organizations = emptyList(),
            selectedOrganization = null,
            onOrganizationSelected = {},
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
                courses = DiscoveryMocks.courses(1),
                numCourses = 1
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
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false,
            organizations = emptyList(),
            selectedOrganization = null,
            onOrganizationSelected = {},
        )
    }
}
