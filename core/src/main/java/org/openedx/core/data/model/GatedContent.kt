package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.GatedContentDb
import org.openedx.core.domain.model.GatedContent as DomainGatedContent

data class GatedContent(
    @SerializedName("prereq_id")
    val prereqId: String?,
    @SerializedName("prereq_section_name")
    val prereqSectionName: String?,
    @SerializedName("gated")
    val gated: Boolean?,
    @SerializedName("gated_section_name")
    val gatedSectionName: String?,
    @SerializedName("prereq_url")
    val prereqUrl: String?
) {
    fun mapToDomain() = DomainGatedContent(
        prereqId = prereqId.orEmpty(),
        prereqSectionName = prereqSectionName.orEmpty(),
        gated = gated ?: false,
        gatedSectionName = gatedSectionName.orEmpty(),
        prereqUrl = prereqUrl.orEmpty()
    )

    fun mapToRoomEntity() = GatedContentDb(
        prereqId = prereqId,
        prereqSectionName = prereqSectionName,
        gated = gated,
        gatedSectionName = gatedSectionName,
        prereqUrl = prereqUrl
    )
}

