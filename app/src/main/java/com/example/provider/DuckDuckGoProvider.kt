package com.example.provider

import com.example.data.model.SourceInfo
import com.example.network.HttpClientProvider
import com.example.nlp.QuestionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class DuckDuckGoProvider : SearchProvider {
    override val name: String = "DuckDuckGo Instant Knowledge"
    override val isEnabled: Boolean = true

    override suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SourceInfo>()
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val apiUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

                val request = Request.Builder().url(apiUrl).build()
                val response = HttpClientProvider.client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)

                // 1. Direct Abstract
                val abstractText = json.optString("AbstractText", "")
                val abstractUrl = json.optString("AbstractURL", "")
                val abstractSource = json.optString("AbstractSource", "DuckDuckGo")
                val heading = json.optString("Heading", query)

                if (abstractText.isNotBlank()) {
                    val url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encodedQuery" }
                    results.add(
                        SourceInfo(
                            title = heading,
                            url = url,
                            domain = DomainTrustFilter.extractDomain(url),
                            snippet = abstractText.take(180),
                            fullText = abstractText,
                            trustTier = DomainTrustFilter.classifyTrust(url),
                            sourceName = abstractSource,
                            providerName = "DuckDuckGo Knowledge",
                            extractedFact = abstractText
                        )
                    )
                }

                // 2. Direct Definition
                val definition = json.optString("Definition", "")
                val definitionUrl = json.optString("DefinitionURL", "")
                val definitionSource = json.optString("DefinitionSource", "")
                if (definition.isNotBlank()) {
                    val url = definitionUrl.ifBlank { "https://duckduckgo.com/?q=$encodedQuery" }
                    results.add(
                        SourceInfo(
                            title = definitionSource.ifBlank { "Definition" },
                            url = url,
                            domain = DomainTrustFilter.extractDomain(url),
                            snippet = definition.take(180),
                            fullText = definition,
                            trustTier = DomainTrustFilter.classifyTrust(url),
                            sourceName = definitionSource.ifBlank { "Official Definition" },
                            providerName = "DuckDuckGo Definition",
                            extractedFact = definition
                        )
                    )
                }

                // 3. Related topics
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until relatedTopics.length()) {
                        if (results.size >= 5) break
                        val topic = relatedTopics.optJSONObject(i) ?: continue
                        val text = topic.optString("Text", "")
                        val firstUrl = topic.optString("FirstURL", "")
                        if (text.isNotBlank() && firstUrl.isNotBlank() && DomainTrustFilter.isAllowedDomain(firstUrl)) {
                            results.add(
                                SourceInfo(
                                    title = text.substringBefore(" - ").take(60),
                                    url = firstUrl,
                                    domain = DomainTrustFilter.extractDomain(firstUrl),
                                    snippet = text.take(180),
                                    fullText = text,
                                    trustTier = DomainTrustFilter.classifyTrust(firstUrl),
                                    sourceName = DomainTrustFilter.extractDomain(firstUrl),
                                    providerName = "DuckDuckGo Reference",
                                    extractedFact = text
                                )
                            )
                        }
                    }
                }

            } catch (_: Exception) {
                // Ignore transient errors
            }

            results
        }
}
