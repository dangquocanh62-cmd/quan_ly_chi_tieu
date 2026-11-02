package com.quanlychitieuapp.domain.model

data class Person(
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val createdAt: Long
)
