package com.example.provider

import com.example.data.model.SourceInfo
import com.example.network.HttpClientProvider
import com.example.nlp.QuestionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class WikipediaProvider : SearchProvider {
    override val name: String = "Wikipedia / Wikimedia"
    override val isEnabled: Boolean = true

    override suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SourceInfo>()
            val lang = analysis.language
            val searchTerms = analysis.searchTerms.ifEmpty { listOf(query) }
            val primaryTerm = searchTerms.first()

            try {
                val encodedTerm = URLEncoder.encode(primaryTerm, "UTF-8")
                val searchUrl = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedTerm&format=json&utf8=1&srlimit=4"

                val request = Request.Builder().url(searchUrl).build()
                val response = HttpClientProvider.client.newCall(request).execute()
                val bodyString = response.body?.string() ?: return@withContext emptyList()

                val rootJson = JSONObject(bodyString)
                val queryObj = rootJson.optJSONObject("query") ?: return@withContext emptyList()
                val searchArray = queryObj.optJSONArray("search") ?: return@withContext emptyList()

                val titlesToFetch = mutableListOf<String>()
                for (i in 0 until searchArray.length()) {
                    val item = searchArray.getJSONObject(i)
                    val title = item.optString("title")
                    if (title.isNotBlank()) {
                        titlesToFetch.add(title)
                    }
                }

                // Fetch page summary as auxiliary source only (max 1)
                coroutineScope {
                    val summaryJobs = titlesToFetch.take(1).map { title ->
                        async {
                            fetchPageSummary(title, lang)
                        }
                    }
                    val summaries = summaryJobs.awaitAll().filterNotNull()
                    results.addAll(summaries)
                }

            } catch (_: Exception) {
                // Ignore network/json failures and return whatever was collected
            }

            results
        }

    private fun fetchPageSummary(title: String, lang: String): SourceInfo? {
        return try {
            val encodedTitle = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
            val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"
            val request = Request.Builder().url(summaryUrl).build()
            val response = HttpClientProvider.client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string() ?: return null)

            val extract = json.optString("extract", "")
            val description = json.optString("description", "")
            val contentUrls = json.optJSONObject("content_urls")
            val desktopUrl = contentUrls?.optJSONObject("desktop")?.optString("page", "https://$lang.wikipedia.org/wiki/$encodedTitle")
                ?: "https://$lang.wikipedia.org/wiki/$encodedTitle"

            if (extract.isBlank()) return null

            SourceInfo(
                title = title,
                url = desktopUrl,
                domain = "$lang.wikipedia.org",
                snippet = description.ifBlank { extract.take(160) },
                fullText = extract,
                trustTier = DomainTrustFilter.classifyTrust(desktopUrl),
                sourceName = "Wikipedia ($lang)",
                providerName = "Wikipedia REST API",
                extractedFact = extract.take(200)
            )
        } catch (_: Exception) {
            null
        }
    }
}
