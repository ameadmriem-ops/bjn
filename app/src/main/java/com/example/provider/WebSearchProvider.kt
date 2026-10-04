package com.example.provider

import com.example.data.model.SourceInfo
import com.example.network.HttpClientProvider
import com.example.nlp.QuestionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

class WebSearchProvider(
    private val customApiUrl: () -> String = { "" },
    private val customApiKey: () -> String = { "" }
) : SearchProvider {
    override val name: String = "Open Web & Official Sources"
    override val isEnabled: Boolean = true

    override suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SourceInfo>()

            // 1. If custom Search API / proxy is configured, call it
            val apiUrl = customApiUrl()
            val apiKey = customApiKey()
            if (apiUrl.isNotBlank()) {
                val customResults = searchCustomApi(query, apiUrl, apiKey)
                if (customResults.isNotEmpty()) {
                    return@withContext customResults
                }
            }

            // 2. Query DuckDuckGo Lite / HTML web snippets
            try {
                val formBody = FormBody.Builder()
                    .add("q", query)
                    .add("b", "")
                    .add("kl", if (analysis.language == "ar") "xa-ar" else "us-en")
                    .build()

                val request = Request.Builder()
                    .url("https://html.duckduckgo.com/html/")
                    .post(formBody)
                    .header("Referer", "https://html.duckduckgo.com/")
                    .build()

                val response = HttpClientProvider.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    results.addAll(parseDuckDuckGoHtml(html))
                }
            } catch (_: Exception) {
                // Ignore transient network errors
            }

            results
        }

    private fun parseDuckDuckGoHtml(html: String): List<SourceInfo> {
        val list = mutableListOf<SourceInfo>()
        // Simple fast regex parser for duckduckgo html results without heavy overhead
        val resultBlockRegex = Regex(
            "<a\\s+class=\"result__url\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>[\\s\\S]*?<a\\s+class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>",
            RegexOption.IGNORE_CASE
        )

        val matches = resultBlockRegex.findAll(html).take(6)
        for (m in matches) {
            var rawUrl = m.groupValues[1].trim()
            val rawSnippet = cleanHtml(m.groupValues[3])

            // DuckDuckGo redirects: //duckduckgo.com/l/?uddg=http%3A%2F%2F...
            if (rawUrl.contains("uddg=")) {
                val encodedPart = rawUrl.substringAfter("uddg=").substringBefore("&")
                try {
                    rawUrl = URLDecoder.decode(encodedPart, "UTF-8")
                } catch (_: Exception) {}
            }

            if (rawUrl.startsWith("http") && DomainTrustFilter.isAllowedDomain(rawUrl)) {
                val domain = DomainTrustFilter.extractDomain(rawUrl)
                val trustTier = DomainTrustFilter.classifyTrust(rawUrl)

                if (rawSnippet.isNotBlank() && rawSnippet.length > 20) {
                    list.add(
                        SourceInfo(
                            title = domain,
                            url = rawUrl,
                            domain = domain,
                            snippet = rawSnippet,
                            fullText = rawSnippet,
                            trustTier = trustTier,
                            sourceName = domain,
                            providerName = "Web Source Crawler",
                            extractedFact = rawSnippet
                        )
                    )
                }
            }
        }
        return list
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun searchCustomApi(query: String, apiUrl: String, apiKey: String): List<SourceInfo> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val fullUrl = if (apiUrl.contains("?")) {
                "$apiUrl&q=$encodedQuery&key=$apiKey"
            } else {
                "$apiUrl?q=$encodedQuery&key=$apiKey"
            }
            val request = Request.Builder().url(fullUrl).build()
            val response = HttpClientProvider.client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            // Generic parser for items
            val list = mutableListOf<SourceInfo>()
            val json = org.json.JSONObject(body)
            val items = json.optJSONArray("items") ?: json.optJSONArray("results")
            if (items != null) {
                for (i in 0 until items.length().coerceAtMost(5)) {
                    val item = items.getJSONObject(i)
                    val title = item.optString("title", "")
                    val link = item.optString("link", item.optString("url", ""))
                    val snippet = item.optString("snippet", item.optString("content", ""))
                    if (link.isNotBlank() && DomainTrustFilter.isAllowedDomain(link)) {
                        list.add(
                            SourceInfo(
                                title = title,
                                url = link,
                                domain = DomainTrustFilter.extractDomain(link),
                                snippet = snippet,
                                fullText = snippet,
                                trustTier = DomainTrustFilter.classifyTrust(link),
                                sourceName = DomainTrustFilter.extractDomain(link),
                                providerName = "Custom Search API",
                                extractedFact = snippet
                            )
                        )
                    }
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
