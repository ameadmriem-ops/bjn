package com.example.nlp

import java.util.Locale

data class QuestionAnalysis(
    val originalQuery: String,
    val normalizedQuery: String,
    val language: String, // "ar", "en", "fr"
    val intent: QuestionIntent,
    val category: QuestionCategory = QuestionCategory.GENERAL_KNOWLEDGE,
    val isTimeSensitive: Boolean = false,
    val keyTokens: List<String>,
    val targetSubject: String,
    val searchTerms: List<String>
)

enum class QuestionCategory {
    GEOGRAPHY_NATIONS,      // عواصم، دول، جغرافيا -> مصادر حكومية، بريتانيكا، الأمم المتحدة
    HEALTH_MEDICAL,         // أعراض، أمراض، أدوية، صحة -> منظمة الصحة العالمية، CDC، Mayo Clinic
    TECH_DEVELOPER,         // برمجة، SDK، تقنية -> توثيق رسمي، Android Developers، Google
    CORPORATE_BUSINESS,     // شركات، اقتصاد -> بيانات رسمية، رويترز، بلومبرغ
    CURRENT_NEWS,           // أخبار جارية، مناصب سياسية -> رويترز، فرانس 24، بي بي سي
    GENERAL_KNOWLEDGE       // علوم، مراجع عامة -> مؤسسات علمية، جامعات، بريتانيكا
}

enum class QuestionIntent {
    WHO,            // من هو رئيس فرنسا؟ / Who is... / Qui est...
    WHAT,           // ما / ماذا / What / Qu'est-ce que
    WHERE,          // أين تقع مراكش؟ / Where is... / Où est...
    WHEN,           // متى تأسست الأمم المتحدة؟ / When was... / Quand est-ce que...
    WHICH,          // أي... / Which... / Quel...
    HOW_MANY,       // كم عدد القارات؟ / How many... / Combien de...
    HOW_MUCH,       // كم يبلغ / كم سعر / How much...
    YES_NO,         // هل... / Is it... / Est-ce que...
    DEFINITION,     // ما هو / ما مفهوم / What is
    COMPARISON,     // ما الفرق بين / Difference / Comparaison
    CAPITAL,        // ما عاصمة المغرب؟
    SUPERLATIVE,    // ما أكبر محيط / ما أعلى جبل
    LEADER,         // رئيس / ملك / رئيس وزراء
    FIRST_PERSON    // من أول إنسان مشى على القمر؟
}

object TextPreprocessor {

    private val ARABIC_STOP_WORDS = setOf(
        "من", "في", "على", "إلى", "عن", "ما", "ماذا", "هل", "هو", "هي", "هم", "هن",
        "هذا", "هذه", "ذلك", "تلك", "كان", "كانت", "يكون", "تكون", "أن", "إن", "التي",
        "الذي", "الذين", "مع", "أو", "ثم", "بل", "لكن", "لقد", "قد", "أي", "أين",
        "متى", "كيف", "كم", "لماذا", "ماذا", "يا", "كل", "بعض", "غير", "بين"
    )

