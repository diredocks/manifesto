package com.project.manifesto.common.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    private val mapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        if (attribute.isNullOrEmpty()) return null
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        if (dbData.isNullOrBlank()) return null
        return mapper.readValue(dbData, object : TypeReference<List<String>>() {})
    }
}
