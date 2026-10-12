package com.example.unisphere.repository

import com.example.unisphere.db.network.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(
    private val client: HttpClient
) {
    // ==========================================
    // CASA E MEMBRI
    // ==========================================

    suspend fun createHouse(name: String): Result<HouseDto> = runCatching {
        client.post("/house") {
            contentType(ContentType.Application.Json)
            setBody(CreateHouseRequest(name = name))
        }.body()
    }

    suspend fun getHouseById(houseId: Int): Result<HouseDto> = runCatching {
        client.get("/house/$houseId").body()
    }

    suspend fun deleteHouse(houseId: Int): Result<Unit> = runCatching {
        client.delete("/house/$houseId")
    }

    suspend fun getUserMembership(): Result<HouseDto?> = runCatching {
        // Ora restituisce HouseDto (i dati della casa), non più il membro!
        val response = client.get("/house/my")
        if (response.status == HttpStatusCode.NotFound) null else response.body()
    }

    suspend fun getHouseMembers(houseId: Int): Result<List<HouseMemberDto>> = runCatching {
        client.get("/house/$houseId/members").body()
    }

    suspend fun removeMemberFromHouse(houseId: Int, userId: Int): Result<Unit> = runCatching {
        client.delete("/house/$houseId/members/$userId")
    }

    // ==========================================
    // INVITI
    // ==========================================

    suspend fun sendInvitation(houseId: Int, receiverId: Int): Result<Unit> = runCatching {
        client.post("/house/$houseId/invite") {
            contentType(ContentType.Application.Json)
            setBody(SendInvitationRequest(receiverId = receiverId))
        }
    }

    suspend fun getPendingInvitations(): Result<List<HouseInvitationDto>> = runCatching {
        client.get("/house/invitations").body()
    }

    suspend fun acceptInvitation(invitationId: Int): Result<Unit> = runCatching {
        client.post("/house/invitations/$invitationId/respond") {
            parameter("accept", true)
        }
    }

    suspend fun declineInvitation(invitationId: Int): Result<Unit> = runCatching {
        client.post("/house/invitations/$invitationId/respond") {
            parameter("accept", false)
        }
    }

    // ==========================================
    // ROTAZIONE AUTOMATICA PULIZIE
    // ==========================================

    suspend fun addCleaningService(houseId: Int, name: String): Result<Unit> = runCatching {
        client.post("/house/$houseId/cleaning") {
            contentType(ContentType.Application.Json)
            setBody(CreateServiceRequest(name = name))
        }
    }

    suspend fun deleteCleaningService(serviceId: Int): Result<Unit> = runCatching {
        client.delete("/cleaning-services/$serviceId")
    }

    suspend fun toggleCleaningServiceCompletion(serviceId: Int, isCompleted: Boolean): Result<Unit> = runCatching {
        client.patch("/cleaning-services/$serviceId/status") {
            contentType(ContentType.Application.Json)
            setBody(ToggleServiceRequest(isCompleted = isCompleted))
        }
    }

    suspend fun getWeeklyCleaningRotation(houseId: Int): Result<List<CleaningRotationalStateDto>> = runCatching {
        client.get("/house/$houseId/cleaning/rotation").body()
    }

    // ==========================================
    // SPESE E BILANCI (SPLITWISE)
    // ==========================================

    suspend fun addGroupExpense(
        houseId: Int,
        title: String,
        totalAmount: Double,
        payerId: Int,
        date: String,
        splits: List<TransactionSplitDto>
    ): Result<Unit> = runCatching {
        client.post("/house/$houseId/expenses") {
            contentType(ContentType.Application.Json)
            setBody(CreateGroupTransactionRequest(title, totalAmount, payerId, date, splits))
        }
    }

    suspend fun getGroupTransactions(houseId: Int): Result<List<ExpenseDto>> = runCatching {
        client.get("/house/$houseId/expenses").body()
    }

    suspend fun deleteTransaction(transactionId: Int): Result<Unit> = runCatching {
        client.delete("/expenses/$transactionId")
    }

    suspend fun updateTransaction(
        transactionId: Int,
        houseId: Int,
        title: String,
        totalAmount: Double,
        payerId: Int,
        date: String,
        splits: List<TransactionSplitDto>
    ): Result<Unit> = runCatching {
        client.put("/expenses/$transactionId") {
            contentType(ContentType.Application.Json)
            setBody(CreateGroupTransactionRequest(title, totalAmount, payerId, date, splits))
        }
    }

    suspend fun getHouseBalances(houseId: Int): Result<List<UserBalanceDto>> = runCatching {
        client.get("/house/$houseId/balances").body()
    }

    // ==========================================
    // RICERCA UTENTI
    // ==========================================

    suspend fun searchUsers(query: String): Result<List<UserSearchDto>> = runCatching {
        client.get("/users/search") {
            parameter("query", query)
        }.body()
    }
}