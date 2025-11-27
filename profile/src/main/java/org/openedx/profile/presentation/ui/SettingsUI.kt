package org.openedx.profile.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.tagId

@Composable
fun SettingsItem(
    text: String,
    icon: ImageVector? = null,
    external: Boolean = false,
    onClick: () -> Unit
) {
    val trailingIcon = if (external) {
        Icons.AutoMirrored.Filled.OpenInNew
    } else {
        Icons.AutoMirrored.Filled.ArrowForwardIos
    }
    Row(
        Modifier
            .testTag("btn_${text.tagId()}")
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                vertical = 24.dp,
                horizontal = 20.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                modifier = Modifier.testTag("txt_${text.tagId()}"),
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary
            )
        }
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.appColors.textPrimary
        )
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        modifier = Modifier
            .padding(
                horizontal = 20.dp
            ),
        color = MaterialTheme.appColors.divider
    )
}
