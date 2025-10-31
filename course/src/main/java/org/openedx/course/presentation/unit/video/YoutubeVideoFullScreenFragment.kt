package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.global.viewBinding
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoFullScreenBinding
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment.Companion.RATE_DIALOG_THRESHOLD
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment.Companion.VIDEO_COMPLETION_THRESHOLD
import org.openedx.foundation.extension.requestApplyInsetsWhenAttached

class YoutubeVideoFullScreenFragment : Fragment(R.layout.fragment_youtube_video_full_screen) {

    private val binding by viewBinding(FragmentYoutubeVideoFullScreenBinding::bind)
    private val viewModel by viewModel<VideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var blockId = ""

    private val youtubeTrackerListener = YouTubePlayerTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable WebView debugging for YouTube player
        WebView.setWebContentsDebuggingEnabled(true)
        if (viewModel.currentVideoTime == 0L) {
            viewModel.currentVideoTime = requireArguments().getLong(ARG_VIDEO_TIME, 0)
        }
        if (viewModel.isPlaying == null) {
            viewModel.isPlaying = requireArguments().getBoolean(ARG_IS_PLAYING)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())

            val statusBarParams = binding.youtubePlayerView.layoutParams as FrameLayout.LayoutParams
            statusBarParams.topMargin = insetsCompat.top
            statusBarParams.bottomMargin = insetsCompat.bottom
            statusBarParams.marginStart = insetsCompat.left
            statusBarParams.marginEnd = insetsCompat.right
            binding.youtubePlayerView.layoutParams = statusBarParams
            insets
        }
        binding.root.requestApplyInsetsWhenAttached()

        lifecycle.addObserver(binding.youtubePlayerView)
        val options = IFramePlayerOptions.Builder()
            .controls(0)  // Hide default controls (using custom UI)
            .rel(0)       // Don't show related videos
            .ivLoadPolicy(3)  // Critical: Disable video annotations
            .ccLoadPolicy(1)  // Show closed captions
            .build()

        binding.youtubePlayerView.initialize(
            object : AbstractYouTubePlayerListener() {
                var isMarkBlockCompletedCalled = false

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState,
                ) {
                    super.onStateChange(youTubePlayer, state)
                    android.util.Log.d("YoutubePlayerFS", "Player state: $state")

                    if (state == PlayerConstants.PlayerState.ENDED) {
                        viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                    }
                    viewModel.isPlaying = when (state) {
                        PlayerConstants.PlayerState.PLAYING -> {
                            android.util.Log.d("YoutubePlayerFS", "✅ Fullscreen video PLAYING")
                            true
                        }
                        PlayerConstants.PlayerState.PAUSED -> {
                            android.util.Log.d("YoutubePlayerFS", "⏸️ Fullscreen video PAUSED")
                            false
                        }
                        else -> return
                    }
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    viewModel.currentVideoTime = (second * 1000f).toLong()
                    val completePercentage = second / youtubeTrackerListener.videoDuration
                    if (completePercentage >= VIDEO_COMPLETION_THRESHOLD && !isMarkBlockCompletedCalled) {
                        viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                        isMarkBlockCompletedCalled = true
                    }
                    if (completePercentage >= RATE_DIALOG_THRESHOLD && !appReviewManager.isDialogShowed) {
                        if (!appReviewManager.isDialogShowed) {
                            appReviewManager.tryToOpenRateDialog()
                        }
                    }
                }

                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    binding.youtubePlayerView.isVisible = true

                    android.util.Log.d("YoutubePlayerFS", "=== Fullscreen onReady ===")
                    android.util.Log.d("YoutubePlayerFS", "Video URL: ${viewModel.videoUrl}")

                    val videoId = extractYouTubeVideoId(viewModel.videoUrl)
                    android.util.Log.d("YoutubePlayerFS", "Extracted video ID: '$videoId'")

                    if (videoId != null) {
                        val startTime = viewModel.currentVideoTime.toFloat() / 1000
                        // Always use cueVideo - avoids autoplay policy issues
                        android.util.Log.d("YoutubePlayerFS", "Action: cueVideo, Start time: $startTime sec")

                        youTubePlayer.cueVideo(videoId, startTime)
                        youTubePlayer.addListener(youtubeTrackerListener)
                    } else {
                        android.util.Log.e("YoutubePlayerFS", "❌ FAILED to extract video ID from: ${viewModel.videoUrl}")
                    }
                }
            },
            options
        )
    }

    override fun onDestroyView() {
        viewModel.sendTime()
        super.onDestroyView()
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

    companion object {
        private const val ARG_BLOCK_VIDEO_URL = "blockVideoUrl"
        private const val ARG_VIDEO_TIME = "videoTime"
        private const val ARG_BLOCK_ID = "blockID"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_IS_PLAYING = "isPlaying"

        fun newInstance(
            videoUrl: String,
            videoTime: Long,
            blockId: String,
            courseId: String,
            isPlaying: Boolean,
        ): YoutubeVideoFullScreenFragment {
            val fragment = YoutubeVideoFullScreenFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_VIDEO_URL to videoUrl,
                ARG_VIDEO_TIME to videoTime,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_IS_PLAYING to isPlaying
            )
            return fragment
        }
    }
}
