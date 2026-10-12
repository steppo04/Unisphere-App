package com.example.unisphere.ui.screen.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.CreateCategoryRequest
import com.example.unisphere.db.network.CreateTransactionRequest
import com.example.unisphere.db.network.TransactionCategoryDto
import com.example.unisphere.db.network.TransactionDto
import com.example.unisphere.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ChartPoint(val date: LocalDate, val balance: Double, val hasTransaction: Boolean)
data class PieSlice(val categoryName: String, val colorHex: String, val totalAmount: Double, val sweepAngle: Float)

data class WalletState(
    // Entity sostituite dai DTO
    val transactions: List<TransactionDto> = emptyList(),
    val filteredTransactions: List<TransactionDto> = emptyList(),
    val categories: List<TransactionCategoryDto> = emptyList(),
    val selectedTransaction: TransactionDto? = null,
    val showAddDialog: Boolean = false,

    val totalIncomes: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val pieSlices: List<PieSlice> = emptyList(),
    val lineChartPoints: List<ChartPoint> = emptyList(),
    val maxTimelineBalance: Double = 100.0,
    val minTimelineBalance: Double = 0.0,
    val timelineRange: Double = 100.0,
    val selectedChartIndex: Int? = null,

    // Filtri avanzati (ID numerici come nel backend)
    val filterCategoryId: Int? = null,
    val filterIsIncome: Boolean? = null,
    val filterMinAmount: String = "",
    val filterMaxAmount: String = "",
    val isFilterPanelExpanded: Boolean = false,
    val isFilterCategoryDropdownExpanded: Boolean = false,
    val showAllTransactions: Boolean = false,

    val newTransactionTitle: String = "",
    val newTransactionAmount: String = "",
    val newTransactionCategoryId: Int = 0,
    val newTransactionDate: LocalDate = LocalDate.now(),
    val newTransactionIsIncome: Boolean = false,
    val showDatePicker: Boolean = false,
    val showCategoryCreationDialog: Boolean = false,
    val categoryToDelete: TransactionCategoryDto? = null,
    val isCategoryDropdownExpanded: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryColorHex: String = "#34C759",

    val isDetailEditingActive: Boolean = false,
    val detailTitleText: String = "",
    val detailAmountText: String = "",
    val detailCategoryId: Int = 0,
    val detailDateValue: LocalDate = LocalDate.now(),
    val detailIsIncomeValue: Boolean = false,
    val showDetailDatePicker: Boolean = false,
    val isDetailCategoryDropdownExpanded: Boolean = false,

    val isLoading: Boolean = true
)

sealed interface WalletAction {
    data class OnTitleChanged(val value: String) : WalletAction
    data class OnAmountChanged(val value: String) : WalletAction
    data class OnCategoryChanged(val categoryId: Int) : WalletAction
    data class OnDateChanged(val value: LocalDate) : WalletAction
    data class OnTypeChanged(val isIncome: Boolean) : WalletAction
    data object OnAddClicked : WalletAction
    data object OnDismissAddDialog : WalletAction
    data object OnSaveTransactionClicked : WalletAction

    data class OnTransactionSelected(val transaction: TransactionDto?) : WalletAction
    data object OnToggleDetailEditing : WalletAction
    data class OnDetailTitleChanged(val value: String) : WalletAction
    data class OnDetailAmountChanged(val value: String) : WalletAction
    data class OnDetailCategoryChanged(val categoryId: Int) : WalletAction
    data class OnDetailTypeChanged(val isIncome: Boolean) : WalletAction
    data class OnDetailDateChanged(val value: LocalDate) : WalletAction
    data object OnSaveUpdateTransactionClicked : WalletAction
    data object OnDeleteTransactionClicked : WalletAction

    data class OnCreateCategoryType(val name: String, val colorHex: String) : WalletAction
    data class OnRequestDeleteCategory(val category: TransactionCategoryDto?) : WalletAction
    data object OnConfirmDeleteCategory : WalletAction
    data class OnNewCategoryNameChanged(val value: String) : WalletAction
    data class OnNewCategoryColorChanged(val hex: String) : WalletAction

    data object ToggleDatePicker : WalletAction
    data object ToggleDetailDatePicker : WalletAction
    data class ToggleCategoryDropdown(val expanded: Boolean) : WalletAction
    data class ToggleDetailCategoryDropdown(val expanded: Boolean) : WalletAction
    data class ToggleCategoryCreationDialog(val show: Boolean) : WalletAction
    data class OnChartPointSelected(val index: Int?) : WalletAction

