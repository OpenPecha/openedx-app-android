package org.openedx.profile.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.R
import org.openedx.core.data.storage.ThemeMode
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.tagId
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.presentation.ui.SettingsDivider

class ThemeFragment : Fragment() {

    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState()

                val currentTheme = (uiState as? SettingsUIState.Data)?.themeMode ?: ThemeMode.SYSTEM

                ThemeScreen(
                    windowSize = windowSize,
                    currentTheme = currentTheme,
                    onThemeSelected = { mode ->
                        viewModel.setThemeMode(mode)
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )

                LaunchedEffect(Unit) {
                    viewModel.themeChanged.collect {
                        requireActivity().recreate()
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): ThemeFragment {
            return ThemeFragment()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ThemeScreen(
    windowSize: WindowSize,
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onBackClick: () -> Unit
) {

    val topBarWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                compact = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .settingsHeaderBackground()
            .statusBarsInset()
    ) {
        Toolbar(
            modifier = topBarWidth
                .align(Alignment.CenterHorizontally)
                .displayCutoutForLandscape(),
            label = stringResource(id = R.string.core_theme),
            canShowBackBtn = true,
            labelTint = MaterialTheme.appColors.settingsTitleContent,
            iconTint = MaterialTheme.appColors.settingsTitleContent,
            onBackClick = onBackClick
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.appShapes.screenBackgroundShape,
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .displayCutoutForLandscape(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .then(contentWidth)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(30.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.appShapes.cardShape,
                                elevation = 0.dp,
                                backgroundColor = MaterialTheme.appColors.cardViewBackground
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    val themeOptions = listOf(
                                        ThemeMode.SYSTEM to R.string.core_theme_system,
                                        ThemeMode.LIGHT to R.string.core_theme_light,
                                        ThemeMode.DARK to R.string.core_theme_dark
                                    )

                                    themeOptions.forEachIndexed { index, (mode, stringRes) ->
                                        ThemeOption(
                                            title = stringResource(id = stringRes),
                                            selected = currentTheme == mode,
                                            onClick = {
                                                onThemeSelected(mode)
                                            }
                                        )

                                        if (index < themeOptions.size - 1) {
                                            SettingsDivider()
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .testTag("btn_theme_${title.tagId()}")
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.testTag("txt_theme_${title.tagId()}"),
            text = title,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium
        )
        if (selected) {
            Icon(
                modifier = Modifier
                    .testTag("ic_theme_selected_${title.tagId()}")
                    .size(20.dp),
                painter = painterResource(id = R.drawable.core_ic_check),
                tint = MaterialTheme.appColors.primary,
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun ThemeScreenPreview() {
    OpenEdXTheme {
        ThemeScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            currentTheme = ThemeMode.SYSTEM,
            onThemeSelected = {},
            onBackClick = {}
        )
    }
}

