package com.example.unisphere.repository

import com.example.unisphere.db.network.CreateCategoryRequest
import com.example.unisphere.db.network.CreateTransactionRequest
import com.example.unisphere.db.network.TransactionCategoryDto
import com.example.unisphere.db.network.TransactionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val client: HttpClient
) {
    suspend fun getCategories(): Result<List<TransactionCategoryDto>> = runCatching {
        client.get("/wallet/categories").body()
    }

    suspend fun saveCategory(request: CreateCategoryRequest): Result<TransactionCategoryDto> = runCatching {
        client.post("/wallet/categories") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteCategory(categoryId: Int): Result<Unit> = runCatching {
        client.delete("/wallet/categories/$categoryId")
        Unit
    }

    suspend fun getTransactions(): Result<List<TransactionDto>> = runCatching {
        client.get("/wallet/transactions").body()
    }

    suspend fun saveTransaction(request: CreateTransactionRequest): Result<TransactionDto> = runCatching {
        client.post("/wallet/transactions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteTransaction(transactionId: Int): Result<Unit> = runCatching {
        client.delete("/wallet/transactions/$transactionId")
        Unit
    }
}