package com.example.expensetrackerapp

import java.io.Serializable

data class Expense(
    val id: Long? = null,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long
) : Serializable
