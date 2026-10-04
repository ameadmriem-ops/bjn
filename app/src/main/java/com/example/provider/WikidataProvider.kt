package com.example.provider

import com.example.data.model.SourceInfo
import com.example.network.HttpClientProvider
import com.example.nlp.QuestionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class WikidataProvider : SearchProvider {
    override val name: String = "Wikidata Open Knowledge Base"
    override val isEnabled: Boolean = true

    override suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SourceInfo>()
            try {
                val searchTerm = analysis.targetSubject.ifBlank { query }
                val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
                val lang = analysis.language

                val url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=$encodedTerm&language=$lang&format=json&limit=3&uselang=$lang"

                val request = Request.Builder().url(url).build()
                val response = HttpClientProvider.client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val searchArr = json.optJSONArray("search") ?: return@withContext emptyList()

                for (i in 0 until searchArr.length()) {
                    val item = searchArr.getJSONObject(i)
                    val label = item.optString("label", "")
                    val description = item.optString("description", "")
                    val conceptUri = item.optString("concepturi", "https://www.wikidata.org")

                    if (label.isNotBlank() && description.isNotBlank()) {
                        val fullSentence = when (lang) {
                            "ar" -> "$label: $description."
                            "fr" -> "$label : $description."
                            else -> "$label is $description."
                        }

                        results.add(
                            SourceInfo(
                                title = label,
                                url = conceptUri,
                                domain = "wikidata.org",
                                snippet = description,
                                fullText = fullSentence,
                                trustTier = DomainTrustFilter.classifyTrust(conceptUri),
                                sourceName = "Wikidata Knowledge Graph",
                                providerName = "Wikidata API",
                                extractedFact = fullSentence
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore transient errors
            }

            results
        }
}
