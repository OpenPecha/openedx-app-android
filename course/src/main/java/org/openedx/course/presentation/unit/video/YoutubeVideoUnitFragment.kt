package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoUnitBinding
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.foundation.extension.computeWindowSizeClasses
import org.openedx.foundation.extension.objectToString
import org.openedx.foundation.extension.stringToObject
import org.openedx.foundation.presentation.WindowSize

class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var _binding: FragmentYoutubeVideoUnitBinding? = null
    private val binding get() = _binding!!

    private var windowSize: WindowSize? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    // Track if we're using WebView fallback (persists across rotations)
    private var isUsingWebViewFallback = false

    // Track last known WebView playback position (persists across rotations)
    private var webViewLastPlaybackPosition: Float = 0f


    private val youtubeTrackerListener = YouTubePlayerTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            blockId = getString(ARG_BLOCK_ID, "")
        }
        viewModel.downloadSubtitles()

        savedInstanceState?.let {
            isUsingWebViewFallback = it.getBoolean(KEY_USING_WEBVIEW_FALLBACK, false)
            webViewLastPlaybackPosition = it.getFloat(KEY_WEBVIEW_PLAYBACK_POSITION, 0f)
            android.util.Log.d("YoutubeVideoUnit", "Restored state - isUsingWebViewFallback: $isUsingWebViewFallback, playback position: $webViewLastPlaybackPosition sec")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentYoutubeVideoUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying) {
            _youTubePlayer?.play()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cvVideoTitle?.setContent {
            OpenEdXTheme {
                VideoTitle(text = requireArguments().getString(ARG_TITLE) ?: "")
            }
        }

        binding.connectionError.setContent {
            OpenEdXTheme {
                ConnectionErrorView {
                    binding.connectionError.isVisible = !viewModel.hasInternetConnection
                }
            }
        }

        binding.subtitles.setContent {
            OpenEdXTheme {
                val state = rememberLazyListState()
                val currentIndex by viewModel.currentIndex.collectAsState(0)
                val transcriptObject by viewModel.transcriptObject.observeAsState()
                VideoSubtitles(
                    listState = state,
                    timedTextObject = transcriptObject,
                    subtitleLanguage = LocaleUtils.getDisplayLanguage(viewModel.transcriptLanguage),
                    showSubtitleLanguage = viewModel.transcripts.size > 1,
                    currentIndex = currentIndex,
                    onTranscriptClick = {
                        _youTubePlayer?.apply {
                            seekTo(it.start.mseconds / 1000f)
                            play()
                        }
                    },
                    onSettingsClick = {
                        _youTubePlayer?.pause()
                        val dialog =
                            SelectBottomDialogFragment.newInstance(
                                LocaleUtils.getLanguages(viewModel.transcripts.keys.toList())
                            )
                        dialog.show(
                            requireActivity().supportFragmentManager,
                            SelectBottomDialogFragment::class.simpleName
                        )
                    }
                )
            }
        }

        binding.connectionError.isVisible = !viewModel.hasInternetConnection

        if (isUsingWebViewFallback) {
            // Hide YouTube player, show WebView
            binding.youtubePlayerView.visibility = View.GONE
            binding.fallbackWebview.visibility = View.VISIBLE
            // Re-setup WebView
            setupWebViewPlayer()
        } else {
            // Hide WebView initially - it's only a fallback
            binding.fallbackWebview.visibility = View.GONE
            binding.youtubePlayerView.visibility = View.VISIBLE
            // Initialize YouTube player library (original approach)
            initializeYoutubePlayer()
        }
    }

    private fun initializeYoutubePlayer() {
        lifecycle.addObserver(binding.youtubePlayerView)

        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .build()

        val listener = object : AbstractYouTubePlayerListener() {
            var isMarkBlockCompletedCalled = false
            var hasError = false

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.setCurrentVideoTime((second * 1000f).toLong())
                val completePercentage = second / youtubeTrackerListener.videoDuration
                if (completePercentage >= VIDEO_COMPLETION_THRESHOLD && !isMarkBlockCompletedCalled) {
                    viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                    isMarkBlockCompletedCalled = true
                }
                if (completePercentage >= RATE_DIALOG_THRESHOLD && !appReviewManager.isDialogShowed) {
                    appReviewManager.tryToOpenRateDialog()
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState,
            ) {
                super.onStateChange(youTubePlayer, state)
                viewModel.isPlaying = when (state) {
                    PlayerConstants.PlayerState.PLAYING -> true
                    PlayerConstants.PlayerState.PAUSED -> false
                    else -> return
                }
                viewModel.logPlayPauseEvent(
                    viewModel.videoUrl,
                    viewModel.isPlaying,
                    viewModel.getCurrentVideoTime(),
                    CourseAnalyticsKey.YOUTUBE.key
                )
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(youTubePlayer, error)
                // If YouTube player fails, switch to WebView fallback
                if (!hasError) {
                    hasError = true
                    android.util.Log.w("YoutubePlayer", "YouTube player error: $error - switching to WebView fallback")
                    switchToWebViewFallback()
                }
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                android.util.Log.d("YoutubePlayer", "=== onReady called ===")
                android.util.Log.d("YoutubePlayer", "Video URL: ${viewModel.videoUrl}")

                _youTubePlayer = youTubePlayer
                if (_binding != null) {
                    val defPlayerUiController = DefaultPlayerUiController(
                        binding.youtubePlayerView,
                        youTubePlayer
                    )
                    defPlayerUiController.setFullScreenButtonClickListener {
                        router.navigateToFullScreenYoutubeVideo(
                            requireActivity().supportFragmentManager,
                            viewModel.videoUrl,
                            viewModel.getCurrentVideoTime(),
                            blockId,
                            viewModel.courseId,
                            viewModel.isPlaying
                        )
                    }
                    binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)
                }

                viewModel.videoUrl.split("watch?v=").getOrNull(1)?.let { videoId ->
                    android.util.Log.d("YoutubePlayer", "Extracted video ID: '$videoId'")
                    if (viewModel.isPlaying && isResumed) {
                        android.util.Log.d("YoutubePlayer", "Action: loadVideo (autoplay)")
                        android.util.Log.d("YoutubePlayer", "Video ID: '$videoId', Start time: ${viewModel.getCurrentVideoTime() / 1000f} sec, isResumed: $isResumed")
                        youTubePlayer.loadVideo(
                            videoId,
                            viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    } else {
                        android.util.Log.d("YoutubePlayer", "Action: cueVideo (user must click play)")
                        android.util.Log.d("YoutubePlayer", "Video ID: '$videoId', Start time: ${viewModel.getCurrentVideoTime() / 1000f} sec")
                        youTubePlayer.cueVideo(
                            videoId,
                            viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    }
                }
                youTubePlayer.addListener(youtubeTrackerListener)
                viewModel.logLoadedCompletedEvent(
                    viewModel.videoUrl,
                    true,
                    viewModel.getCurrentVideoTime(),
                    CourseAnalyticsKey.YOUTUBE.key
                )
            }
        }

        binding.youtubePlayerView.initialize(listener, options)
    }

    private fun switchToWebViewFallback() {
        isUsingWebViewFallback = true

        // Hide YouTube player, show WebView
        binding.youtubePlayerView.visibility = View.GONE
        binding.fallbackWebview.visibility = View.VISIBLE

        // Setup and load WebView
        setupWebViewPlayer()
    }

    override fun onPause() {
        super.onPause()
        _youTubePlayer?.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save state before rotation
        outState.putBoolean(KEY_USING_WEBVIEW_FALLBACK, isUsingWebViewFallback)

        // If using WebView, get current playback position via JavaScript
        if (isUsingWebViewFallback && _binding != null) {
            binding.fallbackWebview?.evaluateJavascript(
                "(function() { try { return player ? player.getCurrentTime() : 0; } catch(e) { return 0; } })()"
            ) { result ->
                try {
                    val position = result?.toFloatOrNull() ?: 0f
                    webViewLastPlaybackPosition = position
                    android.util.Log.d("YoutubeVideoUnit", "Saved WebView playback position: $position sec")
                } catch (e: Exception) {
                    android.util.Log.e("YoutubeVideoUnit", "Error saving playback position", e)
                }
            }
        }

        outState.putFloat(KEY_WEBVIEW_PLAYBACK_POSITION, webViewLastPlaybackPosition)
        android.util.Log.d("YoutubeVideoUnit", "Saving state - isUsingWebViewFallback: $isUsingWebViewFallback, position: $webViewLastPlaybackPosition sec")
    }

    override fun onDestroyView() {
        _youTubePlayer = null
        super.onDestroyView()
        _binding = null
    }

    private fun setupWebViewPlayer() {
        val videoId = extractYouTubeVideoId(viewModel.videoUrl) ?: run {
            android.util.Log.e("YoutubeVideoUnit", "Failed to extract video ID from: ${viewModel.videoUrl}")
            return
        }

        // Hide YouTube player library view, show WebView
        binding.youtubePlayerView.visibility = View.GONE
        binding.fallbackWebview.visibility = View.VISIBLE

        // Configure WebView with better settings for video playback
        binding.fallbackWebview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.databaseEnabled = true

            // Enable fullscreen support
            settings.javaScriptCanOpenWindowsAutomatically = true

            // Set black background like native player
            setBackgroundColor(android.graphics.Color.BLACK)

            // Add JavaScript interface to track playback position
            addJavascriptInterface(WebAppInterface(), "Android")

            // Add WebChromeClient for fullscreen support
            webChromeClient = object : WebChromeClient() {
                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }

                    customView = view
                    customViewCallback = callback

                    // Enter fullscreen
                    (activity as? AppCompatActivity)?.let { activity ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            activity.window.insetsController?.let { controller ->
                                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            activity.window.decorView.systemUiVisibility = (
                                    View.SYSTEM_UI_FLAG_FULLSCREEN
                                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    )
                        }

                        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
                        contentView.addView(view, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                    }
                }

                override fun onHideCustomView() {
                    (activity as? AppCompatActivity)?.let { activity ->
                        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
                        contentView.removeView(customView)

                        // Exit fullscreen
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            activity.window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                        } else {
                            @Suppress("DEPRECATION")
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        }
                    }

                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }
            }

            // Add WebViewClient to handle errors and block external navigation
            webViewClient = object : android.webkit.WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    android.util.Log.e("YoutubePlayer", "WebView error: $errorCode - $description")
                    // Let YouTube's own error handling show in the iframe
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: android.webkit.WebView?,
                    url: String?
                ): Boolean {
                    // Block all navigation attempts - keep user in the video player
                    if (url != null) {
                        android.util.Log.d("YoutubePlayer", "Blocked navigation to: $url")

                        // Check if it's trying to navigate to YouTube website, channel, or external links
                        when {
                            url.contains("youtube.com/channel") ||
                                    url.contains("youtube.com/user") ||
                                    url.contains("youtube.com/c/") ||
                                    url.contains("youtube.com/@") ||
                                    url.contains("youtube.com/watch") ||
                                    url.contains("youtube.com/playlist") ||
                                    url.contains("youtu.be") -> {
                                android.util.Log.w("YoutubePlayer", "Blocked YouTube navigation attempt")
                                return true // Block navigation
                            }
                            url.startsWith("http") && !url.contains("staging.sherab.org") -> {
                                android.util.Log.w("YoutubePlayer", "Blocked external navigation attempt")
                                return true // Block any external navigation
                            }
                        }
                    }
                    return false // Allow internal navigation (shouldn't happen in our case)
                }

                override fun shouldOverrideUrlLoading(
                    view: android.webkit.WebView?,
                    request: android.webkit.WebResourceRequest?
                ): Boolean {
                    // Modern API version - also block navigation
                    val url = request?.url?.toString()
                    if (url != null) {
                        android.util.Log.d("YoutubePlayer", "Blocked navigation to: $url")

                        when {
                            url.contains("youtube.com/channel") ||
                                    url.contains("youtube.com/user") ||
                                    url.contains("youtube.com/c/") ||
                                    url.contains("youtube.com/@") ||
                                    url.contains("youtube.com/watch") ||
                                    url.contains("youtube.com/playlist") ||
                                    url.contains("youtu.be") -> {
                                android.util.Log.w("YoutubePlayer", "Blocked YouTube navigation attempt")
                                return true
                            }
                            url.startsWith("http") && !url.contains("staging.sherab.org") -> {
                                android.util.Log.w("YoutubePlayer", "Blocked external navigation attempt")
                                return true
                            }
                        }
                    }
                    return false
                }
            }
        }

        // Try primary embed approach first
        loadYouTubeEmbed(videoId)
    }

    private fun loadYouTubeEmbed(videoId: String) {
        // Use restored position if available (after rotation), otherwise use ViewModel's position
        val startTime = if (webViewLastPlaybackPosition > 0) {
            android.util.Log.d("YoutubeVideoUnit", "Using restored position: $webViewLastPlaybackPosition sec")
            webViewLastPlaybackPosition.toInt()
        } else {
            val vmTime = (viewModel.getCurrentVideoTime() / 1000).toInt()
            android.util.Log.d("YoutubeVideoUnit", "Using ViewModel position: $vmTime sec")
            vmTime
        }

        // Create HTML with YouTube IFrame Player API for native-like experience
        val embedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { 
                        margin: 0; 
                        padding: 0; 
                        box-sizing: border-box;
                    }
                    html, body { 
                        width: 100%;
                        height: 100%;
                        background: #000;
                        overflow: hidden;
                    }
                    .player-container {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #000;
                    }
                    #player {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                    }
                    /* Hide YouTube branding and clickable elements */
                    .ytp-chrome-top,
                    .ytp-show-cards-title,
                    .ytp-watermark,
                    .ytp-title,
                    .ytp-title-text,
                    .ytp-title-link,
                    .ytp-title-channel,
                    .ytp-cards-button,
                    .ytp-share-button,
                    .ytp-watch-later-button,
                    .iv-branding,
                    .branding-img,
                    .ytp-pause-overlay,
                    .ytp-ce-element,
                    .annotation {
                        display: none !important;
                        visibility: hidden !important;
                        opacity: 0 !important;
                        pointer-events: none !important;
                    }
                </style>
            </head>
            <body>
                <div class="player-container">
                    <div id="player"></div>
                </div>
                
                <script src="https://www.youtube.com/iframe_api"></script>
                <script>
                    var player;
                    var positionTracker;
                    
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'start': $startTime,
                                'controls': 1,
                                'fs': 1,
                                'cc_load_policy': 0,
                                'iv_load_policy': 3,
                                'showinfo': 0,
                                'color': 'white',
                                'disablekb': 0,
                                'enablejsapi': 1,
                                'origin': 'https://staging.sherab.org',
                                'widget_referrer': 'https://staging.sherab.org'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onError': onPlayerError,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }
                    
                    function onPlayerReady(event) {                        
                        // Hide branding elements after player loads
                        hideYouTubeBranding();
                        
                        try {
                            event.target.playVideo();
                            console.log('Autoplay initiated');
                            
                            // Start tracking playback position every 500ms
                            startPositionTracking();
                        } catch (e) {
                            console.log('Autoplay failed, user interaction required: ' + e);
                        }
                    }
                    
                    function startPositionTracking() {
                        // Clear any existing tracker
                        if (positionTracker) {
                            clearInterval(positionTracker);
                        }
                        
                        // Track position every 500ms and send to Android
                        positionTracker = setInterval(function() {
                            if (player && player.getCurrentTime) {
                                try {
                                    var currentTime = player.getCurrentTime();
                                    // Send position to Android via JavaScript interface
                                    if (window.Android && window.Android.updatePlaybackPosition) {
                                        window.Android.updatePlaybackPosition(currentTime);
                                    }
                                } catch (e) {
                                    console.log('Error tracking position: ' + e);
                                }
                            }
                        }, 500);
                    }
                    
                    function hideYouTubeBranding() {
                        // Additional JavaScript to hide elements that CSS might miss
                        setTimeout(function() {
                            var iframe = document.querySelector('iframe');
                            if (iframe && iframe.contentDocument) {
                                try {
                                    var style = iframe.contentDocument.createElement('style');
                                    style.textContent = '.ytp-chrome-top, .ytp-watermark, .ytp-title, .ytp-share-button { display: none !important; }';
                                    iframe.contentDocument.head.appendChild(style);
                                } catch (e) {
                                    console.log('Cannot access iframe content due to CORS');
                                }
                            }
                        }, 1000);
                    }
                    
                    function onPlayerStateChange(event) {
                        var states = {
                            '-1': 'UNSTARTED',
                            '0': 'ENDED',
                            '1': 'PLAYING',
                            '2': 'PAUSED',
                            '3': 'BUFFERING',
                            '5': 'CUED'
                        };
                        console.log('Player state changed: ' + states[event.data] + ' (' + event.data + ')');
                        
                        // Re-hide branding on state changes
                        hideYouTubeBranding();
                        
                        // Start or stop position tracking based on state
                        if (event.data === 1) { // PLAYING
                            startPositionTracking();
                        } else if (event.data === 2 || event.data === 0) { // PAUSED or ENDED
                            if (positionTracker) {
                                clearInterval(positionTracker);
                            }
                        }
                    }
                    
                    function onPlayerError(event) {
                        var errorCodes = {
                            '2': 'Invalid parameter value',
                            '5': 'HTML5 player error',
                            '100': 'Video not found or removed',
                            '101': 'Video cannot be played in embedded player - Embedding disabled by uploader',
                            '150': 'Video cannot be played in embedded player - Same as 101'
                        };
                        console.error('=== YouTube Player Error ===');
                        console.error('Video ID: $videoId');
                        console.error('Error Code: ' + event.data);
                        console.error('Error Description: ' + (errorCodes[event.data] || 'Unknown error'));
                        console.error('Video URL: ${viewModel.videoUrl}');
                        
                        // Stop position tracking on error
                        if (positionTracker) {
                            clearInterval(positionTracker);
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        // Load the HTML
        binding.fallbackWebview?.loadDataWithBaseURL(
            "https://staging.sherab.org",
            embedHtml,
            "text/html",
            "UTF-8",
            null
        )
    }


    private fun extractYouTubeVideoId(url: String): String? {
        return try {
            when {
                // Standard watch URL: https://www.youtube.com/watch?v=VIDEO_ID
                url.contains("watch?v=") -> {
                    val parts = url.split("watch?v=")
                    if (parts.size > 1) {
                        parts[1].split("&").firstOrNull()
                    } else null
                }
                // Short URL: https://youtu.be/VIDEO_ID
                url.contains("youtu.be/") -> {
                    val parts = url.split("youtu.be/")
                    if (parts.size > 1) {
                        parts[1].split("?").firstOrNull()?.split("&")?.firstOrNull()
                    } else null
                }
                // Embed URL: https://www.youtube.com/embed/VIDEO_ID
                url.contains("/embed/") -> {
                    val parts = url.split("/embed/")
                    if (parts.size > 1) {
                        parts[1].split("?").firstOrNull()?.split("&")?.firstOrNull()
                    } else null
                }
                // v parameter: https://youtube.com/v/VIDEO_ID
                url.contains("/v/") -> {
                    val parts = url.split("/v/")
                    if (parts.size > 1) {
                        parts[1].split("?").firstOrNull()?.split("&")?.firstOrNull()
                    } else null
                }
                else -> {
                    android.util.Log.w("YoutubePlayer", "Unknown YouTube URL format: $url")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YoutubePlayer", "Error extracting video ID from: $url", e)
            null
        }
    }

    /**
     * JavaScript interface to receive playback position updates from WebView
     */
    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun updatePlaybackPosition(position: Float) {
            webViewLastPlaybackPosition = position
            // Also update ViewModel's position for consistency
            viewModel.setCurrentVideoTime((position * 1000f).toLong())
        }
    }

    companion object {

        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "blockTitle"
        private const val KEY_USING_WEBVIEW_FALLBACK = "usingWebViewFallback"
        private const val KEY_WEBVIEW_PLAYBACK_POSITION = "webViewPlaybackPosition"

        const val VIDEO_COMPLETION_THRESHOLD = 0.8f
        const val RATE_DIALOG_THRESHOLD = 0.99f

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            blockTitle: String,
        ): YoutubeVideoUnitFragment {
            val fragment = YoutubeVideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_TITLE to blockTitle
            )
            return fragment
        }
    }
}
