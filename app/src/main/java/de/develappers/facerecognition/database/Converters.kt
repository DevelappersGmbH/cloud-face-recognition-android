package de.develappers.facerecognition.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.develappers.facerecognition.database.model.ServiceResult


class Converters {
    @TypeConverter
    fun fromString(stringListString: String): List<String> {
        return stringListString.split(",").map { it }
    }

    @TypeConverter
    fun toString(stringList: List<String>): String {
        return stringList.joinToString(separator = ",")
    }

    @TypeConverter
    fun toServiceResults(serviceResultsString: String): List<ServiceResult> {
        val gson = Gson()
        val type = object : TypeToken<List<ServiceResult?>?>() {}.type
        return gson.fromJson(serviceResultsString, type)
    }

    @TypeConverter
    fun fromServiceResults(serviceResults: List<ServiceResult>): String {
        val gson = Gson()
        val type = object : TypeToken<List<ServiceResult?>?>() {}.type
        return gson.toJson(serviceResults, type)
    }
}
