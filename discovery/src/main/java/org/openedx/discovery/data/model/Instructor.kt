package org.openedx.discovery.data.model

import com.google.gson.annotations.SerializedName

data class Instructor(
    @SerializedName("name")
    val name: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("organization")
    val organization: String?,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("image")
    val image: String?
)

