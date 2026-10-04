package com.example.provider

import com.example.data.model.TrustTier
import java.net.URI
import java.util.Locale

object DomainTrustFilter {

    private val GOV_TLDS = setOf(".gov", ".mil", ".gouv.fr", ".gov.uk", ".gov.ma", ".gov.sa", ".gov.eg", ".gov.ae", ".gov.qa")
    private val EDU_TLDS = setOf(".edu", ".ac.uk", ".ens.fr", ".edu.eg", ".edu.sa", ".edu.ma")

    private val INTERNATIONAL_ORGS = setOf(
        "un.org", "who.int", "worldbank.org", "imf.org", "unesco.org",
        "wmo.int", "fao.org", "oecd.org", "wto.org", "iaea.org", "unicef.org"
    )

    private val SCIENTIFIC_DOMAINS = setOf(
        "nasa.gov", "cern.ch", "nature.com", "science.org", "sciencedirect.com",
        "ieee.org", "noaa.gov", "nationalgeographic.com", "usgs.gov",
        "ox.ac.uk", "cam.ac.uk", "mit.edu", "harvard.edu", "stanford.edu", "caltech.edu"
    )

    private val MEDICAL_DOMAINS = setOf(
        "cdc.gov", "nih.gov", "mayoclinic.org", "nhs.uk", "ncbi.nlm.nih.gov",
        "hopkinsmedicine.org", "clevelandclinic.org", "thelancet.com"
    )

    private val TECH_DOCS_DOMAINS = setOf(
        "developer.android.com", "android.com", "developers.google.com",
        "developer.apple.com", "developer.mozilla.org", "github.com",
        "w3.org", "kotlinlang.org", "docs.oracle.com", "learn.microsoft.com"
    )

    private val REFERENCE_DOMAINS = setOf(
        "britannica.com", "worldatlas.com", "merriam-webster.com", "larousse.fr", "almaany.com"
    )

    private val NEWS_DOMAINS = setOf(
        "reuters.com", "apnews.com", "bbc.com", "bbc.co.uk", "aljazeera.net",
        "aljazeera.com", "france24.com", "dw.com", "afp.com", "economist.com",
        "theguardian.com", "nytimes.com", "wsj.com", "bloomberg.com", "lemonde.fr"
    )

    private val BLOCKED_DOMAINS = setOf(
        "quora.com", "reddit.com", "pinterest.com", "facebook.com", "twitter.com", "x.com",
        "tiktok.com", "instagram.com", "answers.com", "yahoo.com"
    )

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase(Locale.ROOT) ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (_: Exception) {
            url.substringBefore("/").substringAfter("://").lowercase(Locale.ROOT)
        }
    }

    fun classifyTrust(domainOrUrl: String): TrustTier {
        val domain = extractDomain(domainOrUrl)

        if (BLOCKED_DOMAINS.any { domain.contains(it) }) {
            return TrustTier.UNVERIFIED
        }

        // 1. Government Official Sites
        if (GOV_TLDS.any { domain.endsWith(it) || domain.contains(it) }) {
            return TrustTier.GOVERNMENT_OFFICIAL
        }

        // 2. International Organizations
        if (INTERNATIONAL_ORGS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.INTERNATIONAL_ORG
        }

        // 3. Medical / Health Sites
        if (MEDICAL_DOMAINS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.OFFICIAL_MEDICAL
        }

        // 4. Academic / Scientific
        if (EDU_TLDS.any { domain.endsWith(it) || domain.contains(it) } ||
            SCIENTIFIC_DOMAINS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.ACADEMIC_SCIENTIFIC
        }

        // 5. Tech Docs / Developer Portals
        if (TECH_DOCS_DOMAINS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.OFFICIAL_TECH
        }

        // 6. Encyclopedia & Reference (Britannica, World Factbook)
        if (REFERENCE_DOMAINS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.GENERAL_REFERENCE
        }

        // 7. Reputable News
        if (NEWS_DOMAINS.any { domain == it || domain.endsWith(".$it") }) {
            return TrustTier.REPUTABLE_NEWS
        }

        // 8. Wikipedia (Auxiliary only)
        if (domain.contains("wikipedia.org") || domain.contains("wikimedia.org") || domain.contains("wikidata.org")) {
            return TrustTier.WIKIPEDIA_AUXILIARY
        }

        return TrustTier.GENERAL_WEB
    }

    fun isAllowedDomain(url: String): Boolean {
        val domain = extractDomain(url)
        return !BLOCKED_DOMAINS.any { domain.contains(it) }
    }
}