    // Filtri
    data class OnFilterCategoryChanged(val categoryId: Int?) : WalletAction
    data class OnFilterTypeChanged(val isIncome: Boolean?) : WalletAction
    data class OnFilterMinAmountChanged(val value: String) : WalletAction
    data class OnFilterMaxAmountChanged(val value: String) : WalletAction
    data object ToggleFilterPanel : WalletAction
    data class ToggleFilterCategoryDropdown(val expanded: Boolean) : WalletAction
    data object ToggleShowAllTransactions : WalletAction
    data object OnClearFilters : WalletAction
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    application: Application,
    private val walletRepository: WalletRepository
) : AndroidViewModel(application) {

    // Passaggio a StateFlow
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    private var rawTransactions: List<TransactionDto> = emptyList()

    init {
        fetchWalletData()
    }

    private fun fetchWalletData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Chiamate di rete parallele
            val categoriesDef = async { walletRepository.getCategories() }
            val transactionsDef = async { walletRepository.getTransactions() }

            val cats = categoriesDef.await().getOrDefault(emptyList())
            rawTransactions = transactionsDef.await().getOrDefault(emptyList())

            var selectedCatId = _state.value.newTransactionCategoryId
            if (selectedCatId <= 0 || cats.none { it.id == selectedCatId }) {
                selectedCatId = cats.firstOrNull()?.id ?: 0
            }

            _state.update { it.copy(categories = cats, newTransactionCategoryId = selectedCatId, isLoading = false) }
            generateCalculatedData()
        }
    }

    fun onAction(action: WalletAction) {
        val currentState = _state.value
        when (action) {
            is WalletAction.OnTitleChanged -> _state.update { it.copy(newTransactionTitle = action.value) }
            is WalletAction.OnAmountChanged -> _state.update { it.copy(newTransactionAmount = action.value) }
            is WalletAction.OnCategoryChanged -> _state.update { it.copy(newTransactionCategoryId = action.categoryId) }
            is WalletAction.OnDateChanged -> _state.update { it.copy(newTransactionDate = action.value, showDatePicker = false) }
            is WalletAction.OnTypeChanged -> _state.update { it.copy(newTransactionIsIncome = action.isIncome) }

            WalletAction.OnAddClicked -> {
                _state.update {
                    it.copy(
                        showAddDialog = true,
                        newTransactionTitle = "",
                        newTransactionAmount = "",
                        newTransactionCategoryId = it.categories.firstOrNull()?.id ?: 0,
                        newTransactionDate = LocalDate.now(),
                        newTransactionIsIncome = false,
                        isCategoryDropdownExpanded = false
                    )
                }
            }
            WalletAction.OnDismissAddDialog -> _state.update { it.copy(showAddDialog = false) }
            WalletAction.OnSaveTransactionClicked -> saveTransaction()

            is WalletAction.OnTransactionSelected -> {
                val tx = action.transaction
                if (tx != null) {
                    _state.update {
                        it.copy(
                            selectedTransaction = tx,
                            isDetailEditingActive = false,
                            detailTitleText = tx.title,
                            detailAmountText = tx.amount.toString(),
                            detailCategoryId = tx.categoryId,
                            detailDateValue = LocalDate.parse(tx.date),
                            detailIsIncomeValue = tx.isIncome
                        )
                    }
                } else {
                    _state.update { it.copy(selectedTransaction = null) }
                }
            }
            WalletAction.OnToggleDetailEditing -> _state.update { it.copy(isDetailEditingActive = !it.isDetailEditingActive) }
            is WalletAction.OnDetailTitleChanged -> _state.update { it.copy(detailTitleText = action.value) }
            is WalletAction.OnDetailAmountChanged -> _state.update { it.copy(detailAmountText = action.value) }
            is WalletAction.OnDetailCategoryChanged -> _state.update { it.copy(detailCategoryId = action.categoryId) }
            is WalletAction.OnDetailTypeChanged -> _state.update { it.copy(detailIsIncomeValue = action.isIncome) }
            is WalletAction.OnDetailDateChanged -> _state.update { it.copy(detailDateValue = action.value, showDetailDatePicker = false) }

            WalletAction.OnSaveUpdateTransactionClicked -> {
                val currentTx = currentState.selectedTransaction ?: return
                val parsedAmount = currentState.detailAmountText.toDoubleOrNull() ?: 0.0
                if (currentState.detailTitleText.isNotBlank() && parsedAmount > 0) {
                    viewModelScope.launch {
                        // Per l'update, prima eliminiamo il vecchio e poi salviamo il nuovo (oppure usi una rotta PUT dedicata se l'hai fatta)
                        walletRepository.deleteTransaction(currentTx.id)
                        walletRepository.saveTransaction(
                            CreateTransactionRequest(
                                title = currentState.detailTitleText,
                                amount = parsedAmount,
                                categoryId = currentState.detailCategoryId,
                                date = currentState.detailDateValue.toString(), // Convertito in String ISO
                                isIncome = currentState.detailIsIncomeValue
                            )
                        ).onSuccess {
                            _state.update { it.copy(selectedTransaction = null) }
                            fetchWalletData()
                        }
                    }
                }
            }
            WalletAction.OnDeleteTransactionClicked -> {
                currentState.selectedTransaction?.let { trans ->
                    viewModelScope.launch {
                        walletRepository.deleteTransaction(trans.id).onSuccess {
                            _state.update { it.copy(selectedTransaction = null) }
                            fetchWalletData()
                        }
                    }
                }
            }
            is WalletAction.OnCreateCategoryType -> {
                _state.update { it.copy(showCategoryCreationDialog = false) }
                viewModelScope.launch {
                    walletRepository.saveCategory(
                        CreateCategoryRequest(name = action.name, colorHex = action.colorHex)
                    ).onSuccess { createdCategory ->
                        _state.update {
                            it.copy(
                                categories = (it.categories.filterNot { category -> category.id == createdCategory.id } + createdCategory),
                                newTransactionCategoryId = createdCategory.id,
                                newCategoryName = ""
                            )
                        }
                        fetchWalletData()
                    }
                }
            }
            is WalletAction.OnRequestDeleteCategory -> _state.update { it.copy(categoryToDelete = action.category) }

            WalletAction.OnConfirmDeleteCategory -> {
                val catToDelete = currentState.categoryToDelete ?: return
                viewModelScope.launch {
                    walletRepository.deleteCategory(catToDelete.id).onSuccess {
                        _state.update { it.copy(categoryToDelete = null) }
                        fetchWalletData()
                    }
                }
            }
            is WalletAction.OnNewCategoryNameChanged -> _state.update { it.copy(newCategoryName = action.value) }
            is WalletAction.OnNewCategoryColorChanged -> _state.update { it.copy(newCategoryColorHex = action.hex) }

            WalletAction.ToggleDatePicker -> _state.update { it.copy(showDatePicker = !it.showDatePicker) }
            WalletAction.ToggleDetailDatePicker -> _state.update { it.copy(showDetailDatePicker = !it.showDetailDatePicker) }
            is WalletAction.ToggleCategoryDropdown -> _state.update { it.copy(isCategoryDropdownExpanded = action.expanded) }
            is WalletAction.ToggleDetailCategoryDropdown -> _state.update { it.copy(isDetailCategoryDropdownExpanded = action.expanded) }
            is WalletAction.ToggleCategoryCreationDialog -> {
                _state.update {
                    it.copy(
                        showCategoryCreationDialog = action.show,
                        newCategoryName = "",
                        newCategoryColorHex = "#34C759"
                    )
                }
            }
            is WalletAction.OnChartPointSelected -> _state.update { it.copy(selectedChartIndex = action.index) }

            // FILTRI
            is WalletAction.OnFilterCategoryChanged -> {
                _state.update {
                    it.copy(
                        filterCategoryId = action.categoryId,
                        isFilterCategoryDropdownExpanded = false
                    )
                }
                generateCalculatedData()
            }
            is WalletAction.OnFilterTypeChanged -> {
                _state.update { it.copy(filterIsIncome = action.isIncome) }
                generateCalculatedData()
            }
            is WalletAction.OnFilterMinAmountChanged -> {
                _state.update { it.copy(filterMinAmount = action.value) }
                generateCalculatedData()
            }
            is WalletAction.OnFilterMaxAmountChanged -> {
                _state.update { it.copy(filterMaxAmount = action.value) }
                generateCalculatedData()
            }
            WalletAction.ToggleFilterPanel -> _state.update {
                it.copy(
                    isFilterPanelExpanded = !it.isFilterPanelExpanded,
                    isFilterCategoryDropdownExpanded = false
                )
            }
            is WalletAction.ToggleFilterCategoryDropdown -> _state.update { it.copy(isFilterCategoryDropdownExpanded = action.expanded) }
            WalletAction.ToggleShowAllTransactions -> _state.update { it.copy(showAllTransactions = !it.showAllTransactions) }
            WalletAction.OnClearFilters -> {
                _state.update {
                    it.copy(
                        filterCategoryId = null,
                        filterIsIncome = null,
                        filterMinAmount = "",
                        filterMaxAmount = "",
                        isFilterCategoryDropdownExpanded = false
                    )
                }
                generateCalculatedData()
            }
        }
    }

    private fun generateCalculatedData() {
        val currentState = _state.value

        val totalInc = rawTransactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExp = rawTransactions.filter { !it.isIncome }.sumOf { it.amount }

        val filteredList = rawTransactions.filter { trans ->
            val matchesCategory = currentState.filterCategoryId == null || trans.categoryId == currentState.filterCategoryId
            val matchesType = currentState.filterIsIncome == null || trans.isIncome == currentState.filterIsIncome
            val minAmt = currentState.filterMinAmount.toDoubleOrNull()
            val matchesMin = minAmt == null || trans.amount >= minAmt
            val maxAmt = currentState.filterMaxAmount.toDoubleOrNull()
            val matchesMax = maxAmt == null || trans.amount <= maxAmt
            matchesCategory && matchesType && matchesMin && matchesMax
        }

        val expensesOnly = rawTransactions.filter { !it.isIncome }
        val totalExpensesAmount = expensesOnly.sumOf { it.amount }
        val pieGroup = expensesOnly.groupBy { it.categoryId }.map { (catId, list) ->
            val category = currentState.categories.find { it.id == catId }
            val amount = list.sumOf { it.amount }
            val sweep = if (totalExpensesAmount > 0) (amount.toFloat() / totalExpensesAmount.toFloat()) * 360f else 0f
            PieSlice(category?.name ?: "Altro", category?.colorHex ?: "#8E8E93", amount, sweep)
        }

        // Conversione String in LocalDate per l'ordinamento e la logica del grafico
        val sortedTx = rawTransactions.sortedBy { LocalDate.parse(it.date) }
        val timelinePoints = mutableListOf<ChartPoint>()
        var rollingBalance = 0.0
        var cursorDate = LocalDate.now().minusDays(30)
        val today = LocalDate.now()

        while (!cursorDate.isAfter(today)) {
            val daysTransactions = sortedTx.filter { LocalDate.parse(it.date) == cursorDate }
            val hasChange = daysTransactions.isNotEmpty()
            if (hasChange) {
                rollingBalance += daysTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
            }
            timelinePoints.add(ChartPoint(cursorDate, rollingBalance, hasChange))
            cursorDate = cursorDate.plusDays(1)
        }

        val maxBal = timelinePoints.maxOfOrNull { it.balance } ?: 100.0
        val minBal = timelinePoints.minOfOrNull { it.balance } ?: 0.0
        val rangeBal = (maxBal - minBal).coerceAtLeast(100.0)

        _state.update {
            it.copy(
                transactions = rawTransactions,
                filteredTransactions = filteredList,
                totalIncomes = totalInc,
                totalExpenses = totalExp,
                netBalance = totalInc - totalExp,
                pieSlices = pieGroup,
                lineChartPoints = timelinePoints,
                maxTimelineBalance = maxBal,
                minTimelineBalance = minBal,
                timelineRange = rangeBal
            )
        }
    }

    private fun saveTransaction() {
        val currentState = _state.value
        val amountDouble = currentState.newTransactionAmount.toDoubleOrNull() ?: 0.0
        if (currentState.newTransactionTitle.isNotBlank() && amountDouble > 0 && currentState.newTransactionCategoryId > 0) {
            viewModelScope.launch {
                walletRepository.saveTransaction(
                    CreateTransactionRequest(
                        title = currentState.newTransactionTitle,
                        amount = amountDouble,
                        categoryId = currentState.newTransactionCategoryId,
                        date = currentState.newTransactionDate.toString(), // Convertito in String ISO
                        isIncome = currentState.newTransactionIsIncome
                    )
                ).onSuccess {
                    _state.update { it.copy(showAddDialog = false) }
                    fetchWalletData() // Ricarica tutto dal server dopo l'inserimento
                }
            }
        }
    }
}