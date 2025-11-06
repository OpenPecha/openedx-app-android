package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.openedx.core.extension.safeDivBy

@Parcelize
data class Progress(
    val assignmentsCompleted: Int,
    val totalAssignmentsCount: Int,
) : Parcelable {

    @IgnoredOnParcel
    val value: Float = assignmentsCompleted.toFloat().safeDivBy(totalAssignmentsCount.toFloat())

    companion object {
        val DEFAULT_PROGRESS = Progress(0, 0)
    }
}
