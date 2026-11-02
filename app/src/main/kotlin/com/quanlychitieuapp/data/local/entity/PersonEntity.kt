package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quanlychitieuapp.domain.model.Person

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val createdAt: Long
)

fun PersonEntity.toDomain() = Person(id, name, normalizedName, createdAt)
fun Person.toEntity() = PersonEntity(id, name, normalizedName, createdAt)
