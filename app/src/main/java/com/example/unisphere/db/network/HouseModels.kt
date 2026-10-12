package com.example.unisphere.db.network

import kotlinx.serialization.Serializable

// ==========================================
// CASA E MEMBRI
// ==========================================

@Serializable
data class HouseDto(
    val id: Int,
    val name: String,
    val adminId: Int // Rinominato da adminUid per matchare il backend
)

@Serializable
data class CreateHouseRequest(
    val name: String
)

@Serializable
data class HouseMemberDto(
    val userId: Int, // Rinominato da userUid a userId e trasformato in Int
    val email: String? = null, // Aggiunto dal backend
    val fullName: String? = null, // Aggiunto dal backend (nell'app puoi usarlo come username)
    val username: String? = null, // Mantenuto per compatibilità con la UI
    val profilePictureUri: String? = null
)

// ==========================================
// INVITI
// ==========================================

@Serializable
data class HouseInvitationDto(
    val id: Int, // Ora è Int
    val houseId: Int, // Ora è Int
    val houseName: String,
    val senderName: String, // Rinominato da senderUsername a senderName
    val status: String
)

@Serializable
data class SendInvitationRequest(
    val receiverId: Int // Rinominato e il houseId è stato rimosso (il backend lo prende dall'URL)
)

// ==========================================
// PULIZIE (CHORES)
// ==========================================

@Serializable
data class CreateServiceRequest(
    val name: String // houseId rimosso perché Ktor lo prende dall'URL /{id}/cleaning
)

@Serializable
data class CleaningRotationalStateDto(
    val serviceId: Int, // Ora è Int
    val serviceName: String,
    val assigneeId: Int, // Ora è Int
    val assigneeName: String,
    val isCompleted: Boolean
)

@Serializable
data class ToggleServiceRequest(
    val isCompleted: Boolean
)

// ==========================================
// SPESE CONDIVISE E BILANCI
// ==========================================

@Serializable
data class ExpenseDto(
    val id: Int,
    val houseId: Int,
    val title: String,
    val totalAmount: Double, // Rinominato da amount a totalAmount
    val payerId: Int, // Rinominato da payerUid
    val payerUsername: String? = null,
    val date: String,
    val splits: List<TransactionSplitDto> // Aggiornato al nuovo DTO
)

@Serializable
data class TransactionSplitDto( // Rinominato da ExpenseSplitDto per matchare il backend
    val userId: Int, // Ora è Int
    val amountOwed: Double
)

@Serializable
data class CreateGroupTransactionRequest( // Rinominato da CreateExpenseRequest
    val title: String,
    val totalAmount: Double,
    val payerId: Int, // Il backend lo richiede
    val date: String,
    val splits: List<TransactionSplitDto>
)

@Serializable
data class UserBalanceDto(
    val userId: Int, // Ora è Int
    val username: String,
    val netAmount: Double
)

// ==========================================
// UTENTI (Ricerca)
// ==========================================

@Serializable
data class UserSearchDto(
    val userId: Int, // Ora è Int
    val username: String,
    val profilePictureUri: String? = null
)