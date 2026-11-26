package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.TailorRepository
import com.example.model.Customer
import com.example.model.CustomerSortOption
import com.example.model.LanguageMode
import com.example.model.MeasurementHistory
import com.example.model.MeasurementUnit
import com.example.model.ShalwarKameezMeasurement
import com.example.model.ShopProfile
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TailorViewModel(
    private val repository: TailorRepository,
    private val preferencesManager: PreferencesManager,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(CustomerSortOption.RECENT)
    val sortOption: StateFlow<CustomerSortOption> = _sortOption.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val languageMode: StateFlow<LanguageMode> = preferencesManager.languageMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LanguageMode.BOTH
        )

    val defaultUnit: StateFlow<MeasurementUnit> = preferencesManager.defaultUnit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MeasurementUnit.INCHES
        )

    val shopProfile: StateFlow<ShopProfile?> = repository.observeShopProfile(currentUserId)
        .catch { error ->
            val firestoreError = unwrapFirestoreErrorOrThrowCancellation(error)
            Log.w(TAG, "Error observing shop profile", firestoreError)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers(currentUserId)
        .map<List<Customer>, UiState<List<Customer>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreErrorOrThrowCancellation(error)
            Log.w(TAG, "Error observing customers", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load customers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredCustomersState: StateFlow<UiState<List<Customer>>> = combine(
        customersState,
        _searchQuery,
        _sortOption
    ) { state, query, sort ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim().lowercase()
                val filtered = if (trimmed.isEmpty()) {
                    state.data
                } else {
                    state.data.filter { customer ->
                        customer.fullName.lowercase().contains(trimmed) ||
                            customer.mobileNumber.lowercase().contains(trimmed) ||
                            customer.customerCode.lowercase().contains(trimmed) ||
                            customer.address.lowercase().contains(trimmed)
                    }
                }
                val sorted = when (sort) {
                    CustomerSortOption.RECENT -> filtered
                    CustomerSortOption.NAME_ASC -> filtered.sortedBy { it.fullName.lowercase() }
                    CustomerSortOption.CUSTOMER_CODE -> filtered.sortedBy { it.customerCode.lowercase() }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCustomerState: StateFlow<UiState<Customer?>> = _selectedCustomerId
        .flatMapLatest { customerId ->
            if (customerId.isNullOrBlank()) {
                flowOf(UiState.Success(null))
            } else {
                repository.observeCustomer(currentUserId, customerId)
                    .map<Customer?, UiState<Customer?>> { UiState.Success(it) }
                    .catch { error ->
                        val rootError = unwrapFirestoreErrorOrThrowCancellation(error)
                        Log.w(TAG, "Error observing customer $customerId", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load customer"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedCustomerHistoryState: StateFlow<UiState<List<MeasurementHistory>>> = _selectedCustomerId
        .flatMapLatest { customerId ->
            if (customerId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMeasurementHistory(currentUserId, customerId)
                    .map<List<MeasurementHistory>, UiState<List<MeasurementHistory>>> { UiState.Success(it) }
                    .catch { error ->
                        val rootError = unwrapFirestoreErrorOrThrowCancellation(error)
                        Log.w(TAG, "Error observing history for $customerId", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load history"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: CustomerSortOption) {
        _sortOption.value = option
    }

    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            preferencesManager.setLanguageMode(mode)
        }
    }

    fun setDefaultUnit(unit: MeasurementUnit) {
        viewModelScope.launch {
            preferencesManager.setDefaultUnit(unit)
        }
    }

    fun generateUniqueCustomerCode(): String {
        val currentList = (customersState.value as? UiState.Success)?.data.orEmpty()
        val existingCodes = currentList.map { it.customerCode.uppercase() }.toSet()
        val maxNumeric = currentList.mapNotNull { customer ->
            Regex("TB-(\\d+)").find(customer.customerCode.uppercase())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull() ?: 1000

        var candidateNum = maxNumeric + 1
        while ("TB-$candidateNum" in existingCodes) {
            candidateNum++
        }
        return "TB-$candidateNum"
    }

    fun createCustomer(
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.createCustomer(
                userId = currentUserId,
                customerCode = customerCode,
                fullName = fullName,
                mobileNumber = mobileNumber,
                address = address,
                measurementDate = measurementDate,
                notes = notes,
                measurement = measurement
            )
            _isSaving.value = false
            result.onSuccess { newId ->
                _snackbarEvents.tryEmit("Customer saved: $fullName")
                onSuccess(newId)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to save customer")
            }
        }
    }

    fun updateCustomerInfo(
        customerId: String,
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateCustomerInfo(
                userId = currentUserId,
                customerId = customerId,
                customerCode = customerCode,
                fullName = fullName,
                mobileNumber = mobileNumber,
                address = address,
                measurementDate = measurementDate,
                notes = notes
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Customer information updated")
                onSuccess()
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to update customer")
            }
        }
    }

    fun updateCustomerMeasurements(
        customerId: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement,
        recordHistory: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateCustomerMeasurements(
                userId = currentUserId,
                customerId = customerId,
                measurementDate = measurementDate,
                notes = notes,
                measurement = measurement,
                recordHistory = recordHistory
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Measurements updated & saved to history")
                onSuccess()
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to update measurements")
            }
        }
    }

    fun deleteCustomer(
        customerId: String,
        customerName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteCustomer(currentUserId, customerId)
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Deleted $customerName")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to delete customer")
            }
        }
    }

    fun deleteHistoryEntry(customerId: String, historyId: String) {
        viewModelScope.launch {
            val result = repository.deleteHistoryEntry(currentUserId, customerId, historyId)
            result.onSuccess {
                _snackbarEvents.tryEmit("History entry removed")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to remove history entry")
            }
        }
    }

    fun saveShopProfile(
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        preferredUnit: MeasurementUnit,
        preferredLanguage: LanguageMode,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            preferencesManager.setDefaultUnit(preferredUnit)
            preferencesManager.setLanguageMode(preferredLanguage)
            val result = repository.saveShopProfile(
                userId = currentUserId,
                shopName = shopName,
                shopPhone = shopPhone,
                shopAddress = shopAddress,
                preferredUnit = preferredUnit,
                preferredLanguage = preferredLanguage
            )
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Shop profile saved")
                onDone(true, "Shop profile saved")
            }.onFailure { err ->
                onDone(false, err.localizedMessage ?: "Failed to save shop profile")
            }
        }
    }

    fun showMessage(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }

    private fun unwrapFirestoreErrorOrThrowCancellation(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "TailorVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
