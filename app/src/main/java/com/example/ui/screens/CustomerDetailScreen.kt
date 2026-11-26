package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.localization.BiText
import com.example.model.Customer
import com.example.model.LanguageMode
import com.example.model.MeasurementHistory
import com.example.model.MeasurementUnit
import com.example.model.ShalwarKameezMeasurement
import com.example.model.ShopProfile
import com.example.model.UiState
import com.example.receipt.ReceiptPdfAndShareHelper
import com.example.ui.components.BilingualText
import com.example.ui.components.DeleteCustomerDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WhatsAppGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerState: UiState<Customer?>,
    historyState: UiState<List<MeasurementHistory>>,
    shopProfile: ShopProfile?,
    languageMode: LanguageMode,
    onEditInfo: (Customer) -> Unit,
    onEditMeasurements: (Customer) -> Unit,
    onRestoreHistoryMeasurement: (Customer, MeasurementHistory) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showWhatsAppChoice by remember { mutableStateOf(false) }
    var pendingPdfTarget by remember {
        mutableStateOf<Triple<Customer, ShalwarKameezMeasurement, Pair<String, String>>?>(null)
    }
    var pendingWhatsAppTarget by remember {
        mutableStateOf<Triple<Customer, ShalwarKameezMeasurement, Pair<String, String>>?>(null)
    }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val target = pendingPdfTarget
        pendingPdfTarget = null
        if (uri != null && target != null) {
            val (cust, meas, dateAndNotes) = target
            ReceiptPdfAndShareHelper.writePdfToUri(
                context = context,
                uri = uri,
                customer = cust,
                measurement = meas,
                measurementDate = dateAndNotes.first,
                notes = dateAndNotes.second,
                shopProfile = shopProfile,
                languageMode = languageMode
            ).onSuccess {
                onShowMessage("PDF receipt saved successfully")
            }.onFailure { err ->
                onShowMessage(err.localizedMessage ?: "Failed to save PDF")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val cust = (customerState as? UiState.Success)?.data
                    Column {
                        Text(
                            text = cust?.fullName ?: AppStrings.stepCustomerInfo.primary(languageMode),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        if (cust != null) {
                            Text(
                                text = cust.customerCode,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val cust = (customerState as? UiState.Success)?.data
                    if (cust != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = AppStrings.deleteCustomer.primary(languageMode),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            when (customerState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = customerState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                is UiState.Success -> {
                    val customer = customerState.data
                    if (customer == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Customer record not found")
                        }
                    } else {
                        var previewUnit by remember(customer.id, customer.currentMeasurement.unit) {
                            mutableStateOf(MeasurementUnit.fromCode(customer.currentMeasurement.unit))
                        }
                        val displayedMeasurement = remember(customer.currentMeasurement, previewUnit) {
                            customer.currentMeasurement.convertedTo(previewUnit)
                        }

                        if (showDeleteDialog) {
                            DeleteCustomerDialog(
                                customerName = customer.fullName,
                                languageMode = languageMode,
                                onConfirmDelete = {
                                    showDeleteDialog = false
                                    onDeleteCustomer(customer)
                                },
                                onDismiss = { showDeleteDialog = false }
                            )
                        }

                        if (showWhatsAppChoice && pendingWhatsAppTarget != null) {
                            val (cust, meas, dateNotes) = pendingWhatsAppTarget!!
                            AlertDialog(
                                onDismissRequest = { showWhatsAppChoice = false },
                                title = { Text(AppStrings.shareWhatsApp.inline(languageMode)) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                showWhatsAppChoice = false
                                                ReceiptPdfAndShareHelper.shareTextViaWhatsApp(
                                                    context = context,
                                                    customer = cust,
                                                    measurement = meas,
                                                    measurementDate = dateNotes.first,
                                                    notes = dateNotes.second,
                                                    shopProfile = shopProfile,
                                                    languageMode = languageMode
                                                ).onFailure {
                                                    onShowMessage("Could not open WhatsApp")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("whatsapp_share_text_button")
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(AppStrings.shareWhatsAppText.inline(languageMode))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                showWhatsAppChoice = false
                                                ReceiptPdfAndShareHelper.sharePdfViaWhatsApp(
                                                    context = context,
                                                    customer = cust,
                                                    measurement = meas,
                                                    measurementDate = dateNotes.first,
                                                    notes = dateNotes.second,
                                                    shopProfile = shopProfile,
                                                    languageMode = languageMode
                                                ).onFailure {
                                                    onShowMessage("Could not share PDF receipt")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("whatsapp_share_pdf_button")
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(AppStrings.shareWhatsAppPdf.inline(languageMode))
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showWhatsAppChoice = false }) {
                                        Text(AppStrings.cancel.primary(languageMode))
                                    }
                                }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Profile Card
                            item {
                                CustomerProfileHeaderCard(
                                    customer = customer,
                                    languageMode = languageMode,
                                    onCallClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobileNumber}"))
                                        context.startActivity(intent)
                                    },
                                    onEditInfoClick = { onEditInfo(customer) }
                                )
                            }

                            // Print, PDF, WhatsApp Actions
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReceiptActionButton(
                                        icon = Icons.Default.Print,
                                        label = AppStrings.printReceipt.primary(languageMode),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        onClick = {
                                            ReceiptPdfAndShareHelper.printMeasurementReceipt(
                                                context = context,
                                                customer = customer,
                                                measurement = displayedMeasurement,
                                                measurementDate = customer.measurementDate,
                                                notes = customer.notes,
                                                shopProfile = shopProfile,
                                                languageMode = languageMode
                                            ).onFailure {
                                                onShowMessage(it.localizedMessage ?: "Print failed")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("print_receipt_button")
                                    )
                                    ReceiptActionButton(
                                        icon = Icons.Default.PictureAsPdf,
                                        label = AppStrings.downloadPdf.primary(languageMode),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        onClick = {
                                            pendingPdfTarget = Triple(
                                                customer,
                                                displayedMeasurement,
                                                customer.measurementDate to customer.notes
                                            )
                                            pdfSaverLauncher.launch(
                                                ReceiptPdfAndShareHelper.buildSuggestedFileName(customer)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("download_pdf_button")
                                    )
                                    ReceiptActionButton(
                                        icon = Icons.Default.Share,
                                        label = AppStrings.shareWhatsApp.primary(languageMode),
                                        containerColor = WhatsAppGreen,
                                        contentColor = Color.White,
                                        onClick = {
                                            pendingWhatsAppTarget = Triple(
                                                customer,
                                                displayedMeasurement,
                                                customer.measurementDate to customer.notes
                                            )
                                            showWhatsAppChoice = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("share_whatsapp_button")
                                    )
                                }
                            }

                            // Update Measurements Button + Unit Preview Switch
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onEditMeasurements(customer) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("update_measurements_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Straighten, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = AppStrings.updateMeasurements.primary(languageMode),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .height(50.dp)
                                            .clickable {
                                                previewUnit = if (previewUnit == MeasurementUnit.INCHES) {
                                                    MeasurementUnit.CENTIMETERS
                                                } else {
                                                    MeasurementUnit.INCHES
                                                }
                                            }
                                            .testTag("preview_unit_switch")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = previewUnit.code.uppercase(),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Kameez Measurements Card
                            item {
                                MeasurementDisplayCard(
                                    title = AppStrings.kameezSection,
                                    unitCode = displayedMeasurement.unit,
                                    languageMode = languageMode,
                                    rows = listOf(
                                        AppStrings.kameezLength to displayedMeasurement.kameezLength,
                                        AppStrings.shoulder to displayedMeasurement.shoulder,
                                        AppStrings.chest to displayedMeasurement.chest,
                                        AppStrings.kameezWaist to displayedMeasurement.kameezWaist,
                                        AppStrings.kameezHip to displayedMeasurement.kameezHip,
                                        AppStrings.sleeveLength to displayedMeasurement.sleeveLength,
                                        AppStrings.armhole to displayedMeasurement.armhole,
                                        AppStrings.neck to displayedMeasurement.neck,
                                        AppStrings.cuff to displayedMeasurement.cuff,
                                        AppStrings.frontLength to displayedMeasurement.frontLength,
                                        AppStrings.backLength to displayedMeasurement.backLength,
                                        AppStrings.collar to displayedMeasurement.collar
                                    )
                                )
                            }

                            // Shalwar Measurements Card
                            item {
                                MeasurementDisplayCard(
                                    title = AppStrings.shalwarSection,
                                    unitCode = displayedMeasurement.unit,
                                    languageMode = languageMode,
                                    rows = listOf(
                                        AppStrings.shalwarLength to displayedMeasurement.shalwarLength,
                                        AppStrings.shalwarWaist to displayedMeasurement.shalwarWaist,
                                        AppStrings.shalwarHip to displayedMeasurement.shalwarHip,
                                        AppStrings.thigh to displayedMeasurement.thigh,
                                        AppStrings.knee to displayedMeasurement.knee,
                                        AppStrings.bottomPaicha to displayedMeasurement.bottomPaicha
                                    ),
                                    footerLabel = AppStrings.shalwarStyle,
                                    footerValue = AppStrings.formatShalwarStyle(displayedMeasurement.shalwarStyle, languageMode)
                                )
                            }

                            // History Header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    BilingualText(
                                        text = AppStrings.measurementHistory,
                                        languageMode = languageMode,
                                        primaryStyle = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }

                            when (historyState) {
                                is UiState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }
                                is UiState.Error -> {
                                    item {
                                        Text(
                                            text = historyState.message,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                is UiState.Success -> {
                                    val historyList = historyState.data
                                    if (historyList.isEmpty()) {
                                        item {
                                            Text(
                                                text = AppStrings.noHistoryYet.inline(languageMode),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        itemsIndexed(
                                            items = historyList,
                                            key = { _, item -> item.id }
                                        ) { index, entry ->
                                            HistoryEntryCard(
                                                index = historyList.size - index,
                                                entry = entry,
                                                languageMode = languageMode,
                                                onPrint = {
                                                    ReceiptPdfAndShareHelper.printMeasurementReceipt(
                                                        context = context,
                                                        customer = customer,
                                                        measurement = entry.measurement,
                                                        measurementDate = entry.measurementDate,
                                                        notes = entry.notes,
                                                        shopProfile = shopProfile,
                                                        languageMode = languageMode
                                                    )
                                                },
                                                onRestore = { onRestoreHistoryMeasurement(customer, entry) }
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerProfileHeaderCard(
    customer: Customer,
    languageMode: LanguageMode,
    onCallClick: () -> Unit,
    onEditInfoClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${AppStrings.customerId.primary(languageMode)}: ${customer.customerCode}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = onEditInfoClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("edit_customer_info_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.editInfo.primary(languageMode))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    InfoRow(icon = Icons.Default.Call, text = customer.mobileNumber)
                    InfoRow(icon = Icons.Outlined.CalendarToday, text = "${AppStrings.measurementDate.primary(languageMode)}: ${customer.measurementDate}")
                    if (customer.address.isNotBlank()) {
                        InfoRow(icon = Icons.Outlined.LocationOn, text = customer.address)
                    }
                    if (customer.notes.isNotBlank()) {
                        InfoRow(icon = Icons.AutoMirrored.Outlined.Notes, text = customer.notes)
                    }
                }
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.testTag("call_customer_button")
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call customer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReceiptActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MeasurementDisplayCard(
    title: BiText,
    unitCode: String,
    languageMode: LanguageMode,
    rows: List<Pair<BiText, String>>,
    footerLabel: BiText? = null,
    footerValue: String? = null
) {
    val unitShort = AppStrings.unitShort(unitCode, languageMode)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BilingualText(
                    text = title,
                    languageMode = languageMode,
                    primaryStyle = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = unitShort,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            rows.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (label, value) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                Text(
                                    text = label.primary(languageMode),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                label.secondary(languageMode)?.let { ur ->
                                    Text(
                                        text = ur,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (value.isBlank()) "—" else "$value $unitShort",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (footerLabel != null && !footerValue.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = footerLabel.inline(languageMode),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = footerValue,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    index: Int,
    entry: MeasurementHistory,
    languageMode: LanguageMode,
    onPrint: () -> Unit,
    onRestore: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val unitShort = AppStrings.unitShort(entry.measurement.unit, languageMode)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#$index • ${entry.measurementDate}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.notes.isNotBlank()) {
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = unitShort,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand history"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    val m = entry.measurement
                    Text(
                        text = "${AppStrings.kameezSection.primary(languageMode)}: L ${m.kameezLength.ifBlank { "—" }}, Sh ${m.shoulder.ifBlank { "—" }}, Ch ${m.chest.ifBlank { "—" }}, W ${m.kameezWaist.ifBlank { "—" }}, Hip ${m.kameezHip.ifBlank { "—" }}, Slv ${m.sleeveLength.ifBlank { "—" }}, Arm ${m.armhole.ifBlank { "—" }}, Neck ${m.neck.ifBlank { "—" }}, Cuff ${m.cuff.ifBlank { "—" }}, F ${m.frontLength.ifBlank { "—" }}, B ${m.backLength.ifBlank { "—" }}, Col ${m.collar.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                    Text(
                        text = "${AppStrings.shalwarSection.primary(languageMode)}: L ${m.shalwarLength.ifBlank { "—" }}, W ${m.shalwarWaist.ifBlank { "—" }}, Hip ${m.shalwarHip.ifBlank { "—" }}, Th ${m.thigh.ifBlank { "—" }}, Kn ${m.knee.ifBlank { "—" }}, Paicha ${m.bottomPaicha.ifBlank { "—" }} (${m.shalwarStyle})",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPrint,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(AppStrings.printReceipt.primary(languageMode), style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(AppStrings.restoreVersion.primary(languageMode), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
