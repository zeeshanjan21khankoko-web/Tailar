package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.Customer
import com.example.model.CustomerSortOption
import com.example.model.LanguageMode
import com.example.model.UiState
import com.example.ui.components.CustomerListCard
import com.example.ui.components.CustomerSearchBar

@Composable
fun CustomersListScreen(
    filteredCustomersState: UiState<List<Customer>>,
    searchQuery: String,
    sortOption: CustomerSortOption,
    languageMode: LanguageMode,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (CustomerSortOption) -> Unit,
    onAddNewCustomer: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CustomerSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                languageMode = languageMode,
                testTag = "customers_list_search_input"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(
                    label = if (languageMode == LanguageMode.UR) "تازہ ترین" else "Recent",
                    selected = sortOption == CustomerSortOption.RECENT,
                    onClick = { onSortOptionChange(CustomerSortOption.RECENT) }
                )
                SortChip(
                    label = if (languageMode == LanguageMode.UR) "نام (A-Z)" else "Name A–Z",
                    selected = sortOption == CustomerSortOption.NAME_ASC,
                    onClick = { onSortOptionChange(CustomerSortOption.NAME_ASC) }
                )
                SortChip(
                    label = if (languageMode == LanguageMode.UR) "آئی ڈی" else "ID",
                    selected = sortOption == CustomerSortOption.CUSTOMER_CODE,
                    onClick = { onSortOptionChange(CustomerSortOption.CUSTOMER_CODE) }
                )
            }

            when (filteredCustomersState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = filteredCustomersState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredCustomersState.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) AppStrings.noSearchMatches.inline(languageMode)
                                else AppStrings.emptyCustomersTitle.inline(languageMode),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = filteredCustomersState.data,
                                key = { it.id }
                            ) { customer ->
                                CustomerListCard(
                                    customer = customer,
                                    languageMode = languageMode,
                                    onClick = { onCustomerClick(customer) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNewCustomer,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("customers_fab_add")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = AppStrings.addNewCustomer.primary(languageMode)
            )
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}
