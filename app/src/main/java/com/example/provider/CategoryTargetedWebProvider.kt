package com.example.provider

import com.example.data.model.SourceInfo
import com.example.data.model.TrustTier
import com.example.network.HttpClientProvider
import com.example.nlp.QuestionAnalysis
import com.example.nlp.QuestionCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Searches the open web targeting authoritative sources based on the question category.
 * (Government portals, Medical institutions, Developer docs, Reputable news, etc.)
 */
class CategoryTargetedWebProvider : SearchProvider {
    override val name: String = "Multi-Source Targeted Web Engine"
    override val isEnabled: Boolean = true

    override suspend fun search(query: String, analysis: QuestionAnalysis): List<SourceInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SourceInfo>()

            // 1. Build targeted search query based on category
            val targetedQuery = buildTargetedQuery(query, analysis)

            try {
                val formBody = FormBody.Builder()
                    .add("q", targetedQuery)
                    .add("b", "")
                    .add("kl", if (analysis.language == "ar") "xa-ar" else "us-en")
                    .build()

                val request = Request.Builder()
                    .url("https://html.duckduckgo.com/html/")
                    .post(formBody)
                    .header("Referer", "https://html.duckduckgo.com/")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = HttpClientProvider.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    results.addAll(parseSearchResults(html, analysis))
                }
            } catch (_: Exception) {
                // Ignore transient network errors
            }

            // 2. If targeted search gave few results, fallback to clean raw query
            if (results.size < 2) {
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
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build()

                    val response = HttpClientProvider.client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        results.addAll(parseSearchResults(html, analysis))
                    }
                } catch (_: Exception) {}
            }

            results.distinctBy { it.url }
        }

    private fun buildTargetedQuery(query: String, analysis: QuestionAnalysis): String {
        return when (analysis.category) {
            QuestionCategory.GEOGRAPHY_NATIONS -> {
                if (analysis.language == "ar") "$query موقع رسمي حكومي" else "$query official portal"
            }
            QuestionCategory.HEALTH_MEDICAL -> {
                if (analysis.language == "ar") "$query منظمة الصحة العالمية وزارة الصحة" else "$query WHO CDC Mayo Clinic"
            }
            QuestionCategory.TECH_DEVELOPER -> {
                "$query developer documentation official"
            }
            QuestionCategory.CORPORATE_BUSINESS -> {
                if (analysis.language == "ar") "$query تقرير رسمي شركة" else "$query official report company"
            }
            QuestionCategory.CURRENT_NEWS -> {
                if (analysis.language == "ar") "$query رويترز فرانس 24 الجزيرة" else "$query Reuters AP News BBC"
            }
            QuestionCategory.GENERAL_KNOWLEDGE -> {
                query
            }
        }
    }

    private fun parseSearchResults(html: String, analysis: QuestionAnalysis): List<SourceInfo> {
        val list = mutableListOf<SourceInfo>()
        val resultBlockRegex = Regex(
            "<a\\s+class=\"result__url\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>[\\s\\S]*?<a\\s+class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>",
            RegexOption.IGNORE_CASE
        )

        val matches = resultBlockRegex.findAll(html).take(6)
        for (m in matches) {
            var rawUrl = m.groupValues[1].trim()
            val rawSnippet = cleanHtml(m.groupValues[3])

            if (rawUrl.contains("uddg=")) {
                val encodedPart = rawUrl.substringAfter("uddg=").substringBefore("&")
                try {
                    rawUrl = URLDecoder.decode(encodedPart, "UTF-8")
                } catch (_: Exception) {}
            }

            if (rawUrl.startsWith("http") && DomainTrustFilter.isAllowedDomain(rawUrl)) {
                val domain = DomainTrustFilter.extractDomain(rawUrl)
                val trustTier = DomainTrustFilter.classifyTrust(rawUrl)

                if (rawSnippet.isNotBlank() && rawSnippet.length > 15) {
                    list.add(
                        SourceInfo(
                            title = formatSourceTitle(domain, trustTier),
                            url = rawUrl,
                            domain = domain,
                            snippet = rawSnippet,
                            fullText = rawSnippet,
                            trustTier = trustTier,
                            sourceName = domain,
                            providerName = "Targeted Web Indexer",
                            extractedFact = rawSnippet
                        )
                    )
                }
            }
        }
        return list
    }

    private fun formatSourceTitle(domain: String, tier: TrustTier): String {
        return when (tier) {
            TrustTier.GOVERNMENT_OFFICIAL -> "بوابة رسمية حكومية ($domain)"
            TrustTier.INTERNATIONAL_ORG -> "منظمة دولية معتمدة ($domain)"
            TrustTier.ACADEMIC_SCIENTIFIC -> "مؤسسة علمية / أكاديمية ($domain)"
            TrustTier.OFFICIAL_MEDICAL -> "مرجع طبي وصحي ($domain)"
            TrustTier.OFFICIAL_TECH -> "توثيق تقني رسمي ($domain)"
            TrustTier.OFFICIAL_CORPORATE -> "الموقع الرسمي ($domain)"
            TrustTier.REPUTABLE_NEWS -> "وكالة أنباء معتمدة ($domain)"
            TrustTier.GENERAL_REFERENCE -> "مرجع وموسوعة موثوقة ($domain)"
            TrustTier.WIKIPEDIA_AUXILIARY -> "ويكيبيديا (مصدر مساعد)"
            else -> domain
        }
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
}
