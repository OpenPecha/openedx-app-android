package org.openedx.discovery.presentation

import org.openedx.discovery.domain.model.Course

sealed class DiscoveryUIState {
    data class Courses(val courses: List<Course>, val numCourses: Int) : DiscoveryUIState()
    data object Loading : DiscoveryUIState()
}
