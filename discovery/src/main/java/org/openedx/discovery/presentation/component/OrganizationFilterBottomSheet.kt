package org.openedx.discovery.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Organization
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.request.ImageRequest


@Composable
fun OrganizationFilterBottomSheet(
    orgList: List<Organization>,
    isLoading: Boolean,
    selectedOrg: Organization?,
    onClose: () -> Unit,
    onOrgSelected: (Organization) -> Unit
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
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(orgList.size) { index ->
                    OrganizationCard(
                        organization = orgList[index],
                        isSelected = selectedOrg?.organization == orgList[index].organization,
                        onClick = { onOrgSelected(orgList[index]) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Apply Button
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colors.primary)
                .clickable { onClose() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.apply),
                color = Color.White,
                style = MaterialTheme.appTypography.titleMedium
            )
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun OrganizationFilterBottomSheetPreview() {
    val sampleOrgs = listOf(
        Organization("org1", "https://example.com/mock_logo.png", "Org 1"),
        Organization("org2", "https://example.com/mock_logo2.png", "Org 2"),
    )

    OpenEdXTheme {
        OrganizationFilterBottomSheet(
            orgList = sampleOrgs,
            isLoading = false,
            onClose = {},
            onOrgSelected = {},
            selectedOrg = sampleOrgs[0]
        )
    }
}

@Composable
fun OrganizationCard(
    organization: Organization,
    isSelected: Boolean,
    onClick: (Organization) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick(organization) }
    ) {
        // Circular card background with border if selected
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.appColors.secondary,
                    shape = CircleShape
                )
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(organization.logo)
                    .crossfade(true)
                    .build(),
                contentDescription = organization.name,
                contentScale = ContentScale.Fit, // Show entire logo
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp) // inner padding to give logo space
            )
        }

        // Organization name
        Text(
            text = organization.name,
            style = MaterialTheme.appTypography.bodySmall,
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 6.dp)
                .width(85.dp)
        )
    }
}
