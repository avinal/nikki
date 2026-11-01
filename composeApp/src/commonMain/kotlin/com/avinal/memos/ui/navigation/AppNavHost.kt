package com.avinal.memos.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.auth.LoginScreen
import com.avinal.memos.ui.memos.MainScreen
import com.avinal.memos.ui.memos.MemoDetailScreen
import com.avinal.memos.ui.memos.MemoEditorScreen
import com.avinal.memos.ui.onboarding.PermissionsScreen
import com.avinal.memos.ui.onboarding.WelcomeScreen
import kotlinx.coroutines.launch

private const val ANIM_DURATION = 300

@Composable
fun AppNavHost(deps: AppDependencies, sharedText: String? = null) {
    val navController = rememberNavController()
    val isLoggedIn by deps.authRepository.isLoggedIn.collectAsState(initial = false)
    val onboardingComplete by deps.tokenStore.onboardingComplete.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    val startDestination = when {
        isLoggedIn -> Route.Main
        !onboardingComplete -> Route.Welcome
        else -> Route.Login
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION))
        },
    ) {
        composable<Route.Welcome>(
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
        ) {
            WelcomeScreen(
                onNext = { navController.navigate(Route.Permissions) },
            )
        }

        composable<Route.Permissions>(
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
        ) {
            PermissionsScreen(
                onBack = { navController.popBackStack() },
                onNext = {
                    navController.navigate(Route.Login)
                },
            )
        }

        composable<Route.Login>(
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) },
        ) {
            val canGoBack by navController.currentBackStack.collectAsState()
            LoginScreen(
                deps = deps,
                onLoginSuccess = {
                    scope.launch { deps.tokenStore.setOnboardingComplete() }
                    navController.navigate(Route.Main) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = if (canGoBack.size > 1) {
                    { navController.popBackStack() }
                } else null,
            )
        }

        composable<Route.Main>(
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION))
            },
        ) {
            MainScreen(
                deps = deps,
                sharedText = sharedText,
                onMemoClick = { memoId -> navController.navigate(Route.MemoDetail(memoId)) },
                onCreateMemo = { navController.navigate(Route.MemoEditor()) },
                onLogout = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.MemoDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MemoDetail>()
            MemoDetailScreen(
                memoId = route.memoId,
                deps = deps,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.MemoEditor(route.memoId)) },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable<Route.MemoEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.MemoEditor>()
            MemoEditorScreen(
                memoId = route.memoId.ifEmpty { null },
                deps = deps,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
