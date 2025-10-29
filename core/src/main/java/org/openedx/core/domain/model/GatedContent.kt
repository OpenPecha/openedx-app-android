package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GatedContent(
    val prereqId: String,
    val prereqSectionName: String,
    val gated: Boolean,
    val gatedSectionName: String,
    val prereqUrl: String
) : Parcelable

