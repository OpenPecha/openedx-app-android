package org.openedx.discovery.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Organization

@Composable
fun OrganizationFilterBottomSheet(
    orgList: List<Organization>,
    isLoading: Boolean,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.appColors.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 6.dp)
                .width(60.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.LightGray.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = stringResource(id = R.string.schools_and_partners),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )

        // Loading or Grid
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyVerticalGrid(
                modifier = Modifier.heightIn(max = 360.dp),
                columns = GridCells.Fixed(3)
            ) {
                items(orgList.size) { index ->
                    OrganizationCard(
                        organization = orgList[index],
                        onClick = { onClose() }
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun OrganizationFilterBottomSheetPreview() {
    val sampleOrgs = listOf(
        Organization("org1", "Org 1", "https://sherab.share.zrok.io/media/partner/BDRC_Logo.png"),
        Organization("org2", "Org 2", "https://sherab.share.zrok.io/media/partner/Palpung_logo_g8hgck6.png"),
    )

    OpenEdXTheme {
        OrganizationFilterBottomSheet(
            orgList = sampleOrgs,
            isLoading = false,
            onClose = {}
        )
    }
}

@Composable
fun OrganizationCard(
    organization: Organization,
    onClick: (Organization) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.appColors.cardViewBackground)
            .clickable { onClick(organization) }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = organization.logo,
            contentDescription = organization.name,
            modifier = Modifier
                .height(50.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = organization.name,
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
