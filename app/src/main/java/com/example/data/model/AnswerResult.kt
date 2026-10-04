package com.example.data.model

data class SourceInfo(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String,
    val fullText: String = "",
    val trustTier: TrustTier,
    val sourceName: String,
    val providerName: String,
    val extractedFact: String = "",
    val isVerified: Boolean = trustTier.priorityRank <= 2
)

data class CandidateSentence(
    val text: String,
    val cleanSentence: String,
    val score: Double,
    val sourceTitle: String,
    val sourceUrl: String,
    val domain: String,
    val trustTier: TrustTier,
    val matchedKeywords: List<String>,
    val extractedEntity: String = ""
)

enum class AnswerStatus {
    SUCCESS,
    CONFLICT,
    INSUFFICIENT_DATA,
    OFFLINE,
    ERROR
}

enum class ConfidenceLevel {
    HIGH,
    MODERATE,
    LOW,
    CONFLICT
}

data class ConflictView(
    val claim: String,
    val sourceName: String,
    val sourceUrl: String,
    val trustTier: TrustTier
)

data class HowWeFoundItDetails(
    val totalSourcesQueried: Int,
    val highTrustSourcesCount: Int,
    val consensusSourcesCount: Int,
    val confidencePercentage: Int,
    val processingTimeMs: Long,
    val extractionMethod: String,
    val primaryDomainTypes: List<String>
)

data class SynthesizedAnswer(
    val query: String,
    val language: String,
    val status: AnswerStatus,
    val directAnswer: String,
    val confidenceLevel: ConfidenceLevel,
    val confidencePercentage: Int,
    val sources: List<SourceInfo>,
    val howWeFoundIt: HowWeFoundItDetails,
    val conflictViews: List<ConflictView> = emptyList(),
    val topSentences: List<CandidateSentence> = emptyList()
)
