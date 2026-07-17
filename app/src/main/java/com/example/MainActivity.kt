package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.FinanceViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.GlobalToast
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial deep link intent on launch
        intent?.data?.let { uri ->
            if (uri.scheme == "personalfinance" && uri.host == "auth") {
                viewModel.handleDeepLink(uri)
            }
        }

        setContent {
            MyApplicationTheme {
                val isOnboarded by viewModel.isOnboarded.collectAsState()
                val currentTab by viewModel.currentTab.collectAsState()
                val toastState by viewModel.toastState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = isOnboarded,
                        animationSpec = tween(durationMillis = 350),
                        label = "onboard_crossfade"
                    ) { onboarded ->
                        if (!onboarded) {
                            OnboardingScreen(viewModel = viewModel)
                        } else {
                            MainAppShell(
                                viewModel = viewModel,
                                currentTab = currentTab,
                                onTabSelected = { viewModel.setTab(it) }
                            )
                        }
                    }

                    // Global floating toast overlaid at the absolute top of everything
                    GlobalToast(
                        toastState = toastState,
                        onDismiss = { viewModel.dismissToast() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link intent when app is already running in background
        intent.data?.let { uri ->
            if (uri.scheme == "personalfinance" && uri.host == "auth") {
                viewModel.handleDeepLink(uri)
            }
        }
    }
}

@Composable
fun MainAppShell(
    viewModel: FinanceViewModel,
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_navigation_bar"),
                containerColor = Color(0xFF1E293B),
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    AppTab.OVERVIEW to ("Overview" to Icons.Default.Assessment),
                    AppTab.EXPENSES to ("Expenses" to Icons.Default.Payments),
                    AppTab.SAVINGS to ("Savings" to Icons.Default.Savings),
                    AppTab.SETTINGS to ("Settings" to Icons.Default.Settings)
                )

                tabs.forEach { (tab, details) ->
                    val (label, icon) = details
                    val isSelected = currentTab == tab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.testTag("nav_icon_${label.lowercase()}")
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.testTag("nav_label_${label.lowercase()}")
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6366F1), // Professional Indigo Accent
                            selectedTextColor = Color(0xFF6366F1),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF334155) // Dark slate indicator
                        ),
                        modifier = Modifier.testTag("nav_item_${label.lowercase()}")
                    )
                }
            }
        },
        containerColor = Color(0xFF0F172A) // Professional Slate Dark Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.OVERVIEW -> DashboardScreen(viewModel = viewModel)
                AppTab.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                AppTab.SAVINGS -> SavingsScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
