package com.starmaen.accountingstar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmaen.accountingstar.ui.AppUiState
import com.starmaen.accountingstar.ui.AppViewModel
import com.starmaen.accountingstar.ui.Dest
import com.starmaen.accountingstar.ui.screens.ContactsScreen
import com.starmaen.accountingstar.ui.screens.DashboardScreen
import com.starmaen.accountingstar.ui.screens.InstallmentsScreen
import com.starmaen.accountingstar.ui.screens.InventoryScreen
import com.starmaen.accountingstar.ui.screens.InvoicesScreen
import com.starmaen.accountingstar.ui.screens.LoginScreen
import com.starmaen.accountingstar.ui.screens.PosScreen
import com.starmaen.accountingstar.ui.screens.ProductsScreen
import com.starmaen.accountingstar.ui.screens.ProjectsScreen
import com.starmaen.accountingstar.ui.screens.PurchaseScreen
import com.starmaen.accountingstar.ui.screens.ReportsScreen
import com.starmaen.accountingstar.ui.screens.SettingsScreen
import com.starmaen.accountingstar.ui.theme.AccountingStarTheme
import com.starmaen.accountingstar.ui.theme.BrandGold
import com.starmaen.accountingstar.ui.theme.BrandGreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountingStarTheme {
                AppRoot(viewModel)
            }
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    when {
        state.loading && state.currentUser == null -> LoadingScreen()
        state.currentUser == null -> LoginScreen(vm)
        else -> MainScaffold(vm, state)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandGreen)
            Text("جارٍ تحميل نجم المحاسبة...", Modifier.padding(top = 12.dp), fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(vm: AppViewModel, state: AppUiState) {
    var dest by rememberSaveable { mutableStateOf(Dest.DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHost.showSnackbar(message)
            vm.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(18.dp)) {
                    Text("نجم المحاسبة", style = MaterialTheme.typography.headlineSmall, color = BrandGreen)
                    Text(
                        state.currentProject?.name ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${state.currentUser?.fullName ?: ""} • ${state.currentBranchName}",
                        fontSize = 12.sp,
                        color = Color(0xFF77736B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Dest.values().forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = dest == item,
                        onClick = {
                            dest = item
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BrandGreen.copy(alpha = 0.12f)
                        )
                    )
                }
                Text(
                    "الإصدار 1.0 — يعمل دون إنترنت، وكل مشروع معزول ببياناته",
                    fontSize = 11.sp,
                    color = Color(0xFF8A867D),
                    modifier = Modifier.padding(18.dp)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(dest.label, fontSize = 18.sp, color = Color.White)
                            Text(
                                "${state.currentProject?.name ?: ""} • ${state.activeBranchName}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                        }
                        IconButton(onClick = { vm.logout() }) {
                            Icon(Icons.Default.Logout, contentDescription = "خروج", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(
                        Triple(Dest.DASHBOARD, Icons.Default.Home, "الرئيسية"),
                        Triple(Dest.POS, Icons.Default.PointOfSale, "البيع"),
                        Triple(Dest.INVOICES, Icons.Default.ReceiptLong, "الفواتير"),
                        Triple(Dest.REPORTS, Icons.Default.BarChart, "التقارير")
                    ).forEach { (target, icon, label) ->
                        NavigationBarItem(
                            selected = dest == target,
                            onClick = { dest = target },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (dest) {
                    Dest.DASHBOARD -> DashboardScreen(vm, state, onNavigate = { dest = it })
                    Dest.POS -> PosScreen(vm, state)
                    Dest.PURCHASE -> PurchaseScreen(vm, state)
                    Dest.PRODUCTS -> ProductsScreen(vm, state)
                    Dest.INVENTORY -> InventoryScreen(vm, state)
                    Dest.INVOICES -> InvoicesScreen(vm, state)
                    Dest.CUSTOMERS -> ContactsScreen(vm, state, startWithCustomers = true)
                    Dest.SUPPLIERS -> ContactsScreen(vm, state, startWithCustomers = false)
                    Dest.INSTALLMENTS -> InstallmentsScreen(vm, state)
                    Dest.REPORTS -> ReportsScreen(vm, state)
                    Dest.SETTINGS -> SettingsScreen(vm, state)
                    Dest.PROJECTS -> ProjectsScreen(vm, state)
                }
            }
        }
    }
}
