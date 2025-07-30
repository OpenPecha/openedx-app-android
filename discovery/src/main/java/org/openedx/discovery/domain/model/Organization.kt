package org.openedx.discovery.domain.model

import com.google.gson.annotations.SerializedName

data class Organization(
    @SerializedName("partner_name")
    val name: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("organization")
    val organization: String,
)
