package com.example.nlp

import com.example.data.model.AnswerStatus
import com.example.data.model.CandidateSentence
import com.example.data.model.ConfidenceLevel
import com.example.data.model.ConflictView
import com.example.data.model.HowWeFoundItDetails
import com.example.data.model.SourceInfo
import com.example.data.model.SynthesizedAnswer
import kotlin.math.min

object ConsensusEngine {

    fun synthesize(
        query: String,
        sources: List<SourceInfo>,
        rankedSentences: List<CandidateSentence>,
        analysis: QuestionAnalysis,
        durationMs: Long
    ): SynthesizedAnswer {
        val lang = analysis.language

        // If no sources or ranked sentences
        if (sources.isEmpty() && rankedSentences.isEmpty()) {
            val noDataMessage = when (lang) {
                "ar" -> "لم أجد معلومات موثوقة كافية للإجابة عن هذا السؤال."
                "fr" -> "Aucune information fiable suffisante pour répondre à cette question."
                else -> "Could not find sufficient reliable information to answer this question."
            }
            return SynthesizedAnswer(
                query = query,
                language = lang,
                status = AnswerStatus.INSUFFICIENT_DATA,
                directAnswer = noDataMessage,
                confidenceLevel = ConfidenceLevel.LOW,
                confidencePercentage = 0,
                sources = sources,
                howWeFoundIt = HowWeFoundItDetails(
                    totalSourcesQueried = sources.size,
                    highTrustSourcesCount = sources.count { it.trustTier.priorityRank <= 2 },
                    consensusSourcesCount = 0,
                    confidencePercentage = 0,
                    processingTimeMs = durationMs,
                    extractionMethod = "Deterministic Fact Extraction (0% AI)",
                    primaryDomainTypes = sources.map { it.trustTier.getLabel(lang) }.distinct()
                ),
                topSentences = rankedSentences
            )
        }

        // 1. Run FactExtractor for deterministic fact resolution (0% AI)
        val factResult = FactExtractor.extractFact(analysis, sources, rankedSentences)

        // 2. Count sources by trust tier
        val highTrustSources = sources.filter { it.trustTier.priorityRank <= 2 }

        // 3. Conflict detection across sources
        val consensusClusters = mutableListOf<MutableList<CandidateSentence>>()
        val topCandidates = rankedSentences.take(5)

        for (candidate in topCandidates) {
            var addedToCluster = false
            for (cluster in consensusClusters) {
                val rep = cluster.first()
                val similarity = calculateTokenOverlap(candidate.cleanSentence, rep.cleanSentence, lang)
                val entityMatch = candidate.extractedEntity.isNotBlank() &&
                        candidate.extractedEntity.equals(rep.extractedEntity, ignoreCase = true)

                if (similarity >= 0.45 || entityMatch) {
                    cluster.add(candidate)
                    addedToCluster = true
                    break
                }
            }
            if (!addedToCluster) {
                consensusClusters.add(mutableListOf(candidate))
            }
        }

        // Check if there is a conflict between mutually exclusive statements
        if (consensusClusters.size >= 2) {
            val cluster1 = consensusClusters[0]
            val cluster2 = consensusClusters[1]
            val score1 = cluster1.maxOf { it.score }
            val score2 = cluster2.maxOf { it.score }

            val entity1 = cluster1.firstOrNull { it.extractedEntity.isNotBlank() }?.extractedEntity
            val entity2 = cluster2.firstOrNull { it.extractedEntity.isNotBlank() }?.extractedEntity

            if (entity1 != null && entity2 != null && !entity1.equals(entity2, ignoreCase = true) && score2 > 0.7 * score1) {
                val conflictHeader = when (lang) {
                    "ar" -> "وجدت معلومات مختلفة بين المصادر."
                    "fr" -> "Il existe une divergence entre les sources."
                    else -> "There is a discrepancy between the sources."
                }

                val conflictViews = listOf(
                    ConflictView(
                        claim = cluster1.first().text,
                        sourceName = cluster1.first().domain,
                        sourceUrl = cluster1.first().sourceUrl,
                        trustTier = cluster1.first().trustTier
                    ),
                    ConflictView(
                        claim = cluster2.first().text,
                        sourceName = cluster2.first().domain,
                        sourceUrl = cluster2.first().sourceUrl,
                        trustTier = cluster2.first().trustTier
                    )
                )

                return SynthesizedAnswer(
                    query = query,
                    language = lang,
                    status = AnswerStatus.CONFLICT,
                    directAnswer = conflictHeader,
                    confidenceLevel = ConfidenceLevel.CONFLICT,
                    confidencePercentage = 50,
                    sources = sources,
                    howWeFoundIt = HowWeFoundItDetails(
                        totalSourcesQueried = sources.size,
                        highTrustSourcesCount = highTrustSources.size,
                        consensusSourcesCount = 0,
                        confidencePercentage = 50,
                        processingTimeMs = durationMs,
                        extractionMethod = "Multi-Source Discrepancy Detection (0% AI)",
                        primaryDomainTypes = sources.map { it.trustTier.getLabel(lang) }.distinct()
                    ),
                    conflictViews = conflictViews,
                    topSentences = rankedSentences
                )
            }
        }

        val status = if (factResult.isConfident) AnswerStatus.SUCCESS else AnswerStatus.INSUFFICIENT_DATA
        val confidenceScore = if (factResult.isConfident) 92 else 30
        val confidenceLevel = if (factResult.isConfident) ConfidenceLevel.HIGH else ConfidenceLevel.LOW

        return SynthesizedAnswer(
            query = query,
            language = lang,
            status = status,
            directAnswer = factResult.directAnswer,
            confidenceLevel = confidenceLevel,
            confidencePercentage = confidenceScore,
            sources = sources,
            howWeFoundIt = HowWeFoundItDetails(
                totalSourcesQueried = sources.size,
                highTrustSourcesCount = highTrustSources.size,
                consensusSourcesCount = sources.size.coerceAtLeast(1),
                confidencePercentage = confidenceScore,
                processingTimeMs = durationMs,
                extractionMethod = "Deterministic Fact Extraction (0% AI)",
                primaryDomainTypes = sources.map { it.trustTier.getLabel(lang) }.distinct()
            ),
            topSentences = rankedSentences
        )
    }

    private fun calculateTokenOverlap(s1: String, s2: String, lang: String): Double {
        val t1 = TextPreprocessor.tokenize(s1, lang).toSet()
        val t2 = TextPreprocessor.tokenize(s2, lang).toSet()
        if (t1.isEmpty() || t2.isEmpty()) return 0.0
        val intersection = t1.intersect(t2).size.toDouble()
        val union = t1.union(t2).size.toDouble()
        return if (union > 0) intersection / union else 0.0
    }
}
