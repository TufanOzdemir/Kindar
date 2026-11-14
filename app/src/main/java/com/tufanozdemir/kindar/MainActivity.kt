package com.tufanozdemir.kindar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tufanozdemir.kindar.ui.MainViewModel
import com.tufanozdemir.kindar.ui.ProfileViewModel
import com.tufanozdemir.kindar.ui.screens.AddEventScreen
import com.tufanozdemir.kindar.ui.screens.GuideScreen
import com.tufanozdemir.kindar.ui.screens.HomeScreen
import com.tufanozdemir.kindar.ui.screens.NewProfileScreen
import com.tufanozdemir.kindar.ui.screens.PeopleScreen
import com.tufanozdemir.kindar.ui.screens.ProfileScreen
import com.tufanozdemir.kindar.ui.screens.SelfScreen
import com.tufanozdemir.kindar.ui.theme.KindarTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as KindarApp
        setContent {
            KindarTheme {
                KindarRoot(app)
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val title: String) {
    HOME("home", "Bugün", Icons.Default.Home, "Kindar"),
    PEOPLE("people", "Kişiler", Icons.Default.Person, "Kişiler"),
    SELF("self", "Karnem", Icons.Default.Face, "Karnem"),
    GUIDE("guide", "Strateji", Icons.Default.Info, "Strateji rehberi"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindarRoot(app: KindarApp) {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel { MainViewModel(app) }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(vm.hasContactsPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) {
            vm.onContactsPermissionGranted()
        } else {
            vm.showMessage("Rehber izni olmadan kişileri elle ekleyebilirsin.")
        }
    }
    val requestContacts = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            vm.exportTo { json ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Dosya açılamadı")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importFrom {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: error("Dosya açılamadı")
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }

    Scaffold(
        topBar = {
            if (tab != null) {
                TopAppBar(
                    title = { Text(tab.title) },
                    actions = {
                        if ((tab == Tab.HOME || tab == Tab.PEOPLE) && hasPermission) {
                            if (syncing) {
                                Box(Modifier.size(48.dp)) {
                                    CircularProgressIndicator(Modifier.size(24.dp).align(androidx.compose.ui.Alignment.Center))
                                }
                            } else {
                                IconButton(onClick = vm::syncContacts) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Rehberi senkronize et")
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (tab != null) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { nav.navigateTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.PEOPLE) {
                FloatingActionButton(onClick = { nav.navigate("profile/new") }) {
                    Icon(Icons.Default.Add, contentDescription = "Kişi ekle")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController = nav, startDestination = Tab.HOME.route) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    state = state,
                    hasContactsPermission = hasPermission,
                    onRequestContacts = requestContacts,
                    onOpenProfile = { nav.navigate("profile/$it") },
                    contentPadding = padding,
                )
            }
            composable(Tab.PEOPLE.route) {
                PeopleScreen(
                    summaries = state.summaries,
                    onOpenProfile = { nav.navigate("profile/$it") },
                    contentPadding = padding,
                )
            }
            composable(Tab.SELF.route) {
                SelfScreen(
                    report = state.report,
                    onExport = {
                        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("kindar-yedek-$stamp.json")
                    },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    contentPadding = padding,
                )
            }
            composable(Tab.GUIDE.route) { GuideScreen(contentPadding = padding) }

            composable("profile/new") {
                NewProfileScreen(
                    onBack = { nav.popBackStack() },
                    onSave = { name, phone, type, importance, notes ->
                        vm.addProfile(name, phone, type, importance, notes) { id ->
                            nav.popBackStack()
                            nav.navigate("profile/$id")
                        }
                    },
                )
            }
            composable(
                "profile/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val pvm: ProfileViewModel = viewModel(key = "profile-$id") { ProfileViewModel(app, id) }
                val pState by pvm.state.collectAsStateWithLifecycle()
                ProfileScreen(
                    state = pState,
                    onBack = { nav.popBackStack() },
                    onAddEvent = { nav.navigate("profile/$id/event") },
                    onDeleteEvent = pvm::deleteEvent,
                    onUpdateProfile = pvm::updateProfile,
                    onDeleteProfile = { pvm.deleteProfile { nav.popBackStack() } },
                )
            }
            composable(
                "profile/{id}/event",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                val pvm: ProfileViewModel = viewModel(key = "profile-event-$id") { ProfileViewModel(app, id) }
                val pState by pvm.state.collectAsStateWithLifecycle()
                AddEventScreen(
                    personName = pState.profile?.displayName ?: "",
                    onBack = { nav.popBackStack() },
                    onSave = { kind, points, title, note, occurredAt ->
                        pvm.addEvent(kind, points, title, note, occurredAt)
                        nav.popBackStack()
                    },
                )
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
