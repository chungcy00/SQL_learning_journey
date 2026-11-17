package com.example.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ui.screens.CustomFlashcardsScreen
import com.example.ui.screens.DatasetsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FlashcardSessionScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KnowledgeDetailScreen
import com.example.ui.screens.LabScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.MineScreen
import com.example.ui.screens.OfflineStatusScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.QuestionDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WrongBookScreen
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.PrimaryAction
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable data class LearnRoute(val stage: String? = null, val query: String = "")
@Serializable data class KnowledgeDetailRoute(val kpId: String)
@Serializable data class PracticeRoute(val chapterId: String? = null)
@Serializable data class QuestionDetailRoute(val questionId: String)
@Serializable object ProjectsRoute
@Serializable data class ProjectDetailRoute(val projectId: String)
@Serializable object FlashcardsRoute
@Serializable data class FlashcardSessionRoute(
    val modeId: String,
    val chapterId: String? = null,
    val countTowardsSchedule: Boolean = true
)
@Serializable object CustomFlashcardsRoute
@Serializable data class LabRoute(val datasetId: String? = null, val sql: String? = null)
@Serializable object MineRoute
@Serializable object WrongBookRoute
@Serializable object FavoritesRoute
@Serializable object DatasetsRoute
@Serializable object StatsRoute
@Serializable object SettingsRoute
@Serializable object OfflineStatusRoute

private data class TopNavItem(
    val id: String,
    val label: String,
    @DrawableRes val activeIcon: Int,
    @DrawableRes val inactiveIcon: Int,
    val routeObj: Any,
    val matchCheck: (androidx.navigation.NavDestination?) -> Boolean
)

