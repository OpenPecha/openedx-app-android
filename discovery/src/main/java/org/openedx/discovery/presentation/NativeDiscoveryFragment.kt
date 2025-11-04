package org.openedx.discovery.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.discovery.presentation.ui.DiscoveryScreen
import org.openedx.foundation.presentation.rememberWindowSize

class NativeDiscoveryFragment : Fragment() {

    private val viewModel by viewModel<NativeDiscoveryViewModel>()
    private val router: DiscoveryRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(DiscoveryUIState.Loading)
                val uiMessage by viewModel.uiMessage.collectAsState(initial = null)
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)
                val organizations by viewModel.organizations.observeAsState(emptyList())
                val selectedOrganization by viewModel.selectedOrganization.observeAsState()
                val querySearch = arguments?.getString(ARG_SEARCH_QUERY, "") ?: ""

                DiscoveryScreen(
                    windowSize = windowSize,
                    state = uiState,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    canShowBackButton = viewModel.canShowBackButton,
                    isUserLoggedIn = viewModel.isUserLoggedIn,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    onSearchClick = {
                        viewModel.discoverySearchBarClickedEvent()
                        router.navigateToCourseSearch(
                            requireActivity().supportFragmentManager,
                            ""
                        )
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    onSwipeRefresh = {
                        viewModel.refreshCourses()
                    },
                    onReloadClick = {
                        viewModel.getCoursesList()
                    },
                    onItemClick = { course ->
                        viewModel.discoveryCourseClicked(course.id, course.name)
                        viewModel.courseDetailClickedEvent(course.id, course.name)
                        router.navigateToCourseDetail(
                            requireActivity().supportFragmentManager,
                            course.id
                        )
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager, null, null)
                    },
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager, null, null)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onSettingsClick = {
                        router.navigateToSettings(requireActivity().supportFragmentManager)
                    },
                    organizations = organizations,
                    selectedOrganization = selectedOrganization,
                    onOrganizationSelected = { organization ->
                        viewModel.searchCoursesByOrganization(organization)
                    },
                )
                LaunchedEffect(uiState) {
                    if (querySearch.isNotEmpty()) {
                        router.navigateToCourseSearch(
                            requireActivity().supportFragmentManager,
                            querySearch
                        )
                        arguments?.putString(ARG_SEARCH_QUERY, "")
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_SEARCH_QUERY = "query_search"
        const val LOAD_MORE_THRESHOLD = 4
        fun newInstance(querySearch: String = ""): NativeDiscoveryFragment {
            val fragment = NativeDiscoveryFragment()
            fragment.arguments = bundleOf(
                ARG_SEARCH_QUERY to querySearch
            )
            return fragment
        }
    }
}
