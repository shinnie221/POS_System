package com.example.pos_system.data.local.database

import androidx.room.TypeConverter
import com.example.pos_system.data.model.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCartItemList(value: List<CartItem>?): String {
        return if (value == null) "[]" else gson.toJson(value)
    }

    @TypeConverter
    fun toCartItemList(value: String?): List<CartItem> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}