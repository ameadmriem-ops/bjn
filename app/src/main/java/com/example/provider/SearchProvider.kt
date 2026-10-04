package com.example.provider

import com.example.data.model.SourceInfo
import com.example.nlp.QuestionAnalysis

interface SearchProvider {
    val name: String
    val isEnabled: Boolean
    suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo>
}
