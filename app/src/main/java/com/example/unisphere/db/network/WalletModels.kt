package com.example.unisphere.db.network

import kotlinx.serialization.Serializable

@Serializable
data class TransactionCategoryDto(
    val id: Int,
    val name: String,
    val colorHex: String
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val colorHex: String
)

@Serializable
data class TransactionDto(
    val id: Int,
    val title: String,
    val amount: Double,
    val categoryId: Int,
    val date: String,
    val isIncome: Boolean
)

@Serializable
data class CreateTransactionRequest(
    val title: String,
    val amount: Double,
    val categoryId: Int,
    val date: String,
    val isIncome: Boolean
)