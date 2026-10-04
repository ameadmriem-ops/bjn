package com.example.data.model

enum class TrustTier(
    val weight: Double,
    val arabicLabel: String,
    val englishLabel: String,
    val frenchLabel: String,
    val priorityRank: Int
) {
    GOVERNMENT_OFFICIAL(
        weight = 2.0,
        arabicLabel = "موقع حكومي رسمي",
        englishLabel = "Official Government Source",
        frenchLabel = "Site gouvernemental officiel",
        priorityRank = 1
    ),
    INTERNATIONAL_ORG(
        weight = 1.9,
        arabicLabel = "منظمة دولية معتمدة",
        englishLabel = "International Organization (UN/World Bank)",
        frenchLabel = "Organisation internationale",
        priorityRank = 1
    ),
    ACADEMIC_SCIENTIFIC(
        weight = 1.8,
        arabicLabel = "جامعة / مؤسسة علمية",
        englishLabel = "Academic / Scientific Institution",
        frenchLabel = "Institution académique / scientifique",
        priorityRank = 1
    ),
    OFFICIAL_MEDICAL(
        weight = 1.7,
        arabicLabel = "مؤسسة صحية / طبية معتمدة",
        englishLabel = "Authoritative Health / Medical Institution",
        frenchLabel = "Institution médicale reconnue",
        priorityRank = 2
    ),
    OFFICIAL_TECH(
        weight = 1.6,
        arabicLabel = "توثيق تقني رسمي",
        englishLabel = "Official Technical Documentation",
        frenchLabel = "Documentation technique officielle",
        priorityRank = 2
    ),
    OFFICIAL_CORPORATE(
        weight = 1.5,
        arabicLabel = "موقع رسمي لمنظمة أو شركة",
        englishLabel = "Official Corporate / Org Portal",
        frenchLabel = "Site officiel d'entreprise",
        priorityRank = 2
    ),
    REPUTABLE_NEWS(
        weight = 1.4,
        arabicLabel = "وكالة أنباء موثوقة",
        englishLabel = "Reputable News Agency",
        frenchLabel = "Agence de presse réputée",
        priorityRank = 3
    ),
    GENERAL_REFERENCE(
        weight = 1.3,
        arabicLabel = "موسوعة ومرجع معتمد (بريتانيكا)",
        englishLabel = "Verified Reference (Britannica/Factbook)",
        frenchLabel = "Référence certifiée",
        priorityRank = 3
    ),
    WIKIPEDIA_AUXILIARY(
        weight = 0.8,
        arabicLabel = "ويكيبيديا (مصدر مساعد)",
        englishLabel = "Wikipedia (Auxiliary Reference)",
        frenchLabel = "Wikipédia (Source secondaire)",
        priorityRank = 4
    ),
    GENERAL_WEB(
        weight = 0.6,
        arabicLabel = "موقع معلومات عام",
        englishLabel = "General Web Resource",
        frenchLabel = "Ressource web générale",
        priorityRank = 5
    ),
    UNVERIFIED(
        weight = 0.2,
        arabicLabel = "مصدر غير معتمد",
        englishLabel = "Unverified Source",
        frenchLabel = "Source non vérifiée",
        priorityRank = 6
    );

    fun getLabel(lang: String): String {
        return when (lang.lowercase()) {
            "ar" -> arabicLabel
            "fr" -> frenchLabel
            else -> englishLabel
        }
    }
}
