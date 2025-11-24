package org.openedx.profile.presentation.settings

import org.openedx.profile.domain.model.Configuration
import org.openedx.core.data.storage.ThemeMode

sealed class SettingsUIState {
    data class Data(
        val configuration: Configuration,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
    ) : SettingsUIState()

    object Loading : SettingsUIState()
}
