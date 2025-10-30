package org.openedx.course.presentation.unit

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.extension.parcelable
import org.openedx.course.R as courseR

class PrerequisiteLockedFragment : Fragment() {

    private val router by inject<CourseRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val prereqData = arguments?.parcelable<PrerequisiteData>(ARG_PREREQUISITE_DATA)

                PrerequisiteLockedScreen(
                    prereqSectionName = prereqData?.prereqSectionName ?: "",
                    onNavigateToPrerequisite = {
                        prereqData?.let { data ->
                            router.navigateToCourseSubsections(
                                fm = requireActivity().supportFragmentManager,
                                courseId = data.courseId,
                                subSectionId = data.prereqId,
                                unitId = "",
                                componentId = "",
                                mode = data.mode
                            )
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_PREREQUISITE_DATA = "prerequisite_data"

        fun newInstance(
            courseId: String,
            prereqId: String,
            prereqSectionName: String,
            mode: CourseViewMode
        ): PrerequisiteLockedFragment {
            val fragment = PrerequisiteLockedFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(
                    ARG_PREREQUISITE_DATA,
                    PrerequisiteData(courseId, prereqId, prereqSectionName, mode)
                )
            }
            return fragment
        }
    }
}

@kotlinx.parcelize.Parcelize
data class PrerequisiteData(
    val courseId: String,
    val prereqId: String,
    val prereqSectionName: String,
    val mode: CourseViewMode
) : android.os.Parcelable

@Composable
private fun PrerequisiteLockedScreen(
    prereqSectionName: String,
    onNavigateToPrerequisite: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.appColors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = courseR.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = courseR.string.course_content_locked),
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    id = courseR.string.course_must_complete_prerequisite,
                    prereqSectionName
                ),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OpenEdXButton(
                text = stringResource(id = courseR.string.course_go_to_prerequisite_section),
                onClick = onNavigateToPrerequisite,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PrerequisiteLockedScreenPreview() {
    OpenEdXTheme {
        PrerequisiteLockedScreen(
            prereqSectionName = "Introduction to Programming",
            onNavigateToPrerequisite = {}
        )
    }
}
