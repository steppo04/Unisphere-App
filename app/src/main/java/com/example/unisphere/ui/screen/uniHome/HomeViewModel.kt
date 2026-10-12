
package com.example.unisphere.ui.screen.uniHome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.*
import com.example.unisphere.repository.HouseRepository
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UniHomeState(
    val isLoading: Boolean = true,
    val hasHouse: Boolean = false,
    val houseId: Int? = null,
    val houseName: String = "",
    val adminId: Int = -1,
    val currentUserId: Int = -1,
    val currentUsername: String = "",
    val currentUserAvatar: String? = null,

    val members: List<HouseMemberDto> = emptyList(),
    val pendingInvitations: List<HouseInvitationDto> = emptyList(),
    val cleaningRotations: List<CleaningRotationalStateDto> = emptyList(),
    val balances: List<UserBalanceDto> = emptyList(),
    val transactions: List<ExpenseDto> = emptyList(),
    val searchedUsers: List<UserSearchDto> = emptyList(),

    val snackbarMessage: String? = null,
    val isSuccessSnackbar: Boolean = false,

    val newHouseName: String = "",
    val inviteUserQuery: String = "",
    val newServiceName: String = "",
    val newExpenseTitle: String = "",
    val newExpenseAmount: String = "",
    val showInviteDialog: Boolean = false,
    val showServiceDialog: Boolean = false,
    val showExpenseDialog: Boolean = false,
    val showSettleDialog: Boolean = false,
    val showAllTxDetailsPage: Boolean = false,
    val txToDeleteId: Int? = null,
    val showDeleteHouseConfirm: Boolean = false,
    val showLeaveHouseConfirm: Boolean = false,

    val selectedMembersForSplit: List<HouseMemberDto> = emptyList(),
    val selectedSettleDebtor: UserBalanceDto? = null,
    val settleAmountText: String = ""
)

sealed interface UniHomeAction {
    data class OnNewHouseNameChanged(val name: String) : UniHomeAction
    data object OnCreateHouseClicked : UniHomeAction
    data object OnDeleteHouseClicked : UniHomeAction

    data class OnInviteQueryChanged(val query: String) : UniHomeAction
    data class OnSelectUserToInvite(val user: UserSearchDto) : UniHomeAction
    data object OnDismissSnackbar : UniHomeAction

    data class OnAcceptInvitation(val invitation: HouseInvitationDto) : UniHomeAction
    data class OnDeclineInvitation(val invitationId: Int) : UniHomeAction
    data class OnRemoveMember(val member: HouseMemberDto) : UniHomeAction
    data object OnLeaveHouseClicked : UniHomeAction

    data class OnNewServiceNameChanged(val name: String) : UniHomeAction
    data object OnAddCleaningServiceClicked : UniHomeAction
    data class OnDeleteServiceClicked(val serviceId: Int) : UniHomeAction
    data class OnToggleServiceCompleted(val serviceId: Int) : UniHomeAction

    data class OnNewExpenseTitleChanged(val title: String) : UniHomeAction
    data class OnNewExpenseAmountChanged(val amount: String) : UniHomeAction
    data class OnToggleMemberSplitSelection(val member: HouseMemberDto) : UniHomeAction
    data object OnAddExpenseClicked : UniHomeAction

    data class OnSettleDebtorSelected(val balance: UserBalanceDto) : UniHomeAction
    data class OnSettleAmountTextChanged(val amount: String) : UniHomeAction
    data object OnSettleDebtClicked : UniHomeAction

    data class OnRequestDeleteTransaction(val txId: Int?) : UniHomeAction
    data object OnConfirmDeleteTransaction : UniHomeAction

    data class OnToggleInviteDialog(val show: Boolean) : UniHomeAction
    data class OnToggleServiceDialog(val show: Boolean) : UniHomeAction
    data class OnToggleExpenseDialog(val show: Boolean) : UniHomeAction
    data class OnToggleSettleDialog(val show: Boolean) : UniHomeAction
    data class OnToggleAllTxDetailsPage(val show: Boolean) : UniHomeAction
    data class OnToggleDeleteHouseConfirm(val show: Boolean) : UniHomeAction
    data class OnToggleLeaveHouseConfirm(val show: Boolean) : UniHomeAction
}

