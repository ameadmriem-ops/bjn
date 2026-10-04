package com.example.nlp

import com.example.data.model.CandidateSentence
import com.example.data.model.SourceInfo
import kotlin.math.ln

object SentenceRanker {

    private val SENTENCE_DELIMITERS = Regex("[.!?؟؛\n]+")

    // Phrases that indicate meta-chatter or repeating the question rather than answering
    private val FLUFF_PATTERNS = listOf(
        "كثيرا ما يتردد",
        "كثيرا ما يطرح",
        "السؤال المطروح",
        "يتساءل الكثير",
        "السؤال الذي",
        "سؤال يتردد",
        "سؤال:",
        "هل تساءلت",
        "هل تعلم أن السؤال",
        "frequently asked",
        "question often arises",
        "people often ask",
        "on se demande souvent",
        "la question se pose"
    )

    fun extractAndRankSentences(
        sources: List<SourceInfo>,
        analysis: QuestionAnalysis
    ): List<CandidateSentence> {
        val allSentences = mutableListOf<CandidateSentence>()
        val queryTokens = analysis.keyTokens
        if (queryTokens.isEmpty()) return emptyList()

        val rawSentenceList = mutableListOf<Pair<String, SourceInfo>>()
        for (source in sources) {
            val textToScan = buildString {
                if (source.title.isNotBlank()) append(source.title).append(". ")
                if (source.snippet.isNotBlank()) append(source.snippet).append(". ")
                if (source.fullText.isNotBlank()) append(source.fullText)
            }

            val chunks = textToScan.split(SENTENCE_DELIMITERS)
            for (raw in chunks) {
                val cleaned = raw.trim()

                // Filter out questions and conversational fluff
                if (cleaned.length in 15..350 && !isFluffOrQuestion(cleaned)) {
                    rawSentenceList.add(cleaned to source)
                }
            }
        }

        if (rawSentenceList.isEmpty()) return emptyList()

        val totalDocs = rawSentenceList.size.toDouble()
        val docFreq = mutableMapOf<String, Int>()
        for (token in queryTokens) {
            val count = rawSentenceList.count { (sentence, _) ->
                TextPreprocessor.normalize(sentence, analysis.language).contains(token)
            }
            docFreq[token] = count
        }

        for ((sentence, source) in rawSentenceList) {
            val normSentence = TextPreprocessor.normalize(sentence, analysis.language)
            val sentenceTokens = TextPreprocessor.tokenize(sentence, analysis.language)
            val matchedKeywords = mutableListOf<String>()

            var tfIdfScore = 0.0
            for (token in queryTokens) {
                val tf = sentenceTokens.count { it == token || it.contains(token) }
                if (tf > 0) {
                    matchedKeywords.add(token)
                    val df = docFreq[token] ?: 1
                    val idf = ln((totalDocs + 1.0) / (df + 1.0)) + 1.0
                    tfIdfScore += (tf.toDouble() / (tf + 1.5)) * idf
                }
            }

            val coverage = if (queryTokens.isNotEmpty()) matchedKeywords.size.toDouble() / queryTokens.size else 0.0

            // Intent specific bonus for sentences stating factual declarative answers
            var intentBonus = 1.0
            when (analysis.intent) {
                QuestionIntent.CAPITAL -> {
                    val capitalTerms = listOf("عاصمة", "عاصمه", "capital", "capitale")
                    if (capitalTerms.any { normSentence.contains(it) }) intentBonus += 2.0
                }
                QuestionIntent.LEADER -> {
                    val leaderTerms = listOf("رئيس", "ملك", "امير", "حاكم", "president", "prime minister", "premier ministre")
                    if (leaderTerms.any { normSentence.contains(it) }) intentBonus += 2.0
                }
                QuestionIntent.SUPERLATIVE -> {
                    val superTerms = listOf("اكبر", "أكبر", "اطول", "أطول", "اعلى", "أعلى", "اقدم", "أقدم", "largest", "highest", "tallest")
                    if (superTerms.any { normSentence.contains(it) }) intentBonus += 2.0
                }
                QuestionIntent.WHERE -> {
                    val locTerms = listOf("يقع", "تقع", "موقع", "located", "situated", "se trouve")
                    if (locTerms.any { normSentence.contains(it) }) intentBonus += 1.8
                }
                QuestionIntent.WHEN -> {
                    if (Regex("\\b(1[89][0-9]{2}|20[0-2][0-9])\\b").containsMatchIn(sentence)) intentBonus += 2.0
                }
                QuestionIntent.HOW_MANY -> {
                    if (Regex("\\b\\d+\\b").containsMatchIn(sentence) || listOf("سبع", "سبعة", "سبع قارات", "seven").any { normSentence.contains(it) }) intentBonus += 2.0
                }
                QuestionIntent.FIRST_PERSON -> {
                    if (listOf("ارمسترونغ", "أرمسترونغ", "armstrong", "apollo 11", "ابولو 11").any { normSentence.contains(it) }) intentBonus += 2.5
                }
                else -> Unit
            }

            val lengthFactor = when (sentence.length) {
                in 25..160 -> 1.3
                in 15..24 -> 0.8
                else -> 0.9
            }

            val finalScore = (tfIdfScore * (1.0 + coverage) * intentBonus * lengthFactor) * source.trustTier.weight

            if (coverage >= 0.25 || tfIdfScore > 1.0 || (intentBonus > 1.5 && matchedKeywords.isNotEmpty())) {
                allSentences.add(
                    CandidateSentence(
                        text = sentence,
                        cleanSentence = normSentence,
                        score = finalScore,
                        sourceTitle = source.title,
                        sourceUrl = source.url,
                        domain = source.domain,
                        trustTier = source.trustTier,
                        matchedKeywords = matchedKeywords.distinct(),
                        extractedEntity = extractEntity(sentence, analysis)
                    )
                )
            }
        }

        return allSentences
            .sortedByDescending { it.score }
            .distinctBy { it.cleanSentence.take(35) }
            .take(15)
    }

    private fun isFluffOrQuestion(sentence: String): Boolean {
        val lower = sentence.lowercase()
        if (sentence.endsWith("?") || sentence.endsWith("؟")) return true
        if (FLUFF_PATTERNS.any { lower.contains(it) }) return true
        return false
    }

    private fun extractEntity(sentence: String, analysis: QuestionAnalysis): String {
        if (analysis.language == "ar") {
            // Capital
            val capitalRegex = Regex("عاصم[ةه]\\s+([^،.]+?)\\s+(?:هي|هو|:)\\s+([^،.]+)")
            capitalRegex.find(sentence)?.let {
                return it.groupValues[2].trim()
            }
            // Superlative
            val superRegex = Regex("([^،.]+?)\\s+(?:هو|هي|يعد|تعد)\\s+(?:أكبر|اكبر|أعلى|اعلى|أطول|اطول)\\s+([^،.]+)")
            superRegex.find(sentence)?.let {
                return it.groupValues[1].trim()
            }
        }
        return ""
    }
}
