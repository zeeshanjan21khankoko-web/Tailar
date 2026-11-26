package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.Customer
import com.example.model.LanguageMode
import com.example.model.ShopProfile
import com.example.model.UiState
import com.example.ui.components.BilingualText
import com.example.ui.components.CustomerListCard
import com.example.ui.components.CustomerSearchBar
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun DashboardScreen(
    customersState: UiState<List<Customer>>,
    filteredCustomersState: UiState<List<Customer>>,
    shopProfile: ShopProfile?,
    searchQuery: String,
    languageMode: LanguageMode,
    onSearchQueryChange: (String) -> Unit,
    onAddNewCustomer: () -> Unit,
    onViewAllCustomers: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = (customersState as? UiState.Success)?.data?.size ?: 0
    val displayedCustomers = when (filteredCustomersState) {
        is UiState.Success -> {
            if (searchQuery.isBlank()) filteredCustomersState.data.take(6)
            else filteredCustomersState.data
        }
        else -> emptyList()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AtelierStatsHeroCard(
                    shopName = shopProfile?.shopName?.ifBlank { AppStrings.appTitle.primary(languageMode) }
                        ?: AppStrings.appTitle.primary(languageMode),
                    totalCustomers = totalCount,
                    languageMode = languageMode
                )
            }

            item {
                Button(
                    onClick = onAddNewCustomer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("add_new_customer_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.addNewCustomer.inline(languageMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                CustomerSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    languageMode = languageMode,
                    testTag = "dashboard_search_input"
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BilingualText(
                        text = AppStrings.recentCustomers,
                        languageMode = languageMode,
                        primaryStyle = MaterialTheme.typography.titleLarge,
                        secondaryStyle = MaterialTheme.typography.bodySmall
                    )
                    if (totalCount > 0) {
                        TextButton(
                            onClick = onViewAllCustomers,
                            modifier = Modifier.testTag("view_all_customers_button")
                        ) {
                            Text(
                                text = AppStrings.viewAll.primary(languageMode),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            when (filteredCustomersState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = filteredCustomersState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (displayedCustomers.isEmpty()) {
                        item {
                            EmptyTailorStateCard(
                                isSearchActive = searchQuery.isNotBlank(),
                                languageMode = languageMode,
                                onAddNewCustomer = onAddNewCustomer
                            )
                        }
                    } else {
                        items(
                            items = displayedCustomers,
                            key = { it.id }
                        ) { customer ->
                            CustomerListCard(
                                customer = customer,
                                languageMode = languageMode,
                                onClick = { onCustomerClick(customer) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AtelierStatsHeroCard(
    shopName: String,
    totalCustomers: Int,
    languageMode: LanguageMode
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_atelier_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = AppStrings.appSubtitle.inline(languageMode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
                    modifier = Modifier.testTag("total_customers_badge")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = totalCustomers.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = AppStrings.totalCustomers.primary(languageMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTailorStateCard(
    isSearchActive: Boolean,
    languageMode: LanguageMode,
    onAddNewCustomer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Straighten,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(32.dp)
                )
            }
            Text(
                text = if (isSearchActive) AppStrings.noSearchMatches.inline(languageMode)
                else AppStrings.emptyCustomersTitle.inline(languageMode),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (!isSearchActive) {
                Text(
                    text = AppStrings.emptyCustomersBody.primary(languageMode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                AppStrings.emptyCustomersBody.secondary(languageMode)?.let { ur ->
                    Text(
                        text = ur,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAddNewCustomer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(AppStrings.addNewCustomer.primary(languageMode))
                }
            }
        }
    }
}