@HiltViewModel
class UniHomeViewModel @Inject constructor(
    application: Application,
    private val repository: HouseRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(UniHomeState())
    val state: StateFlow<UniHomeState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    // --- INIZIALIZZAZIONE ---

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val profile = userRepository.getCurrentUserProfile()

                if (profile == null) {
                    _state.update {
                        it.copy(isLoading = false, hasHouse = false)
                    }
                    showSnackbar(
                        "Errore di autenticazione. Effettua nuovamente il login.",
                        false
                    )
                    return@launch
                }

                _state.update {
                    it.copy(currentUserId = profile.id)
                }

                fetchUserMembership()
                fetchPendingInvitations()
            } catch (error: Exception) {
                error.printStackTrace()
                _state.update { it.copy(isLoading = false) }
                showSnackbar(
                    "Errore durante il caricamento: ${error.message ?: "errore sconosciuto"}",
                    false
                )
            }
        }
    }

    private suspend fun fetchUserMembership() {
        repository.getUserMembership()
            .onSuccess { myHouse ->
                if (myHouse != null) {
                    _state.update {
                        it.copy(
                            hasHouse = true,
                            houseId = myHouse.id,
                            houseName = myHouse.name,
                            adminId = myHouse.adminId,
                            isLoading = false
                        )
                    }
                    refreshHouseData()
                } else {
                    _state.update {
                        it.copy(
                            hasHouse = false,
                            houseId = null,
                            houseName = "",
                            adminId = -1,
                            members = emptyList(),
                            cleaningRotations = emptyList(),
                            balances = emptyList(),
                            transactions = emptyList(),
                            selectedMembersForSplit = emptyList(),
                            selectedSettleDebtor = null,
                            showDeleteHouseConfirm = false,
                            showLeaveHouseConfirm = false,
                            isLoading = false
                        )
                    }
                }
            }
            .onFailure { error ->
                error.printStackTrace()
                _state.update { it.copy(isLoading = false) }
                showSnackbar(
                    "Impossibile caricare la casa: ${error.message ?: "errore di rete"}",
                    false
                )
            }
    }

    private suspend fun fetchPendingInvitations() {
        repository.getPendingInvitations()
            .onSuccess { invites ->
                _state.update { it.copy(pendingInvitations = invites) }
            }
            .onFailure { error ->
                error.printStackTrace()
            }
    }

    private fun refreshHouseData() {
        val houseId = _state.value.houseId ?: return

        viewModelScope.launch {
            try {
                val membersDef = async {
                    repository.getHouseMembers(houseId)
                }
                val choresDef = async {
                    repository.getWeeklyCleaningRotation(houseId)
                }
                val expensesDef = async {
                    repository.getGroupTransactions(houseId)
                }
                val balancesDef = async {
                    repository.getHouseBalances(houseId)
                }

                val membersResult = membersDef.await()
                val choresResult = choresDef.await()
                val expensesResult = expensesDef.await()
                val balancesResult = balancesDef.await()

                val membersList = membersResult.getOrDefault(emptyList())
                val choresList = choresResult.getOrDefault(emptyList())
                val expensesList = expensesResult.getOrDefault(emptyList())
                val balancesList = balancesResult.getOrDefault(emptyList())

                _state.update { currentState ->
                    currentState.copy(
                        members = membersList,
                        cleaningRotations = choresList,
                        transactions = expensesList,
                        balances = balancesList,
                        selectedMembersForSplit =
                            currentState.selectedMembersForSplit.filter { selected ->
                                membersList.any { it.userId == selected.userId }
                            },
                        isLoading = false
                    )
                }

                val errors = listOf(
                    membersResult.exceptionOrNull(),
                    choresResult.exceptionOrNull(),
                    expensesResult.exceptionOrNull(),
                    balancesResult.exceptionOrNull()
                ).filterNotNull()

                if (errors.isNotEmpty()) {
                    errors.forEach { it.printStackTrace() }
                    showSnackbar(
                        "Alcuni dati della casa non sono stati aggiornati.",
                        false
                    )
                }
            } catch (error: Exception) {
                error.printStackTrace()
                _state.update { it.copy(isLoading = false) }
                showSnackbar(
                    "Errore aggiornando i dati: ${error.message ?: "errore sconosciuto"}",
                    false
                )
            }
        }
    }

    private fun areAllBalancesEven(): Boolean {
        val balances = _state.value.balances

        return balances.isNotEmpty() &&
                balances.all { kotlin.math.abs(it.netAmount) < 0.01 }
    }

    private fun displayMemberName(member: HouseMemberDto): String {
        return member.fullName
            ?.takeIf { it.isNotBlank() }
            ?: member.username
                ?.takeIf { it.isNotBlank() }
            ?: member.email
                ?.takeIf { it.isNotBlank() }
            ?: "Coinquilino"
    }

    private fun showSnackbar(
        message: String,
        isSuccess: Boolean = true
    ) {
        _state.update {
            it.copy(
                snackbarMessage = message,
                isSuccessSnackbar = isSuccess
            )
        }
    }

    // --- AZIONI ---

    fun onAction(action: UniHomeAction) {
        val currentState = _state.value

        when (action) {
            is UniHomeAction.OnNewHouseNameChanged -> {
                _state.update { it.copy(newHouseName = action.name) }
            }

            UniHomeAction.OnCreateHouseClicked -> {
                val name = currentState.newHouseName.trim()
                if (name.isBlank()) return

                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }

                    repository.createHouse(name)
                        .onSuccess { newHouse ->
                            _state.update {
                                it.copy(
                                    newHouseName = "",
                                    hasHouse = true,
                                    houseId = newHouse.id,
                                    houseName = newHouse.name,
                                    adminId = newHouse.adminId,
                                    isLoading = false
                                )
                            }
                            showSnackbar("Casa creata con successo!")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            _state.update { it.copy(isLoading = false) }
                            showSnackbar(
                                "Errore nella creazione: ${error.message ?: "errore sconosciuto"}",
                                false
                            )
                        }
                }
            }

            UniHomeAction.OnDeleteHouseClicked -> {
                val houseId = currentState.houseId ?: return

                if (currentState.currentUserId != currentState.adminId) {
                    showSnackbar(
                        "Solo l'amministratore può eliminare la casa.",
                        false
                    )
                    return
                }

                viewModelScope.launch {
                    if (!areAllBalancesEven()) {
                        showSnackbar(
                            "Impossibile eliminare la casa: ci sono conti in sospeso!",
                            false
                        )
                        _state.update {
                            it.copy(showDeleteHouseConfirm = false)
                        }
                        return@launch
                    }

                    repository.deleteHouse(houseId)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    showDeleteHouseConfirm = false,
                                    isLoading = true
                                )
                            }

                            showSnackbar("Casa eliminata definitivamente.")
                            fetchUserMembership()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            _state.update {
                                it.copy(showDeleteHouseConfirm = false)
                            }
                            showSnackbar(
                                "Errore eliminando la casa: ${error.message ?: "errore sconosciuto"}",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnInviteQueryChanged -> {
                _state.update { it.copy(inviteUserQuery = action.query) }

                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(300)

                    if (action.query.length >= 2) {
                        repository.searchUsers(action.query)
                            .onSuccess { results ->
                                val filtered = results.filter {
                                    it.userId != _state.value.currentUserId
                                }
                                _state.update {
                                    it.copy(searchedUsers = filtered)
                                }
                            }
                            .onFailure { error ->
                                error.printStackTrace()
                                showSnackbar(
                                    "Errore nella ricerca utenti.",
                                    false
                                )
                            }
                    } else {
                        _state.update {
                            it.copy(searchedUsers = emptyList())
                        }
                    }
                }
            }

            is UniHomeAction.OnSelectUserToInvite -> {
                val houseId = currentState.houseId ?: return

                viewModelScope.launch {
                    repository.sendInvitation(
                        houseId,
                        action.user.userId
                    )
                        .onSuccess {
                            showSnackbar(
                                "Invito inviato a ${action.user.username}!"
                            )
                            _state.update {
                                it.copy(
                                    inviteUserQuery = "",
                                    searchedUsers = emptyList(),
                                    showInviteDialog = false
                                )
                            }
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore nell'invio dell'invito.",
                                false
                            )
                        }
                }
            }

            UniHomeAction.OnDismissSnackbar -> {
                _state.update { it.copy(snackbarMessage = null) }
            }

            is UniHomeAction.OnAcceptInvitation -> {
                viewModelScope.launch {
                    repository.acceptInvitation(action.invitation.id)
                        .onSuccess {
                            showSnackbar("Invito accettato!")
                            fetchUserMembership()
                            fetchPendingInvitations()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar("Impossibile accettare l'invito.", false)
                        }
                }
            }

            is UniHomeAction.OnDeclineInvitation -> {
                viewModelScope.launch {
                    repository.declineInvitation(action.invitationId)
                        .onSuccess {
                            showSnackbar("Invito rifiutato.")
                            fetchPendingInvitations()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar("Impossibile rifiutare l'invito.", false)
                        }
                }
            }

            is UniHomeAction.OnRemoveMember -> {
                val houseId = currentState.houseId ?: return
                val member = action.member

                if (member.userId == currentState.currentUserId) {
                    showSnackbar(
                        "Per uscire dalla casa usa il comando Abbandona.",
                        false
                    )
                    return
                }

                if (member.userId == currentState.adminId) {
                    showSnackbar(
                        "Non puoi rimuovere l'amministratore della casa.",
                        false
                    )
                    return
                }

                val memberBalance = currentState.balances
                    .find { it.userId == member.userId }
                    ?.netAmount ?: 0.0

                val memberName = displayMemberName(member)

                viewModelScope.launch {
                    if (kotlin.math.abs(memberBalance) >= 0.01) {
                        showSnackbar(
                            "Impossibile rimuovere $memberName: conti non in pari!",
                            false
                        )
                        return@launch
                    }

                    repository.removeMemberFromHouse(houseId, member.userId)
                        .onSuccess {
                            showSnackbar("$memberName è stato rimosso.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore durante la rimozione di $memberName.",
                                false
                            )
                        }
                }
            }

            UniHomeAction.OnLeaveHouseClicked -> {
                val houseId = currentState.houseId ?: return

                if (currentState.currentUserId == currentState.adminId) {
                    showSnackbar(
                        "L'amministratore deve eliminare la casa o trasferire il ruolo prima di uscire.",
                        false
                    )
                    _state.update {
                        it.copy(showLeaveHouseConfirm = false)
                    }
                    return
                }

                val myBalance = currentState.balances
                    .find { it.userId == currentState.currentUserId }
                    ?.netAmount ?: 0.0

                viewModelScope.launch {
                    if (kotlin.math.abs(myBalance) >= 0.01) {
                        showSnackbar(
                            "Non puoi uscire se il tuo bilancio non è in pari!",
                            false
                        )
                        _state.update {
                            it.copy(showLeaveHouseConfirm = false)
                        }
                        return@launch
                    }

                    repository.removeMemberFromHouse(
                        houseId,
                        currentState.currentUserId
                    )
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    showLeaveHouseConfirm = false,
                                    isLoading = true
                                )
                            }
                            showSnackbar("Sei uscito dalla casa.")
                            fetchUserMembership()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            _state.update {
                                it.copy(showLeaveHouseConfirm = false)
                            }
                            showSnackbar(
                                "Errore durante l'uscita dalla casa.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnNewServiceNameChanged -> {
                _state.update { it.copy(newServiceName = action.name) }
            }

            UniHomeAction.OnAddCleaningServiceClicked -> {
                val houseId = currentState.houseId ?: return
                val name = currentState.newServiceName.trim()
                if (name.isBlank()) return

                viewModelScope.launch {
                    repository.addCleaningService(houseId, name)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    newServiceName = "",
                                    showServiceDialog = false
                                )
                            }
                            showSnackbar("Servizio di pulizia aggiunto.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore aggiungendo il servizio.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnDeleteServiceClicked -> {
                viewModelScope.launch {
                    repository.deleteCleaningService(action.serviceId)
                        .onSuccess {
                            showSnackbar("Servizio eliminato.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore durante l'eliminazione del servizio.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnToggleServiceCompleted -> {
                val currentService = currentState.cleaningRotations
                    .find { it.serviceId == action.serviceId }
                    ?: return

                viewModelScope.launch {
                    repository.toggleCleaningServiceCompletion(
                        action.serviceId,
                        !currentService.isCompleted
                    )
                        .onSuccess {
                            showSnackbar("Stato pulizia aggiornato.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore aggiornando lo stato della pulizia.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnNewExpenseTitleChanged -> {
                _state.update { it.copy(newExpenseTitle = action.title) }
            }

            is UniHomeAction.OnNewExpenseAmountChanged -> {
                _state.update { it.copy(newExpenseAmount = action.amount) }
            }

            is UniHomeAction.OnToggleMemberSplitSelection -> {
                val selected = currentState.selectedMembersForSplit.toMutableList()

                if (selected.any { it.userId == action.member.userId }) {
                    selected.removeAll { it.userId == action.member.userId }
                } else {
                    selected.add(action.member)
                }

                _state.update {
                    it.copy(selectedMembersForSplit = selected)
                }
            }

            UniHomeAction.OnAddExpenseClicked -> {
                val houseId = currentState.houseId ?: return
                val title = currentState.newExpenseTitle.trim()
                val totalAmount = currentState.newExpenseAmount
                    .replace(',', '.')
                    .toDoubleOrNull() ?: 0.0
                val selectedMembers = currentState.selectedMembersForSplit

                if (title.isBlank() || totalAmount <= 0.0 || selectedMembers.isEmpty()) {
                    showSnackbar(
                        "Inserisci descrizione, importo valido e almeno un partecipante.",
                        false
                    )
                    return
                }

                viewModelScope.launch {
                    val dividedAmount = totalAmount / selectedMembers.size

                    val splits = selectedMembers.map { member ->
                        TransactionSplitDto(
                            userId = member.userId,
                            amountOwed = dividedAmount
                        )
                    }

                    repository.addGroupExpense(
                        houseId = houseId,
                        title = title,
                        totalAmount = totalAmount,
                        payerId = currentState.currentUserId,
                        date = LocalDate.now().toString(),
                        splits = splits
                    )
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    newExpenseTitle = "",
                                    newExpenseAmount = "",
                                    showExpenseDialog = false
                                )
                            }
                            showSnackbar("Spesa aggiunta correttamente.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore salvando la spesa: ${error.message ?: "errore sconosciuto"}",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnSettleDebtorSelected -> {
                _state.update {
                    it.copy(selectedSettleDebtor = action.balance)
                }
            }

            is UniHomeAction.OnSettleAmountTextChanged -> {
                _state.update { it.copy(settleAmountText = action.amount) }
            }

            UniHomeAction.OnSettleDebtClicked -> {
                val houseId = currentState.houseId ?: return
                val target = currentState.selectedSettleDebtor ?: return
                val amount = currentState.settleAmountText
                    .replace(',', '.')
                    .toDoubleOrNull() ?: 0.0

                if (amount <= 0.0) {
                    showSnackbar("Inserisci un importo valido.", false)
                    return
                }

                if (target.userId == currentState.currentUserId) {
                    showSnackbar("Non puoi saldare un conto verso te stesso.", false)
                    return
                }

                viewModelScope.launch {
                    val splits = listOf(
                        TransactionSplitDto(
                            userId = target.userId,
                            amountOwed = amount
                        )
                    )

                    repository.addGroupExpense(
                        houseId = houseId,
                        title = "Saldatura: ${currentState.currentUsername} ➔ ${target.username}",
                        totalAmount = amount,
                        payerId = currentState.currentUserId,
                        date = LocalDate.now().toString(),
                        splits = splits
                    )
                        .onSuccess {
                            showSnackbar("Debito saldato correttamente!")
                            _state.update {
                                it.copy(
                                    showSettleDialog = false,
                                    selectedSettleDebtor = null,
                                    settleAmountText = ""
                                )
                            }
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore durante la registrazione del saldo.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnRequestDeleteTransaction -> {
                _state.update { it.copy(txToDeleteId = action.txId) }
            }

            UniHomeAction.OnConfirmDeleteTransaction -> {
                val txId = currentState.txToDeleteId ?: return

                viewModelScope.launch {
                    repository.deleteTransaction(txId)
                        .onSuccess {
                            _state.update { it.copy(txToDeleteId = null) }
                            showSnackbar("Spesa eliminata, conti ricalcolati.")
                            refreshHouseData()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            showSnackbar(
                                "Errore durante l'eliminazione della spesa.",
                                false
                            )
                        }
                }
            }

            is UniHomeAction.OnToggleInviteDialog -> {
                _state.update {
                    it.copy(
                        showInviteDialog = action.show,
                        inviteUserQuery = "",
                        searchedUsers = emptyList()
                    )
                }
            }

            is UniHomeAction.OnToggleServiceDialog -> {
                _state.update {
                    it.copy(
                        showServiceDialog = action.show,
                        newServiceName = ""
                    )
                }
            }

            is UniHomeAction.OnToggleExpenseDialog -> {
                _state.update {
                    it.copy(
                        showExpenseDialog = action.show,
                        newExpenseTitle = "",
                        newExpenseAmount = "",
                        selectedMembersForSplit = it.members
                    )
                }
            }

            is UniHomeAction.OnToggleSettleDialog -> {
                _state.update {
                    it.copy(
                        showSettleDialog = action.show,
                        selectedSettleDebtor = null,
                        settleAmountText = ""
                    )
                }
            }

            is UniHomeAction.OnToggleAllTxDetailsPage -> {
                _state.update {
                    it.copy(showAllTxDetailsPage = action.show)
                }
            }

            is UniHomeAction.OnToggleDeleteHouseConfirm -> {
                _state.update {
                    it.copy(showDeleteHouseConfirm = action.show)
                }
            }

            is UniHomeAction.OnToggleLeaveHouseConfirm -> {
                _state.update {
                    it.copy(showLeaveHouseConfirm = action.show)
                }
            }
        }
    }
}
