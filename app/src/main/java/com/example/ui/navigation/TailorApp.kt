package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.signOutUser
import com.example.data.PreferencesManager
import com.example.data.TailorRepository
import com.example.localization.AppStrings
import com.example.model.LanguageMode
import com.example.model.UiState
import com.example.ui.components.LanguageSelectorPills
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerFormMode
import com.example.ui.screens.CustomerFormScreen
import com.example.ui.screens.CustomersListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.TailorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object TailorRoutes {
    const val DASHBOARD = "dashboard"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val CUSTOMER_NEW = "customer_new"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val CUSTOMER_EDIT_INFO = "customer_edit_info/{customerId}"
    const val CUSTOMER_EDIT_MEASUREMENTS = "customer_edit_measurements/{customerId}"

    fun detail(customerId: String) = "customer_detail/$customerId"
    fun editInfo(customerId: String) = "customer_edit_info/$customerId"
    fun editMeasurements(customerId: String) = "customer_edit_measurements/$customerId"
}

@Composable
fun TailorApp() {
    val context = LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager(context) }
    val unauthLanguage by preferencesManager.languageMode.collectAsStateWithLifecycle(initialValue = LanguageMode.BOTH)
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            languageMode = unauthLanguage,
            onLanguageChange = { mode ->
                scope.launch { preferencesManager.setLanguageMode(mode) }
            },
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedTailorApp(
                currentUserId = uid,
                userEmail = user.email,
                preferencesManager = preferencesManager
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTailorApp(
    currentUserId: String,
    userEmail: String?,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: TailorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TailorViewModel(
                    repository = TailorRepository(db),
                    preferencesManager = preferencesManager,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val languageMode by viewModel.languageMode.collectAsStateWithLifecycle()
    val defaultUnit by viewModel.defaultUnit.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val filteredCustomersState by viewModel.filteredCustomersState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val selectedCustomerState by viewModel.selectedCustomerState.collectAsStateWithLifecycle()
    val selectedHistoryState by viewModel.selectedCustomerHistoryState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val isTopLevelRoute = currentRoute in listOf(
        TailorRoutes.DASHBOARD,
        TailorRoutes.CUSTOMERS,
        TailorRoutes.SETTINGS
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Text(
                                text = AppStrings.appTitle.primary(languageMode),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        actions = {
                            LanguageSelectorPills(
                                selectedMode = languageMode,
                                onSelectMode = { viewModel.setLanguageMode(it) },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(dest.route) {
                                            popUpTo(TailorRoutes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label.primary(languageMode)
                                    )
                                },
                                label = {
                                    Text(dest.label.primary(languageMode), maxLines = 1)
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(dest.route) {
                                            popUpTo(TailorRoutes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label.primary(languageMode)
                                    )
                                },
                                label = {
                                    Text(dest.label.primary(languageMode), maxLines = 1)
                                },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TailorRoutes.DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(TailorRoutes.DASHBOARD) {
                        DashboardScreen(
                            customersState = customersState,
                            filteredCustomersState = filteredCustomersState,
                            shopProfile = shopProfile,
                            searchQuery = searchQuery,
                            languageMode = languageMode,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onAddNewCustomer = {
                                navController.navigate(TailorRoutes.CUSTOMER_NEW)
                            },
                            onViewAllCustomers = {
                                navController.navigate(TailorRoutes.CUSTOMERS) {
                                    popUpTo(TailorRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onCustomerClick = { customer ->
                                viewModel.selectCustomer(customer.id)
                                navController.navigate(TailorRoutes.detail(customer.id))
                            }
                        )
                    }

                    composable(TailorRoutes.CUSTOMERS) {
                        CustomersListScreen(
                            filteredCustomersState = filteredCustomersState,
                            searchQuery = searchQuery,
                            sortOption = sortOption,
                            languageMode = languageMode,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSortOptionChange = viewModel::updateSortOption,
                            onAddNewCustomer = {
                                navController.navigate(TailorRoutes.CUSTOMER_NEW)
                            },
                            onCustomerClick = { customer ->
                                viewModel.selectCustomer(customer.id)
                                navController.navigate(TailorRoutes.detail(customer.id))
                            }
                        )
                    }

                    composable(TailorRoutes.SETTINGS) {
                        SettingsScreen(
                            shopProfile = shopProfile,
                            userEmail = userEmail,
                            languageMode = languageMode,
                            defaultUnit = defaultUnit,
                            isSaving = isSaving,
                            onLanguageChange = viewModel::setLanguageMode,
                            onDefaultUnitChange = viewModel::setDefaultUnit,
                            onSaveShopProfile = { name, phone, addr, unit, lang ->
                                viewModel.saveShopProfile(name, phone, addr, unit, lang) { _, msg ->
                                    viewModel.showMessage(msg)
                                }
                            },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }

                    composable(TailorRoutes.CUSTOMER_NEW) {
                        val suggestedCode = remember(customersState) {
                            viewModel.generateUniqueCustomerCode()
                        }
                        CustomerFormScreen(
                            mode = CustomerFormMode.NEW_CUSTOMER,
                            existingCustomer = null,
                            defaultUnit = defaultUnit,
                            suggestedCustomerCode = suggestedCode,
                            isSaving = isSaving,
                            languageMode = languageMode,
                            onRegenerateCustomerCode = viewModel::generateUniqueCustomerCode,
                            onCreateCustomer = { code, name, mobile, addr, date, notes, meas, onError ->
                                viewModel.createCustomer(
                                    customerCode = code,
                                    fullName = name,
                                    mobileNumber = mobile,
                                    address = addr,
                                    measurementDate = date,
                                    notes = notes,
                                    measurement = meas,
                                    onSuccess = { newId ->
                                        viewModel.selectCustomer(newId)
                                        navController.navigate(TailorRoutes.detail(newId)) {
                                            popUpTo(TailorRoutes.CUSTOMER_NEW) { inclusive = true }
                                        }
                                    },
                                    onError = onError
                                )
                            },
                            onUpdateCustomerInfo = { _, _, _, _, _, _, _, _ -> },
                            onUpdateMeasurements = { _, _, _, _, _ -> },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = TailorRoutes.CUSTOMER_DETAIL,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { entry ->
                        val customerId = entry.arguments?.getString("customerId").orEmpty()
                        LaunchedEffect(customerId) {
                            viewModel.selectCustomer(customerId)
                        }
                        CustomerDetailScreen(
                            customerState = selectedCustomerState,
                            historyState = selectedHistoryState,
                            shopProfile = shopProfile,
                            languageMode = languageMode,
                            onEditInfo = { cust ->
                                navController.navigate(TailorRoutes.editInfo(cust.id))
                            },
                            onEditMeasurements = { cust ->
                                navController.navigate(TailorRoutes.editMeasurements(cust.id))
                            },
                            onRestoreHistoryMeasurement = { cust, hist ->
                                viewModel.updateCustomerMeasurements(
                                    customerId = cust.id,
                                    measurementDate = hist.measurementDate,
                                    notes = hist.notes,
                                    measurement = hist.measurement,
                                    recordHistory = false,
                                    onSuccess = {
                                        viewModel.showMessage("Applied measurement from ${hist.measurementDate}")
                                    },
                                    onError = { err -> viewModel.showMessage(err) }
                                )
                            },
                            onDeleteCustomer = { cust ->
                                viewModel.deleteCustomer(cust.id, cust.fullName) {
                                    navController.popBackStack()
                                }
                            },
                            onShowMessage = viewModel::showMessage,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = TailorRoutes.CUSTOMER_EDIT_INFO,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { entry ->
                        val customerId = entry.arguments?.getString("customerId").orEmpty()
                        LaunchedEffect(customerId) {
                            viewModel.selectCustomer(customerId)
                        }
                        val existing = (selectedCustomerState as? UiState.Success)?.data
                        CustomerFormScreen(
                            mode = CustomerFormMode.EDIT_INFO,
                            existingCustomer = existing,
                            defaultUnit = defaultUnit,
                            suggestedCustomerCode = existing?.customerCode ?: "",
                            isSaving = isSaving,
                            languageMode = languageMode,
                            onRegenerateCustomerCode = viewModel::generateUniqueCustomerCode,
                            onCreateCustomer = { _, _, _, _, _, _, _, _ -> },
                            onUpdateCustomerInfo = { id, code, name, mobile, addr, date, notes, onError ->
                                viewModel.updateCustomerInfo(
                                    customerId = id,
                                    customerCode = code,
                                    fullName = name,
                                    mobileNumber = mobile,
                                    address = addr,
                                    measurementDate = date,
                                    notes = notes,
                                    onSuccess = { navController.popBackStack() },
                                    onError = onError
                                )
                            },
                            onUpdateMeasurements = { _, _, _, _, _ -> },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = TailorRoutes.CUSTOMER_EDIT_MEASUREMENTS,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { entry ->
                        val customerId = entry.arguments?.getString("customerId").orEmpty()
                        LaunchedEffect(customerId) {
                            viewModel.selectCustomer(customerId)
                        }
                        val existing = (selectedCustomerState as? UiState.Success)?.data
                        CustomerFormScreen(
                            mode = CustomerFormMode.EDIT_MEASUREMENTS,
                            existingCustomer = existing,
                            defaultUnit = defaultUnit,
                            suggestedCustomerCode = existing?.customerCode ?: "",
                            isSaving = isSaving,
                            languageMode = languageMode,
                            onRegenerateCustomerCode = viewModel::generateUniqueCustomerCode,
                            onCreateCustomer = { _, _, _, _, _, _, _, _ -> },
                            onUpdateCustomerInfo = { _, _, _, _, _, _, _, _ -> },
                            onUpdateMeasurements = { id, date, notes, meas, onError ->
                                viewModel.updateCustomerMeasurements(
                                    customerId = id,
                                    measurementDate = date,
                                    notes = notes,
                                    measurement = meas,
                                    recordHistory = true,
                                    onSuccess = { navController.popBackStack() },
                                    onError = onError
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: com.example.localization.BiText,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(
        route = TailorRoutes.DASHBOARD,
        label = AppStrings.navDashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TopLevelDestination(
        route = TailorRoutes.CUSTOMERS,
        label = AppStrings.navCustomers,
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    TopLevelDestination(
        route = TailorRoutes.SETTINGS,
        label = AppStrings.navSettings,
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront
    )
)
