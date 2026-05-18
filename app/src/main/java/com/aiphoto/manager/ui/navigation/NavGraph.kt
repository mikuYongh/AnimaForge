package com.aiphoto.manager.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiphoto.manager.App
import com.aiphoto.manager.data.model.Template
import com.aiphoto.manager.ui.screen.detail.DetailScreen
import com.aiphoto.manager.ui.screen.detail.DetailViewModel
import com.aiphoto.manager.ui.screen.edit.EditScreen
import com.aiphoto.manager.ui.screen.generate.GenerateScreen
import com.aiphoto.manager.ui.screen.history.GeneratedHistoryScreen
import com.aiphoto.manager.ui.screen.history.ComfyUIHistoryScreen
import com.aiphoto.manager.ui.screen.home.HomeScreen
import com.aiphoto.manager.ui.screen.settings.SettingsScreen
import com.aiphoto.manager.ui.screen.template.TemplateScreen
import com.aiphoto.manager.ui.screen.workflow.WorkflowScreen
import com.aiphoto.manager.ui.screen.workflow.WorkflowEditScreen
import com.aiphoto.manager.ui.screen.video.VideoGenerateScreen
import com.aiphoto.manager.util.JsonUtil
import java.net.URLDecoder
import java.net.URLEncoder

private const val ANIM_DURATION = 300

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = (context.applicationContext as App).database

    var templateToUse by remember { mutableStateOf<Template?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            JsonUtil.importFromJson(context, it, db.promptDao(), db.tagDao())
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            JsonUtil.exportToJson(context, it, db.promptDao(), db.tagDao())
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            fadeIn(animationSpec = tween(ANIM_DURATION)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(ANIM_DURATION)
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(ANIM_DURATION)) +
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(ANIM_DURATION)
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(ANIM_DURATION)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(ANIM_DURATION)
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(ANIM_DURATION)) +
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(ANIM_DURATION)
                )
        }
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { promptId ->
                    navController.navigate("detail/$promptId")
                },
                onNavigateToAdd = {
                    templateToUse = null
                    navController.navigate("edit")
                },
                onNavigateToTemplates = {
                    navController.navigate("templates")
                },
                onNavigateToWorkflows = {
                    navController.navigate("workflows")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToComfyUIHistory = {
                    navController.navigate("comfyui_history")
                },
                onNavigateToVideo = {
                    navController.navigate("video")
                },
                onExport = {
                    exportLauncher.launch("ai_prompts_export.json")
                },
                onImport = {
                    importLauncher.launch("application/json")
                }
            )
        }

        composable(
            route = "detail/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId") ?: return@composable
            DetailScreen(
                promptId = promptId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate("edit/$id")
                },
                onNavigateToGenerate = { id ->
                    navController.navigate("generate/$id")
                },
                onNavigateToCloned = { clonedId ->
                    navController.navigate("detail/$clonedId") {
                        popUpTo("detail/$promptId") { inclusive = true }
                    }
                }
            )
        }

        composable("edit") {
            EditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId") ?: return@composable
            val detailViewModel: DetailViewModel = viewModel()
            val promptData by detailViewModel.promptData.collectAsState()

            LaunchedEffect(promptId) {
                detailViewModel.loadPrompt(promptId)
            }

            promptData?.let { pwt ->
                EditScreen(
                    existingPrompt = pwt,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable("templates") {
            TemplateScreen(
                onNavigateBack = { navController.popBackStack() },
                onUseTemplate = { template ->
                    templateToUse = template
                    navController.navigate("edit_from_template")
                }
            )
        }

        composable("edit_from_template") {
            val tmpl = templateToUse
            EditScreen(
                initialPositivePrompt = tmpl?.positivePrompt ?: "",
                initialNegativePrompt = tmpl?.negativePrompt ?: "",
                initialTags = tmpl?.tags ?: emptyList(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("workflows") {
            WorkflowScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddWorkflow = { navController.navigate("workflow_edit") },
                onEditWorkflow = { workflowId ->
                    navController.navigate("workflow_edit/$workflowId")
                }
            )
        }

        composable("workflow_edit") {
            WorkflowEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "workflow_edit/{workflowId}",
            arguments = listOf(navArgument("workflowId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workflowId = backStackEntry.arguments?.getString("workflowId")
            WorkflowEditScreen(
                workflowId = workflowId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            GeneratedHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideo = { path ->
                    navController.navigate("video/${URLEncoder.encode(path, "UTF-8")}")
                }
            )
        }

        composable("comfyui_history") {
            ComfyUIHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "generate/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId") ?: return@composable
            val detailViewModel: DetailViewModel = viewModel()
            val promptData by detailViewModel.promptData.collectAsState()

            LaunchedEffect(promptId) {
                detailViewModel.loadPrompt(promptId)
            }

            GenerateScreen(
                promptData = promptData,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideo = { path ->
                    navController.navigate("video/${URLEncoder.encode(path, "UTF-8")}")
                }
            )
        }

        composable("video") {
            VideoGenerateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "video/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath")?.let { URLDecoder.decode(it, "UTF-8") }
            VideoGenerateScreen(
                imagePath = imagePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
