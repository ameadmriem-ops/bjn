package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SearchCacheEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AnswerStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.HowWeFoundItDetails
import com.example.data.model.SourceInfo
import com.example.data.model.SynthesizedAnswer
import com.example.data.model.TrustTier
import com.example.network.NetworkUtils
import com.example.nlp.ConsensusEngine
import com.example.nlp.SentenceRanker
import com.example.nlp.TextPreprocessor
import com.example.provider.CategoryTargetedWebProvider
import com.example.provider.DuckDuckGoProvider
import com.example.provider.SearchProvider
import com.example.provider.WebSearchProvider
import com.example.provider.WikidataProvider
import com.example.provider.WikipediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class SearchRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    var customApiUrl: String = "",
    var customApiKey: String = ""
) {
    private val categoryTargetedWebProvider = CategoryTargetedWebProvider()
    private val webSearchProvider = WebSearchProvider(
        customApiUrl = { customApiUrl },
        customApiKey = { customApiKey }
    )
    private val duckDuckGoProvider = DuckDuckGoProvider()
    private val wikidataProvider = WikidataProvider()
    private val wikipediaProvider = WikipediaProvider() // Auxiliary only

    // Multi-source providers arranged with official, targeted and open-web first
    private val allProviders: List<SearchProvider> = listOf(
        categoryTargetedWebProvider,
        webSearchProvider,
        duckDuckGoProvider,
        wikidataProvider,
        wikipediaProvider
    )

    // Fast in-memory cache for sub-millisecond repeated queries
    private val memoryCache = ConcurrentHashMap<String, SynthesizedAnswer>()

    fun getHistory(): Flow<List<SearchHistoryEntity>> {
        return database.searchHistoryDao().getAllHistory()
    }

    suspend fun clearHistory() {
        database.searchHistoryDao().clearAll()
    }

    suspend fun deleteHistory(id: Long) {
        database.searchHistoryDao().deleteById(id)
    }

    suspend fun clearCache() {
        memoryCache.clear()
        database.searchCacheDao().clearAll()
    }

    suspend fun executeSearch(
        rawQuery: String,
        maxSources: Int = 4,
        strictTrustOnly: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): SynthesizedAnswer = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val query = rawQuery.trim()

        if (query.isBlank()) {
            return@withContext SynthesizedAnswer(
                query = "",
                language = "ar",
                status = AnswerStatus.INSUFFICIENT_DATA,
                directAnswer = "يرجى كتابة سؤال للبحث.",
                confidenceLevel = ConfidenceLevel.LOW,
                confidencePercentage = 0,
                sources = emptyList(),
                howWeFoundIt = HowWeFoundItDetails(0, 0, 0, 0, 0, "None", emptyList())
            )
        }

        // 1. Language & Question Classification & Routing
        val analysis = TextPreprocessor.analyzeQuestion(query)
        val lang = analysis.language
        val cacheKey = "${lang}_${analysis.normalizedQuery}"

        // 2. Cache Check (Only for non-time-sensitive factual questions)
        if (!analysis.isTimeSensitive) {
            // Memory Cache (< 1ms)
            memoryCache[cacheKey]?.let { cached ->
                saveHistory(query, cached.directAnswer, cached.sources.size, cached.status.name, lang, serializeSources(cached.sources), cached.confidencePercentage)
                return@withContext cached
            }

            // Room Database Cache (< 15ms)
            val cachedEntity = database.searchCacheDao().getCache(cacheKey)
            if (cachedEntity != null && System.currentTimeMillis() - cachedEntity.timestamp < 24 * 60 * 60 * 1000L) {
                val restoredSources = deserializeSources(cachedEntity.sourcesJson)
                val duration = System.currentTimeMillis() - startTime
                val statusEnum = try { AnswerStatus.valueOf(cachedEntity.status) } catch (_: Exception) { AnswerStatus.SUCCESS }
                val answer = SynthesizedAnswer(
                    query = query,
                    language = lang,
                    status = statusEnum,
                    directAnswer = cachedEntity.directAnswer,
                    confidenceLevel = if (cachedEntity.confidencePercentage >= 80) ConfidenceLevel.HIGH else ConfidenceLevel.MODERATE,
                    confidencePercentage = cachedEntity.confidencePercentage,
                    sources = restoredSources,
                    howWeFoundIt = HowWeFoundItDetails(
                        totalSourcesQueried = cachedEntity.sourcesCount,
                        highTrustSourcesCount = restoredSources.count { it.trustTier.priorityRank <= 2 },
                        consensusSourcesCount = restoredSources.count { it.isVerified },
                        confidencePercentage = cachedEntity.confidencePercentage,
                        processingTimeMs = duration,
                        extractionMethod = "Cache (0% AI)",
                        primaryDomainTypes = restoredSources.map { it.trustTier.getLabel(lang) }.distinct()
                    )
                )
                memoryCache[cacheKey] = answer
                saveHistory(query, answer.directAnswer, answer.sources.size, answer.status.name, lang, cachedEntity.sourcesJson, answer.confidencePercentage)
                return@withContext answer
            }
        }

        // 3. Connectivity check
        if (!NetworkUtils.isOnline(context)) {
            val offlineMsg = if (lang == "ar") {
                "لا يوجد اتصال بالإنترنت. يرجى الاتصال بالإنترنت وإعادة المحاولة."
            } else {
                "No internet connection. Please check your network and try again."
            }
            return@withContext SynthesizedAnswer(
                query = query,
                language = lang,
                status = AnswerStatus.OFFLINE,
                directAnswer = offlineMsg,
                confidenceLevel = ConfidenceLevel.LOW,
                confidencePercentage = 0,
                sources = emptyList(),
                howWeFoundIt = HowWeFoundItDetails(0, 0, 0, 0, 0, "Offline", emptyList())
            )
        }

        // 4. Parallel multi-source search across real web sources
        val collectedSources = mutableListOf<SourceInfo>()
        coroutineScope {
            val tasks = allProviders.map { provider ->
                async {
                    withTimeoutOrNull(2200L) {
                        try {
                            provider.search(query, analysis)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()
                }
            }
            val resultsList = tasks.awaitAll()
            for (res in resultsList) {
                collectedSources.addAll(res)
            }
        }

        // 5. Strict Hierarchical Source Ranking:
        // Official Gov (Rank 1, Weight 2.0) -> Int'l Org (Rank 1, Weight 1.9) -> Academic/Science (Rank 1, Weight 1.8) -> News/Tech/Medical (Rank 2) -> General Reference (Rank 3) -> Wikipedia (Rank 4, Weight 0.8)
        var sortedSources = collectedSources
            .distinctBy { it.url }
            .distinctBy { it.title.take(30) + it.domain }
            .sortedWith(
                compareBy<SourceInfo> { it.trustTier.priorityRank }
                    .thenByDescending { it.trustTier.weight }
            )

        if (strictTrustOnly) {
            sortedSources = sortedSources.filter { it.trustTier.priorityRank <= 2 }
        }

        val topSources = sortedSources.take(maxSources.coerceIn(3, 6))

        // 6. Sentence extraction & ranking (BM25)
        val rankedSentences = SentenceRanker.extractAndRankSentences(topSources, analysis)

        // 7. Deterministic Fact Extraction & Consensus (0% AI)
        val durationMs = System.currentTimeMillis() - startTime
        val synthesized = ConsensusEngine.synthesize(
            query = query,
            sources = topSources,
            rankedSentences = rankedSentences,
            analysis = analysis,
            durationMs = durationMs
        )

        // 8. Cache result & Save history (only cache static facts, not time-sensitive facts)
        if (synthesized.status == AnswerStatus.SUCCESS || synthesized.status == AnswerStatus.CONFLICT) {
            val sourcesJsonStr = serializeSources(synthesized.sources)

            if (!analysis.isTimeSensitive) {
                memoryCache[cacheKey] = synthesized
                database.searchCacheDao().insert(
                    SearchCacheEntity(
                        queryKey = cacheKey,
                        directAnswer = synthesized.directAnswer,
                        language = lang,
                        status = synthesized.status.name,
                        sourcesCount = synthesized.sources.size,
                        sourcesJson = sourcesJsonStr,
                        confidencePercentage = synthesized.confidencePercentage,
                        howWeFoundItJson = ""
                    )
                )
            }

            saveHistory(
                query = query,
                answer = synthesized.directAnswer,
                sourcesCount = synthesized.sources.size,
                status = synthesized.status.name,
                language = lang,
                sourcesJson = sourcesJsonStr,
                confidence = synthesized.confidencePercentage
            )
        }

        synthesized
    }

    private suspend fun saveHistory(
        query: String,
        answer: String,
        sourcesCount: Int,
        status: String,
        language: String,
        sourcesJson: String = "",
        confidence: Int = 85
    ) {
        try {
            database.searchHistoryDao().insert(
                SearchHistoryEntity(
                    query = query,
                    directAnswer = answer,
                    sourcesCount = sourcesCount,
                    status = status,
                    language = language,
                    sourcesJson = sourcesJson,
                    confidencePercentage = confidence
                )
            )
        } catch (_: Exception) {}
    }

    fun restoreAnswerFromHistory(history: SearchHistoryEntity): SynthesizedAnswer {
        val sources = if (history.sourcesJson.isNotBlank()) {
            deserializeSources(history.sourcesJson)
        } else emptyList()

        val statusEnum = try {
            AnswerStatus.valueOf(history.status)
        } catch (_: Exception) {
            AnswerStatus.SUCCESS
        }

        return SynthesizedAnswer(
            query = history.query,
            language = history.language,
            status = statusEnum,
            directAnswer = history.directAnswer,
            confidenceLevel = if (history.confidencePercentage >= 80) ConfidenceLevel.HIGH else ConfidenceLevel.MODERATE,
            confidencePercentage = history.confidencePercentage.coerceAtLeast(70),
            sources = sources,
            howWeFoundIt = HowWeFoundItDetails(
                totalSourcesQueried = history.sourcesCount,
                highTrustSourcesCount = sources.count { it.trustTier.priorityRank <= 2 },
                consensusSourcesCount = sources.count { it.isVerified },
                confidencePercentage = history.confidencePercentage.coerceAtLeast(70),
                processingTimeMs = 45,
                extractionMethod = "History (0% AI)",
                primaryDomainTypes = sources.map { it.trustTier.getLabel(history.language) }.distinct()
            )
        )
    }

    private fun serializeSources(sources: List<SourceInfo>): String {
        val arr = JSONArray()
        for (s in sources) {
            val obj = JSONObject()
            obj.put("title", s.title)
            obj.put("url", s.url)
            obj.put("domain", s.domain)
            obj.put("snippet", s.snippet)
            obj.put("trustTier", s.trustTier.name)
            obj.put("sourceName", s.sourceName)
            obj.put("providerName", s.providerName)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeSources(json: String): List<SourceInfo> {
        val list = mutableListOf<SourceInfo>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tierName = obj.optString("trustTier", TrustTier.GENERAL_WEB.name)
                val tier = try {
                    TrustTier.valueOf(tierName)
                } catch (_: Exception) {
                    TrustTier.GENERAL_WEB
                }
                list.add(
                    SourceInfo(
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        domain = obj.optString("domain"),
                        snippet = obj.optString("snippet"),
                        trustTier = tier,
                        sourceName = obj.optString("sourceName"),
                        providerName = obj.optString("providerName")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