    private val ENGLISH_STOP_WORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
        "between", "both", "but", "by", "could", "did", "do", "does", "doing", "down",
        "during", "each", "few", "for", "from", "further", "had", "has", "have",
        "having", "he", "her", "here", "hers", "herself", "him", "himself", "his",
        "how", "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more",
        "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only",
        "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
        "same", "she", "should", "so", "some", "such", "than", "that", "the", "their",
        "theirs", "them", "themselves", "then", "there", "these", "they", "this",
        "those", "through", "to", "too", "under", "until", "up", "very", "was", "we",
        "were", "what", "when", "where", "which", "while", "who", "whom", "why",
        "with", "would", "you", "your", "yours", "yourself", "yourselves"
    )

    private val FRENCH_STOP_WORDS = setOf(
        "le", "la", "les", "un", "une", "des", "du", "de", "d", "l", "ce", "cet",
        "cette", "ces", "mon", "ton", "son", "notre", "votre", "leur", "je", "tu",
        "il", "elle", "nous", "vous", "ils", "elles", "on", "qui", "que", "quoi",
        "dont", "ou", "où", "quand", "comment", "combien", "pourquoi", "est", "sont",
        "et", "ou", "mais", "donc", "or", "ni", "car", "dans", "par", "pour", "en",
        "sur", "avec", "sans", "sous"
    )

    fun detectLanguage(text: String): String {
        var arabicChars = 0
        var latinChars = 0
        for (char in text) {
            val code = char.code
            if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF) {
                arabicChars++
            } else if (char.isLetter()) {
                latinChars++
            }
        }
        if (arabicChars > latinChars) return "ar"

        val lower = text.lowercase(Locale.ROOT)
        val frenchMarkers = listOf(" quelle ", " quel ", " qui ", " est-ce ", " où ", " quand ", " pourquoi ", " dans ", " avec ")
        if (frenchMarkers.any { lower.contains(it) } || lower.contains("capitale") || lower.contains("océan")) {
            return "fr"
        }
        return "en"
    }

    fun normalizeArabic(text: String): String {
        return text
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "") // Remove tashkeel/diacritics
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[؟?!.,،:;\"'()\\[\\]\\-–—/]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizeLatin(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[?!.,:;\"'()\\[\\]\\-–—/]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalize(text: String, lang: String): String {
        return if (lang == "ar") normalizeArabic(text) else normalizeLatin(text)
    }

    fun tokenize(text: String, lang: String): List<String> {
        val clean = normalize(text, lang)
        val stopWords = when (lang) {
            "ar" -> ARABIC_STOP_WORDS
            "fr" -> FRENCH_STOP_WORDS
            else -> ENGLISH_STOP_WORDS
        }
        return clean.split(" ")
            .map { it.trim() }
            .filter { it.length > 1 && !stopWords.contains(it) }
    }

    /**
     * Deterministic Question Classifier & Routing Engine (0% AI)
     */
    fun analyzeQuestion(query: String): QuestionAnalysis {
        val lang = detectLanguage(query)
        val normalized = normalize(query, lang)
        val lowerOriginal = query.lowercase(Locale.ROOT).trim()

        var intent = QuestionIntent.WHAT
        var targetSubject = ""
        val searchTerms = mutableListOf<String>()

        if (lang == "ar") {
            when {
                // CAPITAL: ما عاصمة المغرب؟
                normalized.contains("عاصمه") || normalized.contains("عاصمة") -> {
                    intent = QuestionIntent.CAPITAL
                    targetSubject = normalized
                        .replace(Regex("^(?:ما|ما هي|ما هو)\\s+"), "")
                        .replace(Regex("عاصم[ةه]\\s+"), "")
                        .trim()
                    searchTerms.add("عاصمة $targetSubject")
                    searchTerms.add(targetSubject)
                }

                // FIRST PERSON ON MOON / INVENTOR
                (normalized.contains("اول") || normalized.contains("أول")) && (normalized.contains("قمر") || normalized.contains("مشى") || normalized.contains("وصل")) -> {
                    intent = QuestionIntent.FIRST_PERSON
                    targetSubject = "أول إنسان مشى على القمر"
                    searchTerms.add("أول إنسان مشى على القمر")
                    searchTerms.add("Neil Armstrong Apollo 11")
                }

                // LEADER: من هو رئيس فرنسا؟ / ملك المغرب
                (normalized.contains("رئيس") || normalized.contains("ملك") || normalized.contains("رئيس وزراء")) && (normalized.contains("من") || normalized.contains("ما")) -> {
                    intent = QuestionIntent.LEADER
                    targetSubject = normalized
                        .replace(Regex("^(?:من هو|من هي|من|ما هو|ما هي)\\s+"), "")
                        .trim()
                    searchTerms.add(targetSubject)
                    searchTerms.add("منصب $targetSubject")
                }

                // WHO: من...
                normalized.startsWith("من ") || normalized.startsWith("من هو ") || normalized.startsWith("من هي ") -> {
                    intent = QuestionIntent.WHO
                    targetSubject = normalized.replace(Regex("^(?:من هو|من هي|من)\\s+"), "").trim()
                    searchTerms.add(targetSubject)
                }

                // SUPERLATIVE: أكبر محيط / أعلى جبل / أطول نهر
                normalized.contains("اكبر") || normalized.contains("أكبر") ||
                        normalized.contains("اعلي") || normalized.contains("اعلى") || normalized.contains("أعلى") ||
                        normalized.contains("اطول") || normalized.contains("أطول") ||
                        normalized.contains("اقدم") || normalized.contains("أقدم") ||
                        normalized.contains("اصغر") || normalized.contains("أصغر") ||
                        normalized.contains("اعمق") || normalized.contains("أعمق") -> {
                    intent = QuestionIntent.SUPERLATIVE
                    val words = normalized.split(" ")
                    val superIdx = words.indexOfFirst { it in listOf("اكبر", "أكبر", "اعلي", "اعلى", "أعلى", "اطول", "أطول", "اقدم", "أقدم", "اصغر", "أصغر", "اعمق", "أعمق") }
                    if (superIdx != -1) {
                        targetSubject = words.subList(superIdx, words.size).joinToString(" ")
                    } else {
                        targetSubject = normalized
                    }
                    searchTerms.add(targetSubject)
                }

                // WHERE: أين تقع مراكش؟
                normalized.contains("اين") || normalized.contains("أين") || normalized.contains("موقع") || normalized.contains("تقع") || normalized.contains("يقع") -> {
                    intent = QuestionIntent.WHERE
                    targetSubject = normalized
                        .replace(Regex("^(?:أين تقع|اين تقع|أين يقع|اين يقع|أين|اين|موقع)\\s+"), "")
                        .trim()
                    searchTerms.add("موقع $targetSubject")
                    searchTerms.add(targetSubject)
                }

                // WHEN: متى تأسست الأمم المتحدة؟
                normalized.contains("متي") || normalized.contains("متى") || normalized.contains("تاريخ") || normalized.contains("تاسست") || normalized.contains("تأسست") -> {
                    intent = QuestionIntent.WHEN
                    targetSubject = normalized
                        .replace(Regex("^(?:متى تأسست|متى تاسست|متي تأسست|متي تاسست|متى ولد|متى توفي|متى حدث|متى تاريخ|متى|متي|تاريخ تأسيس|تاريخ تاسيس|تاريخ)\\s+"), "")
                        .trim()
                    searchTerms.add("تاريخ تأسيس $targetSubject")
                    searchTerms.add(targetSubject)
                }

                // HOW MANY: كم عدد القارات؟
                normalized.contains("كم عدد") || normalized.contains("كم قارة") || normalized.contains("كم دولة") -> {
                    intent = QuestionIntent.HOW_MANY
                    targetSubject = normalized.replace("كم عدد", "").replace("كم", "").trim()
                    searchTerms.add("عدد $targetSubject")
                    searchTerms.add(targetSubject)
                }

                // HOW MUCH: كم يبلغ / كم مساحة
                normalized.contains("كم يبلغ") || normalized.contains("كم مساحة") || normalized.contains("كم سعر") -> {
                    intent = QuestionIntent.HOW_MUCH
                    targetSubject = normalized.replace("كم يبلغ", "").replace("كم مساحة", "").replace("كم", "").trim()
                    searchTerms.add(targetSubject)
                }

                // COMPARISON: الفرق بين
                normalized.contains("الفرق بين") || normalized.contains("مقارنة") -> {
                    intent = QuestionIntent.COMPARISON
                    targetSubject = normalized.replace("ما الفرق بين", "").replace("الفرق بين", "").trim()
                    searchTerms.add(targetSubject)
                }

                // YES/NO: هل
                normalized.startsWith("هل ") -> {
                    intent = QuestionIntent.YES_NO
                    targetSubject = normalized.replace("هل", "").trim()
                    searchTerms.add(targetSubject)
                }

                // DEFINITION / WHAT: ما هو / ما هي
                else -> {
                    intent = QuestionIntent.DEFINITION
                    targetSubject = normalized.replace(Regex("^(?:ما هو|ما هي|ماذا يعني|ما مفهوم|ماذا|ما)\\s+"), "").trim()
                    searchTerms.add(targetSubject)
                }
            }
        } else if (lang == "fr") {
            when {
                lowerOriginal.contains("capitale") -> {
                    intent = QuestionIntent.CAPITAL
                    targetSubject = lowerOriginal.replace(Regex("^(?:quelle est la capitale de|quelle est la capitale du|capitale de|capitale du)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("capitale $targetSubject")
                }
                lowerOriginal.contains("premier homme") && lowerOriginal.contains("lune") -> {
                    intent = QuestionIntent.FIRST_PERSON
                    targetSubject = "premier homme sur la Lune"
                    searchTerms.add("premier homme sur la Lune Neil Armstrong")
                }
                lowerOriginal.contains("président") || lowerOriginal.contains("president") -> {
                    intent = QuestionIntent.LEADER
                    targetSubject = lowerOriginal.replace(Regex("^(?:qui est le président de|qui est le president de|président de)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("président $targetSubject")
                }
                lowerOriginal.startsWith("qui ") || lowerOriginal.startsWith("qui est ") -> {
                    intent = QuestionIntent.WHO
                    targetSubject = lowerOriginal.replace(Regex("^(?:qui est|qui)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.contains("plus grand") || lowerOriginal.contains("plus haut") || lowerOriginal.contains("plus long") -> {
                    intent = QuestionIntent.SUPERLATIVE
                    targetSubject = lowerOriginal
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("où ") || lowerOriginal.contains("se trouve") -> {
                    intent = QuestionIntent.WHERE
                    targetSubject = lowerOriginal.replace(Regex("^(?:où se trouve|où est|ou se trouve|ou est|où|ou)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("quand ") || lowerOriginal.contains("fondée") -> {
                    intent = QuestionIntent.WHEN
                    targetSubject = lowerOriginal.replace(Regex("^(?:quand a été fondée|quand a ete fondee|quand)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("combien de ") -> {
                    intent = QuestionIntent.HOW_MANY
                    targetSubject = lowerOriginal.replace("combien de", "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                else -> {
                    intent = QuestionIntent.WHAT
                    targetSubject = lowerOriginal.replace(Regex("^(?:qu'est-ce que|quel est|quelle est)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
            }
        } else {
            // English
            when {
                lowerOriginal.contains("capital of") -> {
                    intent = QuestionIntent.CAPITAL
                    targetSubject = lowerOriginal.replace(Regex("^(?:what is the capital of|capital of)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("capital of $targetSubject")
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.contains("first") && (lowerOriginal.contains("moon") || lowerOriginal.contains("walked on")) -> {
                    intent = QuestionIntent.FIRST_PERSON
                    targetSubject = "first person to walk on the moon"
                    searchTerms.add("first person on moon Neil Armstrong")
                }
                lowerOriginal.contains("president of") || lowerOriginal.contains("prime minister of") || lowerOriginal.contains("leader of") -> {
                    intent = QuestionIntent.LEADER
                    targetSubject = lowerOriginal.replace(Regex("^(?:who is the president of|who is president of|president of)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("current president of $targetSubject")
                }
                lowerOriginal.startsWith("who ") -> {
                    intent = QuestionIntent.WHO
                    targetSubject = lowerOriginal.replace(Regex("^(?:who is|who was|who)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.contains("largest") || lowerOriginal.contains("highest") || lowerOriginal.contains("tallest") || lowerOriginal.contains("longest") || lowerOriginal.contains("deepest") -> {
                    intent = QuestionIntent.SUPERLATIVE
                    targetSubject = lowerOriginal
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("where ") || lowerOriginal.contains("located") -> {
                    intent = QuestionIntent.WHERE
                    targetSubject = lowerOriginal.replace(Regex("^(?:where is|where was|where does|where)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("location $targetSubject")
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("when ") || lowerOriginal.contains("founded") || lowerOriginal.contains("born") -> {
                    intent = QuestionIntent.WHEN
                    targetSubject = lowerOriginal.replace(Regex("^(?:when was|when did|when)\\s+"), "").replace("?", "").trim()
                    searchTerms.add("date founded $targetSubject")
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("how many ") -> {
                    intent = QuestionIntent.HOW_MANY
                    targetSubject = lowerOriginal.replace("how many", "").replace("?", "").trim()
                    searchTerms.add("number of $targetSubject")
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("how much ") -> {
                    intent = QuestionIntent.HOW_MUCH
                    targetSubject = lowerOriginal.replace("how much", "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.startsWith("is ") || lowerOriginal.startsWith("are ") || lowerOriginal.startsWith("did ") || lowerOriginal.startsWith("does ") || lowerOriginal.startsWith("can ") -> {
                    intent = QuestionIntent.YES_NO
                    targetSubject = lowerOriginal
                    searchTerms.add(targetSubject)
                }
                lowerOriginal.contains("difference between") || lowerOriginal.startsWith("compare ") -> {
                    intent = QuestionIntent.COMPARISON
                    targetSubject = lowerOriginal.replace("what is the difference between", "").replace("difference between", "").trim()
                    searchTerms.add(targetSubject)
                }
                else -> {
                    intent = QuestionIntent.WHAT
                    targetSubject = lowerOriginal.replace(Regex("^(?:what is|what are|what)\\s+"), "").replace("?", "").trim()
                    searchTerms.add(targetSubject)
                }
            }
        }

        // Determine Domain Category for routing
        val category = when {
            normalized.contains("اعراض") || normalized.contains("مرض") || normalized.contains("علاج") ||
                    normalized.contains("دواء") || normalized.contains("صحه") || lowerOriginal.contains("symptom") ||
                    lowerOriginal.contains("disease") || lowerOriginal.contains("treatment") ->
                QuestionCategory.HEALTH_MEDICAL

            normalized.contains("sdk") || normalized.contains("android") || normalized.contains("برمجه") ||
                    normalized.contains("كود") || normalized.contains("api") || normalized.contains("تطبيق") ||
                    lowerOriginal.contains("sdk") || lowerOriginal.contains("android") || lowerOriginal.contains("kotlin") ||
                    lowerOriginal.contains("code") || lowerOriginal.contains("developer") ->
                QuestionCategory.TECH_DEVELOPER

            normalized.contains("شركه") || normalized.contains("سهم") || normalized.contains("ارباح") ||
                    lowerOriginal.contains("company") || lowerOriginal.contains("stock") || lowerOriginal.contains("earnings") ->
                QuestionCategory.CORPORATE_BUSINESS

            intent == QuestionIntent.CAPITAL || intent == QuestionIntent.WHERE ||
                    normalized.contains("دوله") || normalized.contains("مملكه") || normalized.contains("عاصمه") ||
                    lowerOriginal.contains("capital") || lowerOriginal.contains("country") ->
                QuestionCategory.GEOGRAPHY_NATIONS

            normalized.contains("اخبار") || normalized.contains("اخر") || normalized.contains("اليوم") ||
                    intent == QuestionIntent.LEADER || lowerOriginal.contains("news") || lowerOriginal.contains("latest") ->
                QuestionCategory.CURRENT_NEWS

            else -> QuestionCategory.GENERAL_KNOWLEDGE
        }

        val isTimeSensitive = category == QuestionCategory.CURRENT_NEWS ||
                intent == QuestionIntent.LEADER ||
                normalized.contains("سعر") || normalized.contains("طقس") ||
                normalized.contains("نتيجه") || normalized.contains("مباراه") ||
                lowerOriginal.contains("price") || lowerOriginal.contains("weather") || lowerOriginal.contains("match")

        val keyTokens = tokenize(query, lang)

        return QuestionAnalysis(
            originalQuery = query,
            normalizedQuery = normalized,
            language = lang,
            intent = intent,
            category = category,
            isTimeSensitive = isTimeSensitive,
            keyTokens = keyTokens,
            targetSubject = targetSubject,
            searchTerms = searchTerms.distinct().ifEmpty { listOf(query) }
        )
    }
}
