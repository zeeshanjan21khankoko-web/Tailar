package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.Customer
import com.example.model.LanguageMode
import com.example.model.MeasurementUnit
import com.example.model.ShalwarKameezMeasurement
import com.example.ui.components.ShalwarKameezMeasurementFormSection
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CustomerFormMode {
    NEW_CUSTOMER,
    EDIT_INFO,
    EDIT_MEASUREMENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    mode: CustomerFormMode,
    existingCustomer: Customer?,
    defaultUnit: MeasurementUnit,
    suggestedCustomerCode: String,
    isSaving: Boolean,
    languageMode: LanguageMode,
    onRegenerateCustomerCode: () -> String,
    onCreateCustomer: (
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement,
        onError: (String) -> Unit
    ) -> Unit,
    onUpdateCustomerInfo: (
        customerId: String,
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String,
        onError: (String) -> Unit
    ) -> Unit,
    onUpdateMeasurements: (
        customerId: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement,
        onError: (String) -> Unit
    ) -> Unit,
    onBack: () -> Unit
) {
    val todayFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    var currentStep by remember(mode) {
        mutableIntStateOf(if (mode == CustomerFormMode.EDIT_MEASUREMENTS) 1 else 0)
    }

    var fullName by remember(existingCustomer?.id, mode) {
        mutableStateOf(existingCustomer?.fullName ?: "")
    }
    var mobileNumber by remember(existingCustomer?.id, mode) {
        mutableStateOf(existingCustomer?.mobileNumber ?: "")
    }
    var address by remember(existingCustomer?.id, mode) {
        mutableStateOf(existingCustomer?.address ?: "")
    }
    var customerCode by remember(existingCustomer?.id, mode) {
        mutableStateOf(existingCustomer?.customerCode?.ifBlank { suggestedCustomerCode } ?: suggestedCustomerCode)
    }
    var measurementDate by remember(existingCustomer?.id, mode) {
        mutableStateOf(
            if (mode == CustomerFormMode.EDIT_MEASUREMENTS) todayFormatted
            else existingCustomer?.measurementDate?.ifBlank { todayFormatted } ?: todayFormatted
        )
    }
    var notes by remember(existingCustomer?.id, mode) {
        mutableStateOf(existingCustomer?.notes ?: "")
    }
    var measurement by remember(existingCustomer?.id, mode) {
        mutableStateOf(
            existingCustomer?.currentMeasurement
                ?: ShalwarKameezMeasurement(unit = defaultUnit.code)
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        if (mode == CustomerFormMode.NEW_CUSTOMER && currentStep == 1) {
            currentStep = 0
        } else {
            onBack()
        }
    }

    fun validateCustomerInfo(): Boolean {
        return when {
            fullName.trim().isEmpty() -> {
                validationError = "Please enter customer full name."
                false
            }
            mobileNumber.trim().length < 4 -> {
                validationError = "Please enter a valid mobile number (at least 4 digits)."
                false
            }
            customerCode.trim().isEmpty() -> {
                validationError = "Customer ID cannot be empty."
                false
            }
            measurementDate.trim().isEmpty() -> {
                validationError = "Please select a measurement date."
                false
            }
            else -> {
                validationError = null
                true
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            measurementDate = fmt.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(AppStrings.cancel.primary(languageMode))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val titleBi = when (mode) {
        CustomerFormMode.NEW_CUSTOMER -> AppStrings.newCustomerTitle
        CustomerFormMode.EDIT_INFO -> AppStrings.editCustomerTitle
        CustomerFormMode.EDIT_MEASUREMENTS -> AppStrings.editMeasurementsTitle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleBi.primary(languageMode),
                            style = MaterialTheme.typography.titleLarge
                        )
                        titleBi.secondary(languageMode)?.let { ur ->
                            Text(
                                text = ur,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (mode == CustomerFormMode.NEW_CUSTOMER && currentStep == 1) {
                                currentStep = 0
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (mode == CustomerFormMode.NEW_CUSTOMER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StepTabPill(
                            stepNumber = "1",
                            title = AppStrings.stepCustomerInfo.inline(languageMode),
                            selected = currentStep == 0,
                            completed = currentStep == 1,
                            onClick = { currentStep = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        StepTabPill(
                            stepNumber = "2",
                            title = AppStrings.stepMeasurements.inline(languageMode),
                            selected = currentStep == 1,
                            completed = false,
                            onClick = {
                                if (validateCustomerInfo()) currentStep = 1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (currentStep == 0 && mode != CustomerFormMode.EDIT_MEASUREMENTS) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it.take(100); validationError = null },
                                label = { Text("${AppStrings.fullName.inline(languageMode)} *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_full_name")
                            )

                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { mobileNumber = it.take(25); validationError = null },
                                label = { Text("${AppStrings.mobileNumber.inline(languageMode)} *") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_mobile_number")
                            )

                            OutlinedTextField(
                                value = customerCode,
                                onValueChange = { customerCode = it.take(32); validationError = null },
                                label = { Text("${AppStrings.customerId.inline(languageMode)} *") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                trailingIcon = {
                                    if (mode == CustomerFormMode.NEW_CUSTOMER) {
                                        IconButton(
                                            onClick = { customerCode = onRegenerateCustomerCode() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Generate unique ID"
                                            )
                                        }
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_customer_code")
                            )

                            OutlinedTextField(
                                value = measurementDate,
                                onValueChange = { measurementDate = it.take(20); validationError = null },
                                label = { Text("${AppStrings.measurementDate.inline(languageMode)} *") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                trailingIcon = {
                                    TextButton(onClick = { showDatePicker = true }) {
                                        Text("Pick")
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_measurement_date")
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it.take(250) },
                                label = { Text(AppStrings.address.inline(languageMode)) },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                maxLines = 2,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_address")
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it.take(1000) },
                                label = { Text(AppStrings.notes.inline(languageMode)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                                minLines = 2,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_notes")
                            )
                        }
                    }

                    if (mode == CustomerFormMode.NEW_CUSTOMER) {
                        Button(
                            onClick = {
                                if (validateCustomerInfo()) currentStep = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("next_to_measurements_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = AppStrings.nextToMeasurements.inline(languageMode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else if (mode == CustomerFormMode.EDIT_INFO && existingCustomer != null) {
                        Button(
                            onClick = {
                                if (validateCustomerInfo()) {
                                    onUpdateCustomerInfo(
                                        existingCustomer.id,
                                        customerCode,
                                        fullName,
                                        mobileNumber,
                                        address,
                                        measurementDate,
                                        notes
                                    ) { err -> validationError = err }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("save_customer_info_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = AppStrings.updateCustomerInfo.inline(languageMode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    if (mode == CustomerFormMode.EDIT_MEASUREMENTS && existingCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = existingCustomer.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${existingCustomer.customerCode} • ${existingCustomer.mobileNumber}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(measurementDate)
                                }
                            }
                        }
                    }

                    ShalwarKameezMeasurementFormSection(
                        measurement = measurement,
                        onMeasurementChange = { measurement = it },
                        languageMode = languageMode
                    )

                    if (mode == CustomerFormMode.EDIT_MEASUREMENTS) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(1000) },
                            label = { Text(AppStrings.notes.inline(languageMode)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_measurement_notes")
                        )
                    }

                    Button(
                        onClick = {
                            if (mode == CustomerFormMode.NEW_CUSTOMER) {
                                if (validateCustomerInfo()) {
                                    onCreateCustomer(
                                        customerCode,
                                        fullName,
                                        mobileNumber,
                                        address,
                                        measurementDate,
                                        notes,
                                        measurement
                                    ) { err -> validationError = err }
                                }
                            } else if (mode == CustomerFormMode.EDIT_MEASUREMENTS && existingCustomer != null) {
                                onUpdateMeasurements(
                                    existingCustomer.id,
                                    measurementDate,
                                    notes,
                                    measurement
                                ) { err -> validationError = err }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_measurements_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mode == CustomerFormMode.NEW_CUSTOMER) {
                                    AppStrings.saveCustomerAndMeasurements.inline(languageMode)
                                } else {
                                    AppStrings.saveNewMeasurementVersion.inline(languageMode)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepTabPill(
    stepNumber: String,
    title: String,
    selected: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            completed -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            ) {
                Text(
                    text = if (completed) "✓" else stepNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
