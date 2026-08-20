package com.neytron.sshcommander.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromWorkspaceItemList(value: List<WorkspaceItem>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWorkspaceItemList(value: String?): List<WorkspaceItem>? {
        val listType = object : TypeToken<List<WorkspaceItem>>() {}.type
        return gson.fromJson(value, listType)
    }
}
