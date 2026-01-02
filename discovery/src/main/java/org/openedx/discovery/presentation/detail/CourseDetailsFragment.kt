package org.openedx.discovery.presentation.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.model.Media
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.isPreview
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.EmailUtil
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.ui.ImageHeader
import org.openedx.discovery.presentation.ui.WarningLabel
import org.openedx.foundation.extension.applyDarkModeIfEnabled
import org.openedx.foundation.extension.isEmailValid
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import java.nio.charset.StandardCharsets
import java.util.Date
import org.openedx.core.R as CoreR

class CourseDetailsFragment : Fragment() {

    private val viewModel by viewModel<CourseDetailsViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<DiscoveryRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()

                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value

                CourseDetailsScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    htmlBody = viewModel.getCourseAboutBody(
                        colorBackgroundValue,
                        colorTextValue
                    ),
                    hasInternetConnection = viewModel.hasInternetConnection,
                    isUserLoggedIn = viewModel.isUserLoggedIn,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    onReloadClick = {
                        viewModel.getCourseDetail()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onButtonClick = {
                        val currentState = uiState
                        if (currentState is CourseDetailsUIState.CourseData) {
                            when {
                                (!currentState.isUserLoggedIn) -> {
                                    router.navigateToSignIn(
                                        parentFragmentManager,
                                        currentState.course.courseId,
                                        null
                                    )
                                }

                                currentState.course.isEnrolled -> {
                                    router.navigateToCourseOutline(
                                        requireActivity().supportFragmentManager,
                                        currentState.course.courseId,
                                        currentState.course.name,
                                    )
                                }

                                else -> {
                                    viewModel.enrollInACourse(
                                        currentState.course.courseId,
                                        currentState.course.name
                                    )
                                }
                            }
                        }
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager, viewModel.courseId, null)
                    },
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager, viewModel.courseId, null)
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        fun newInstance(courseId: String): CourseDetailsFragment {
            val fragment = CourseDetailsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CourseDetailsScreen(
    windowSize: WindowSize,
    uiState: CourseDetailsUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    htmlBody: String,
    hasInternetConnection: Boolean,
    isUserLoggedIn: Boolean,
    isRegistrationEnabled: Boolean,
    onReloadClick: () -> Unit,
    onBackClick: () -> Unit,
    onButtonClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    val isInternetConnectionShown = rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if (!isUserLoggedIn) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) {
                    AuthButtonsPanel(
                        onRegisterClick = onRegisterClick,
                        onSignInClick = onSignInClick,
                        showRegisterButton = isRegistrationEnabled
                    )
                }
            }
        }
    ) { paddingValues ->
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == ORIENTATION_PORTRAIT) {
                        Modifier.widthIn(Dp.Unspecified, 560.dp)
                    } else {
                        Modifier.widthIn(Dp.Unspecified, 650.dp)
                    },
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
            ) {
                Toolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    label = stringResource(id = R.string.discovery_course_details),
                    canShowBackBtn = true,
                    onBackClick = onBackClick
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (uiState) {
                        is CourseDetailsUIState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseDetailsUIState.CourseData -> {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                if (configuration.orientation == ORIENTATION_LANDSCAPE && windowSize.isTablet) {
                                    CourseDetailNativeContentLandscape(
                                        windowSize = windowSize,
                                        apiHostUrl = apiHostUrl,
                                        hasInternetConnection = hasInternetConnection,
                                        isInternetConnectionShown = isInternetConnectionShown,
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                } else {
                                    CourseDetailNativeContent(
                                        windowSize = windowSize,
                                        apiHostUrl = apiHostUrl,
                                        hasInternetConnection = hasInternetConnection,
                                        isInternetConnectionShown = isInternetConnectionShown,
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                }

                                // About this Course (with short description)
                                if (uiState.course.shortDescription.isNotBlank()) {
                                    CourseInfoSection(
                                        icon = Icons.Default.Info,
                                        title = stringResource(id = R.string.core_about_this_course),
                                        content = uiState.course.shortDescription,
                                        modifier = Modifier.testTag("section_about_course")
                                    )
                                }

                                // Course Description
                                if (uiState.course.description.isNotBlank()) {
                                    CourseInfoSection(
                                        icon = Icons.Default.Description,
                                        title = stringResource(id = R.string.core_course_description),
                                        content = uiState.course.description,
                                        modifier = Modifier.testTag("section_course_description")
                                    )
                                }

                                // Course Overview (HTML WebView)
                                // Only show if overview has actual content (not just empty HTML/entities)
                                if (uiState.course.overview.isNotBlank() &&
                                    uiState.course.overview
                                        .replace(Regex("<[^>]*>"), "") // Remove HTML tags
                                        .replace("&nbsp;", " ") // Replace non-breaking spaces
                                        .replace("&amp;", "&") // Replace ampersands
                                        .replace(Regex("\\s+"), "") // Remove all whitespace
                                        .isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                                contentDescription = null,
                                                tint = MaterialTheme.appColors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(id = R.string.core_course_overview),
                                                style = MaterialTheme.appTypography.titleSmall,
                                                color = MaterialTheme.appColors.textFieldHint
                                            )
                                        }

                                        if (isPreview) {
                                            Text(
                                                text = htmlBody,
                                                modifier = Modifier.testTag("txt_course_overview"),
                                            )
                                        } else {
                                            var webViewAlpha by remember { mutableFloatStateOf(0f) }
                                            if (webViewAlpha == 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .alpha(webViewAlpha),
                                                color = MaterialTheme.appColors.background
                                            ) {
                                                CourseDescription(
                                                    modifier = Modifier,
                                                    apiHostUrl = apiHostUrl,
                                                    body = htmlBody,
                                                    onWebPageLoaded = {
                                                        webViewAlpha = 1f
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Course Requirements
                                if (uiState.course.courseRequirement.isNotBlank()) {
                                    CourseInfoSection(
                                        icon = Icons.AutoMirrored.Filled.Rule,
                                        title = stringResource(id = R.string.core_course_requirements),
                                        content = uiState.course.courseRequirement,
                                        modifier = Modifier.testTag("section_course_requirements")
                                    )
                                }

                                // Learning Outcomes
                                if (uiState.course.learningOutcomes.isNotBlank()) {
                                    CourseInfoSection(
                                        icon = Icons.Default.TrackChanges,
                                        title = stringResource(id = R.string.core_learning_outcomes),
                                        content = uiState.course.learningOutcomes,
                                        modifier = Modifier.testTag("section_learning_outcomes")
                                    )
                                }

                                // Instructors Section
                                if (uiState.course.instructorsList.isNotEmpty()) {
                                    InstructorsSection(
                                        instructors = uiState.course.instructorsList,
                                        modifier = Modifier.testTag("section_instructors")
                                    )
                                }
                            }
                        }
                    }
                    if (!isInternetConnectionShown.value && !hasInternetConnection) {
                        OfflineModeDialog(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            onDismissCLick = {
                                isInternetConnectionShown.value = true
                            },
                            onReloadClick = {
                                isInternetConnectionShown.value = true
                                onReloadClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseDetailNativeContent(
    windowSize: WindowSize,
    apiHostUrl: String,
    course: Course,
    hasInternetConnection: Boolean,
    isInternetConnectionShown: MutableState<Boolean>,
    onButtonClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val buttonWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.width(230.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val contentHorizontalPadding by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = 6.dp,
                compact = 24.dp
            )
        )
    }

    val buttonText = if (course.isEnrolled) {
        stringResource(id = R.string.discovery_view_course)
    } else if (!course.purchaseLink.isNullOrBlank()) {
        stringResource(id = R.string.discovery_buy_course)
    } else {
        stringResource(id = R.string.discovery_enroll_now)
    }

    var showPurchaseDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val durationText = if (course.duration.isBlank()) {
        stringResource(id = R.string.course_duration_unspecified)
    } else {
        stringResource(id = R.string.course_duration_specified, course.duration)
    }

    Column {
        Box(contentAlignment = Alignment.Center) {
            ImageHeader(
                modifier = Modifier
                    .aspectRatio(ratio = 1.86f)
                    .padding(6.dp),
                apiHostUrl = apiHostUrl,
                courseImage = course.media.image?.large,
                courseName = course.name
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    modifier = Modifier.testTag("ib_play_video"),
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.discovery_ic_play),
                        contentDescription = stringResource(id = R.string.discovery_accessibility_play_video),
                        tint = Color.LightGray
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = contentHorizontalPadding)
        ) {
            val enrollmentEnd = course.enrollmentEnd
            if (!hasInternetConnection) {
                isInternetConnectionShown.value = true
                NoInternetLabel()
                Spacer(Modifier.height(24.dp))
            } else if (enrollmentEnd != null && Date() > enrollmentEnd) {
                EnrollOverLabel()
                Spacer(Modifier.height(24.dp))
            }
            Text(
                modifier = Modifier.testTag("txt_course_name"),
                text = course.name,
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier.testTag("txt_course_org"),
                text = course.org,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.primary
            )
            if (!(enrollmentEnd != null && Date() > enrollmentEnd)) {
                Spacer(Modifier.height(32.dp))
                if (course.isEnrolled) {
                    // User is enrolled - show regular View Course button
                    OpenEdXButton(
                        modifier = buttonWidth,
                        text = buttonText,
                        onClick = onButtonClick
                    )
                } else if (!course.purchaseLink.isNullOrBlank()) {
                    // User not enrolled and purchase link exists - show Buy Course button with cart icon
                    OpenEdXButton(
                        modifier = buttonWidth,
                        text = buttonText,
                        onClick = {
                            showPurchaseDialog = true
                        },
                        content = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.appColors.primaryButtonText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buttonText,
                                    color = MaterialTheme.appColors.primaryButtonText,
                                    style = MaterialTheme.appTypography.labelLarge
                                )
                            }
                        }
                    )
                } else {
                    // User not enrolled and no purchase link - show regular Enroll Now button
                    OpenEdXButton(
                        modifier = buttonWidth,
                        text = buttonText,
                        onClick = onButtonClick
                    )
                }
            }

            // Show purchase confirmation dialog
            if (showPurchaseDialog) {
                PurchaseConfirmationDialog(
                    onDismiss = { showPurchaseDialog = false },
                    onContinue = {
                        val intent = Intent(Intent.ACTION_VIEW, course.purchaseLink?.toUri())
                        context.startActivity(intent)
                        showPurchaseDialog = false
                    }
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("txt_course_duration"),
                text = durationText,
                color = MaterialTheme.appColors.textFieldHint,
                style = MaterialTheme.appTypography.titleSmall,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CourseDetailNativeContentLandscape(
    windowSize: WindowSize,
    apiHostUrl: String,
    course: Course,
    hasInternetConnection: Boolean,
    isInternetConnectionShown: MutableState<Boolean>,
    onButtonClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val buttonWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.width(230.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val buttonText = if (course.isEnrolled) {
        stringResource(id = R.string.discovery_view_course)
    } else {
        stringResource(id = R.string.discovery_enroll_now)
    }

    Row(
        Modifier.heightIn(200.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .weight(weight = 3f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    modifier = Modifier.testTag("txt_course_name"),
                    text = course.name,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    modifier = Modifier.testTag("txt_course_org"),
                    text = course.org,
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.primary
                )
                Spacer(Modifier.height(42.dp))
            }
            val enrollmentEnd = course.enrollmentEnd
            if (!hasInternetConnection) {
                isInternetConnectionShown.value = true
                NoInternetLabel()
                Spacer(Modifier.height(24.dp))
            } else if (enrollmentEnd != null && Date() > enrollmentEnd) {
                EnrollOverLabel()
                Spacer(Modifier.height(24.dp))
            } else {
                OpenEdXButton(
                    modifier = buttonWidth,
                    text = buttonText,
                    onClick = onButtonClick
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Box(contentAlignment = Alignment.Center) {
            ImageHeader(
                modifier = Modifier
                    .width(263.dp)
                    .height(200.dp),
                apiHostUrl = apiHostUrl,
                courseImage = course.media.image?.large,
                courseName = course.name
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    modifier = Modifier.testTag("ib_play_video"),
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.discovery_ic_play),
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrollOverLabel() {
    WarningLabel(
        painter = rememberVectorPainter(Icons.Outlined.Report),
        text = stringResource(id = R.string.discovery_you_cant_enroll)
    )
}

@Composable
private fun NoInternetLabel() {
    WarningLabel(
        painter = painterResource(id = CoreR.drawable.core_ic_offline),
        text = stringResource(id = R.string.discovery_no_internet_label)
    )
}

@Composable
private fun CourseInfoSection(
    icon: ImageVector,
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = if (content.isNotBlank()) 8.dp else 0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.appColors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textFieldHint
            )
        }
        if (content.isNotBlank()) {
            Text(
                text = content,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textPrimary
            )
        }
    }
}

@Composable
private fun InstructorCard(
    instructor: org.openedx.discovery.data.model.Instructor,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.appColors.cardViewBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.appColors.background,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!instructor.image.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(instructor.image)
                            .crossfade(true)
                            .build(),
                        contentDescription = instructor.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.appColors.textFieldHint, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.appColors.cardViewBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textFieldHint,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (!instructor.name.isNullOrBlank()) {
                        Text(
                            text = instructor.name,
                            style = MaterialTheme.appTypography.titleMedium.copy(
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.appColors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!instructor.title.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = instructor.title,
                            style = MaterialTheme.appTypography.bodyMedium,
                            color = MaterialTheme.appColors.textFieldHint
                        )
                    }

                    if (!instructor.organization.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Domain,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = instructor.organization,
                                style = MaterialTheme.appTypography.bodyMedium,
                                color = MaterialTheme.appColors.primary
                            )
                        }
                    }
                }
            }

            if (!instructor.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = instructor.bio,
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textFieldHint
                )
            }
        }
    }
}

@Composable
private fun InstructorsSection(
    instructors: List<org.openedx.discovery.data.model.Instructor>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.appColors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.core_instructors),
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textFieldHint
            )
        }

        instructors.forEach { instructor ->
            InstructorCard(instructor = instructor)
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CourseDescription(
    modifier: Modifier,
    apiHostUrl: String,
    body: String,
    onWebPageLoaded: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(modifier = Modifier.then(modifier), factory = {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val clickUrl = request?.url?.toString() ?: ""
                    return if (clickUrl.isNotEmpty() && clickUrl.startsWith("http")) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl))
                        )
                        true
                    } else if (clickUrl.startsWith("mailto:")) {
                        val email = clickUrl.replace("mailto:", "")
                        if (email.isEmailValid()) {
                            EmailUtil.sendEmailIntent(context, email, "", "")
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
            with(settings) {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                setSupportZoom(true)
                loadsImagesAutomatically = true
                domStorageEnabled = true
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            loadDataWithBaseURL(
                apiHostUrl,
                body,
                "text/html",
                StandardCharsets.UTF_8.name(),
                null
            )
            applyDarkModeIfEnabled(isDarkTheme)
        }
    })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PurchaseConfirmationDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        content = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.appColors.background,
                        MaterialTheme.appShapes.cardShape
                    )
                    .clip(MaterialTheme.appShapes.cardShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.cardViewBorder,
                        MaterialTheme.appShapes.cardShape
                    )
                    .padding(horizontal = 40.dp, vertical = 36.dp)
                    .semantics { testTagsAsResourceId = true },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button at top right
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        modifier = Modifier
                            .testTag("ib_close_purchase_dialog")
                            .size(24.dp),
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = CoreR.string.core_cancel),
                            tint = MaterialTheme.appColors.primary
                        )
                    }
                }

                // Shopping cart icon
                Icon(
                    modifier = Modifier
                        .width(88.dp)
                        .height(85.dp),
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.onBackground
                )

                Spacer(Modifier.size(36.dp))

                // Title
                Text(
                    modifier = Modifier.testTag("txt_purchase_dialog_title"),
                    text = stringResource(id = R.string.discovery_purchase_confirmation_title),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.size(16.dp))

                // Message
                Text(
                    modifier = Modifier.testTag("txt_purchase_dialog_message"),
                    text = stringResource(id = R.string.discovery_purchase_confirmation_message),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.size(36.dp))

                // Continue button with primary color
                OpenEdXButton(
                    modifier = Modifier
                        .testTag("btn_continue_purchase")
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.discovery_continue),
                    onClick = onContinue,
                    backgroundColor = MaterialTheme.appColors.primary,
                    content = {
                        Text(
                            modifier = Modifier
                                .testTag("txt_continue_purchase")
                                .fillMaxWidth(),
                            text = stringResource(id = R.string.discovery_continue),
                            color = MaterialTheme.appColors.primaryButtonText,
                            style = MaterialTheme.appTypography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseDetailNativeContentPreview() {
    OpenEdXTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            apiHostUrl = "http://localhost:8000",
            hasInternetConnection = false,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {},
            onRegisterClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDetailNativeContentTabletPreview() {
    OpenEdXTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            apiHostUrl = "http://localhost:8000",
            hasInternetConnection = false,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {},
            onRegisterClick = {},
            onSignInClick = {},
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
    overview = "<p>Overview content</p>",
    isEnrolled = false,
    duration = "30 Days",
    courseRequirement = "",
    description = "",
    learningOutcomes = "",
    instructorsList = listOf(
        org.openedx.discovery.data.model.Instructor(
            name = "Instructor 1",
            title = "Title 1",
            organization = "Organization 1",
            bio = "Bio content for instructor 1",
            image = null
        ),
    )
)