@Composable
fun AppNavGraph(vm: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val topItems = listOf(
        TopNavItem(
            id = "home",
            label = "首页",
            activeIcon = R.drawable.nav_home_active,
            inactiveIcon = R.drawable.nav_home_inactive,
            routeObj = HomeRoute,
            matchCheck = { it?.hasRoute<HomeRoute>() == true }
        ),
        TopNavItem(
            id = "learn",
            label = "学习",
            activeIcon = R.drawable.nav_learn_active,
            inactiveIcon = R.drawable.nav_learn_inactive,
            routeObj = LearnRoute(),
            matchCheck = { it?.hasRoute<LearnRoute>() == true }
        ),
        TopNavItem(
            id = "practice",
            label = "练习",
            activeIcon = R.drawable.nav_practice_active,
            inactiveIcon = R.drawable.nav_practice_inactive,
            routeObj = PracticeRoute(),
            matchCheck = { it?.hasRoute<PracticeRoute>() == true }
        ),
        TopNavItem(
            id = "flashcards",
            label = "闪卡",
            activeIcon = R.drawable.nav_flashcards_active,
            inactiveIcon = R.drawable.nav_flashcards_inactive,
            routeObj = FlashcardsRoute,
            matchCheck = { it?.hasRoute<FlashcardsRoute>() == true }
        ),
        TopNavItem(
            id = "mine",
            label = "我的",
            activeIcon = R.drawable.nav_mine_active,
            inactiveIcon = R.drawable.nav_mine_inactive,
            routeObj = MineRoute,
            matchCheck = { it?.hasRoute<MineRoute>() == true }
        )
    )

    val showBottomBar = topItems.any { it.matchCheck(currentDest) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    windowInsets = WindowInsets.navigationBars,
                    tonalElevation = 0.dp
                ) {
                    topItems.forEach { item ->
                        val selected = item.matchCheck(currentDest)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.routeObj) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Image(
                                    painter = painterResource(id = if (selected) item.activeIcon else item.inactiveIcon),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = PrimaryAction,
                                indicatorColor = CloudSeaBlue,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("bottom_nav_${item.id}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        vm = vm,
                        onOpenKnowledgePoint = { kpId -> navController.navigate(KnowledgeDetailRoute(kpId)) },
                        onOpenLearnWithQuery = { st, q -> navController.navigate(LearnRoute(st, q)) },
                        onOpenLab = { dsId -> navController.navigate(LabRoute(dsId, null)) },
                        onOpenProjects = { navController.navigate(ProjectsRoute) },
                        onOpenProjectDetail = { pId -> navController.navigate(ProjectDetailRoute(pId)) },
                        onOpenWrongBook = { navController.navigate(WrongBookRoute) },
                        onOpenFavorites = { navController.navigate(FavoritesRoute) },
                        onOpenOfflineStatus = { navController.navigate(OfflineStatusRoute) },
                        onOpenFlashcardDue = {
                            navController.navigate(FlashcardSessionRoute("due", null, true))
                        }
                    )
                }

                composable<LearnRoute> { entry ->
                    val route = entry.toRoute<LearnRoute>()
                    LearnScreen(
                        vm = vm,
                        initialStage = route.stage,
                        initialQuery = route.query,
                        onOpenKnowledgePoint = { kpId -> navController.navigate(KnowledgeDetailRoute(kpId)) }
                    )
                }

                composable<KnowledgeDetailRoute> { entry ->
                    val route = entry.toRoute<KnowledgeDetailRoute>()
                    KnowledgeDetailScreen(
                        vm = vm,
                        kpId = route.kpId,
                        onBack = { navController.popBackStack() },
                        onOpenKnowledgePoint = { nextId ->
                            navController.navigate(KnowledgeDetailRoute(nextId)) {
                                popUpTo(KnowledgeDetailRoute(route.kpId)) { inclusive = true }
                            }
                        },
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) },
                        onOpenLabWithSql = { dsId, sql -> navController.navigate(LabRoute(dsId, sql)) }
                    )
                }

                composable<PracticeRoute> { entry ->
                    val route = entry.toRoute<PracticeRoute>()
                    PracticeScreen(
                        vm = vm,
                        initialChapterId = route.chapterId,
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) },
                        onOpenProjects = { navController.navigate(ProjectsRoute) },
                        onOpenWrongBook = { navController.navigate(WrongBookRoute) }
                    )
                }

                composable<QuestionDetailRoute> { entry ->
                    val route = entry.toRoute<QuestionDetailRoute>()
                    QuestionDetailScreen(
                        vm = vm,
                        questionId = route.questionId,
                        onBack = { navController.popBackStack() },
                        onOpenQuestion = { nextQId ->
                            navController.navigate(QuestionDetailRoute(nextQId)) {
                                popUpTo(QuestionDetailRoute(route.questionId)) { inclusive = true }
                            }
                        }
                    )
                }

                composable<ProjectsRoute> {
                    ProjectsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenProjectDetail = { pId -> navController.navigate(ProjectDetailRoute(pId)) },
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) }
                    )
                }

                composable<ProjectDetailRoute> { entry ->
                    val route = entry.toRoute<ProjectDetailRoute>()
                    ProjectDetailScreen(
                        vm = vm,
                        projectId = route.projectId,
                        onBack = { navController.popBackStack() },
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) },
                        onOpenLab = { dsId -> navController.navigate(LabRoute(dsId, null)) }
                    )
                }

                composable<FlashcardsRoute> {
                    FlashcardsScreen(
                        vm = vm,
                        onStartSession = { modeId, chId, countSched ->
                            navController.navigate(FlashcardSessionRoute(modeId, chId, countSched))
                        },
                        onOpenCustomManager = { navController.navigate(CustomFlashcardsRoute) }
                    )
                }

                composable<FlashcardSessionRoute> { entry ->
                    val route = entry.toRoute<FlashcardSessionRoute>()
                    FlashcardSessionScreen(
                        vm = vm,
                        modeId = route.modeId,
                        chapterId = route.chapterId,
                        countTowardsSchedule = route.countTowardsSchedule,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<CustomFlashcardsRoute> {
                    CustomFlashcardsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<LabRoute> { entry ->
                    val route = entry.toRoute<LabRoute>()
                    LabScreen(
                        vm = vm,
                        initialDatasetId = route.datasetId,
                        initialSql = route.sql,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<MineRoute> {
                    MineScreen(
                        vm = vm,
                        onOpenWrongBook = { navController.navigate(WrongBookRoute) },
                        onOpenFavorites = { navController.navigate(FavoritesRoute) },
                        onOpenDatasets = { navController.navigate(DatasetsRoute) },
                        onOpenStats = { navController.navigate(StatsRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenOfflineStatus = { navController.navigate(OfflineStatusRoute) }
                    )
                }

                composable<WrongBookRoute> {
                    WrongBookScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) }
                    )
                }

                composable<FavoritesRoute> {
                    FavoritesScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenKnowledgePoint = { kpId -> navController.navigate(KnowledgeDetailRoute(kpId)) },
                        onOpenQuestion = { qId -> navController.navigate(QuestionDetailRoute(qId)) },
                        onOpenLabWithSql = { sql -> navController.navigate(LabRoute(null, sql)) }
                    )
                }

                composable<DatasetsRoute> {
                    DatasetsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenInLab = { dsId -> navController.navigate(LabRoute(dsId, null)) }
                    )
                }

                composable<StatsRoute> {
                    StatsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<OfflineStatusRoute> {
                    OfflineStatusScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
