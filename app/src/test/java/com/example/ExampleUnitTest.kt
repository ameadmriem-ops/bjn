package com.example

import com.example.data.model.AnswerStatus
import com.example.data.model.CandidateSentence
import com.example.data.model.SourceInfo
import com.example.data.model.TrustTier
import com.example.nlp.ConsensusEngine
import com.example.nlp.QuestionIntent
import com.example.nlp.SentenceRanker
import com.example.nlp.TextPreprocessor
import com.example.provider.DomainTrustFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLanguageDetection() {
        assertEquals("ar", TextPreprocessor.detectLanguage("ما عاصمة المغرب؟"))
        assertEquals("ar", TextPreprocessor.detectLanguage("ما هو أكبر محيط في العالم؟"))
        assertEquals("en", TextPreprocessor.detectLanguage("What is the capital of France?"))
        assertEquals("fr", TextPreprocessor.detectLanguage("Quelle est la capitale du Japon?"))
    }

    @Test
    fun testArabicNormalization() {
        val raw = "مَا هِيَ عَاصِمَةُ الْمَغْرِبِ؟"
        val normalized = TextPreprocessor.normalizeArabic(raw)
        assertTrue(!normalized.contains("َ"))
        assertTrue(!normalized.contains("ِ"))
        assertTrue(!normalized.contains("ُ"))
        assertTrue(!normalized.contains("؟"))
    }

    @Test
    fun testQuestionAnalysisClassifier() {
        assertEquals(QuestionIntent.CAPITAL, TextPreprocessor.analyzeQuestion("ما عاصمة المغرب؟").intent)
        assertEquals(QuestionIntent.SUPERLATIVE, TextPreprocessor.analyzeQuestion("ما أكبر محيط في العالم؟").intent)
        assertEquals(QuestionIntent.SUPERLATIVE, TextPreprocessor.analyzeQuestion("ما أعلى جبل في العالم؟").intent)
        assertEquals(QuestionIntent.FIRST_PERSON, TextPreprocessor.analyzeQuestion("من أول إنسان مشى على القمر؟").intent)
        assertEquals(QuestionIntent.WHERE, TextPreprocessor.analyzeQuestion("أين تقع مراكش؟").intent)
        assertEquals(QuestionIntent.WHEN, TextPreprocessor.analyzeQuestion("متى تأسست الأمم المتحدة؟").intent)
        assertEquals(QuestionIntent.HOW_MANY, TextPreprocessor.analyzeQuestion("كم عدد القارات؟").intent)
        assertEquals(QuestionIntent.LEADER, TextPreprocessor.analyzeQuestion("من هو رئيس فرنسا؟").intent)
    }

    @Test
    fun testDomainTrustFilter() {
        assertEquals(TrustTier.GOVERNMENT_OFFICIAL, DomainTrustFilter.classifyTrust("https://www.whitehouse.gov/briefing/"))
        assertEquals(TrustTier.GOVERNMENT_OFFICIAL, DomainTrustFilter.classifyTrust("https://maroc.gov.ma/portal"))
        assertEquals(TrustTier.ACADEMIC_SCIENTIFIC, DomainTrustFilter.classifyTrust("https://harvard.edu/research"))
        assertEquals(TrustTier.GOVERNMENT_OFFICIAL, DomainTrustFilter.classifyTrust("https://www.nasa.gov/missions"))
        assertEquals(TrustTier.ACADEMIC_SCIENTIFIC, DomainTrustFilter.classifyTrust("https://nature.com/articles"))
        assertEquals(TrustTier.WIKIPEDIA_AUXILIARY, DomainTrustFilter.classifyTrust("https://ar.wikipedia.org/wiki/الرباط"))
        assertEquals(TrustTier.GENERAL_REFERENCE, DomainTrustFilter.classifyTrust("https://britannica.com/topic/Morocco"))
        assertEquals(TrustTier.UNVERIFIED, DomainTrustFilter.classifyTrust("https://www.quora.com/question/123"))
    }

    @Test
    fun testMandatoryQuestionsDeterministicAnswers() {
        // 1. Capital of Morocco
        val q1 = "ما عاصمة المغرب؟"
        val a1 = TextPreprocessor.analyzeQuestion(q1)
        val s1 = listOf(
            SourceInfo(title = "المغرب", url = "https://maroc.gov.ma", domain = "maroc.gov.ma", snippet = "الرباط هي عاصمة المملكة المغربية الرسمية.", trustTier = TrustTier.GOVERNMENT_OFFICIAL, sourceName = "حكومة المغرب", providerName = "Gov"),
            SourceInfo(title = "الرباط", url = "https://ar.wikipedia.org/wiki/الرباط", domain = "ar.wikipedia.org", snippet = "الرباط عاصمة المغرب وتقع على ساحل المحيط الأطلسي.", trustTier = TrustTier.WIKIPEDIA_AUXILIARY, sourceName = "Wikipedia", providerName = "Wiki")
        )
        val r1 = SentenceRanker.extractAndRankSentences(s1, a1)
        val ans1 = ConsensusEngine.synthesize(q1, s1, r1, a1, 50)
        assertEquals("عاصمة المغرب هي الرباط.", ans1.directAnswer)

        // 2. Largest Ocean
        val q2 = "ما أكبر محيط في العالم؟"
        val a2 = TextPreprocessor.analyzeQuestion(q2)
        val s2 = listOf(
            SourceInfo(title = "المحيط الهادئ", url = "https://noaa.gov/pacific", domain = "noaa.gov", snippet = "المحيط الهادئ هو أكبر مسطح مائي ومحيط على وجه الأرض.", trustTier = TrustTier.GOVERNMENT_OFFICIAL, sourceName = "NOAA", providerName = "Gov")
        )
        val r2 = SentenceRanker.extractAndRankSentences(s2, a2)
        val ans2 = ConsensusEngine.synthesize(q2, s2, r2, a2, 50)
        assertEquals("أكبر محيط في العالم هو المحيط الهادئ.", ans2.directAnswer)

        // 3. Highest Mountain
        val q3 = "ما أعلى جبل في العالم؟"
        val a3 = TextPreprocessor.analyzeQuestion(q3)
        val s3 = listOf(
            SourceInfo(title = "جبل إيفرست", url = "https://britannica.com/everest", domain = "britannica.com", snippet = "جبل إيفرست هو أعلى جبل في العالم بارتفاع 8,849 متراً.", trustTier = TrustTier.GENERAL_REFERENCE, sourceName = "Britannica", providerName = "Enc")
        )
        val r3 = SentenceRanker.extractAndRankSentences(s3, a3)
        val ans3 = ConsensusEngine.synthesize(q3, s3, r3, a3, 50)
        assertEquals("أعلى جبل في العالم هو جبل إيفرست، ويبلغ ارتفاعه حوالي 8,849 متراً.", ans3.directAnswer)

        // 4. First person on moon
        val q4 = "من أول إنسان مشى على القمر؟"
        val a4 = TextPreprocessor.analyzeQuestion(q4)
        val s4 = listOf(
            SourceInfo(title = "أبولو 11", url = "https://nasa.gov/apollo11", domain = "nasa.gov", snippet = "نيل أرمسترونغ رائد فضاء أمريكي وأول شخص يمشي على سطح القمر عام 1969.", trustTier = TrustTier.GOVERNMENT_OFFICIAL, sourceName = "NASA", providerName = "Gov")
        )
        val r4 = SentenceRanker.extractAndRankSentences(s4, a4)
        val ans4 = ConsensusEngine.synthesize(q4, s4, r4, a4, 50)
        assertEquals("أول إنسان مشى على القمر هو نيل أرمسترونغ، خلال مهمة Apollo 11 عام 1969.", ans4.directAnswer)

        // 5. Where is Marrakech
        val q5 = "أين تقع مراكش؟"
        val a5 = TextPreprocessor.analyzeQuestion(q5)
        val s5 = listOf(
            SourceInfo(title = "مراكش", url = "https://maroc.gov.ma/marrakech", domain = "maroc.gov.ma", snippet = "تقع مدينة مراكش في المملكة المغربية في الجنوب الأوسط.", trustTier = TrustTier.GOVERNMENT_OFFICIAL, sourceName = "Gov", providerName = "Gov")
        )
        val r5 = SentenceRanker.extractAndRankSentences(s5, a5)
        val ans5 = ConsensusEngine.synthesize(q5, s5, r5, a5, 50)
        assertEquals("تقع مراكش في المملكة المغربية (جنوب وسط المغرب).", ans5.directAnswer)

        // 6. When was UN founded
        val q6 = "متى تأسست الأمم المتحدة؟"
        val a6 = TextPreprocessor.analyzeQuestion(q6)
        val s6 = listOf(
            SourceInfo(title = "الأمم المتحدة", url = "https://un.org/history", domain = "un.org", snippet = "تأسست منظمة الأمم المتحدة رسمياً في 24 أكتوبر 1945.", trustTier = TrustTier.GOVERNMENT_OFFICIAL, sourceName = "UN", providerName = "Gov")
        )
        val r6 = SentenceRanker.extractAndRankSentences(s6, a6)
        val ans6 = ConsensusEngine.synthesize(q6, s6, r6, a6, 50)
        assertEquals("تأسست الأمم المتحدة في 24 أكتوبر 1945.", ans6.directAnswer)

        // 7. How many continents
        val q7 = "كم عدد القارات؟"
        val a7 = TextPreprocessor.analyzeQuestion(q7)
        val s7 = listOf(
            SourceInfo(title = "قارات العالم", url = "https://britannica.com/continents", domain = "britannica.com", snippet = "يوجد في كوكب الأرض 7 قارات رئيسية معترف بها.", trustTier = TrustTier.GENERAL_REFERENCE, sourceName = "Britannica", providerName = "Enc")
        )
        val r7 = SentenceRanker.extractAndRankSentences(s7, a7)
        val ans7 = ConsensusEngine.synthesize(q7, s7, r7, a7, 50)
        assertEquals("عدد قارات العالم 7 قارات.", ans7.directAnswer)
    }

    @Test
    fun testConflictDetection() {
        val query = "تاريخ الحدث"
        val analysis = TextPreprocessor.analyzeQuestion(query)

        val source1 = SourceInfo(
            title = "مصدر أ",
            url = "https://un.org",
            domain = "un.org",
            snippet = "تأسست المنظمة في عام 1945.",
            trustTier = TrustTier.GOVERNMENT_OFFICIAL,
            sourceName = "الأمم المتحدة",
            providerName = "UN Portal"
        )
        val source2 = SourceInfo(
            title = "مصدر ب",
            url = "https://academic.edu",
            domain = "academic.edu",
            snippet = "تأسست المنظمة عام 1950 بحسب وثائق أخرى.",
            trustTier = TrustTier.ACADEMIC_SCIENTIFIC,
            sourceName = "جامعة أكسفورد",
            providerName = "Academic Press"
        )

        val sentence1 = CandidateSentence(
            text = "تأسست المنظمة في عام 1945.",
            cleanSentence = "تاسست المنظمه في عام 1945",
            score = 3.5,
            sourceTitle = "مصدر أ",
            sourceUrl = "https://un.org",
            domain = "un.org",
            trustTier = TrustTier.GOVERNMENT_OFFICIAL,
            matchedKeywords = listOf("تأسست"),
            extractedEntity = "1945"
        )

        val sentence2 = CandidateSentence(
            text = "تأسست المنظمة عام 1950 بحسب وثائق أخرى.",
            cleanSentence = "تاسست المنظمه عام 1950 بحسب وثائق اخرى",
            score = 3.2,
            sourceTitle = "مصدر ب",
            sourceUrl = "https://academic.edu",
            domain = "academic.edu",
            trustTier = TrustTier.ACADEMIC_SCIENTIFIC,
            matchedKeywords = listOf("تأسست"),
            extractedEntity = "1950"
        )

        val answer = ConsensusEngine.synthesize(
            query = query,
            sources = listOf(source1, source2),
            rankedSentences = listOf(sentence1, sentence2),
            analysis = analysis,
            durationMs = 90
        )

        assertEquals(AnswerStatus.CONFLICT, answer.status)
        assertEquals("وجدت معلومات مختلفة بين المصادر.", answer.directAnswer)
        assertTrue(answer.conflictViews.isNotEmpty())
    }

    @Test
    fun testInsufficientData() {
        val query = "سؤال عشوائي بدون مصادر"
        val analysis = TextPreprocessor.analyzeQuestion(query)

        val answer = ConsensusEngine.synthesize(
            query = query,
            sources = emptyList(),
            rankedSentences = emptyList(),
            analysis = analysis,
            durationMs = 20
        )

        assertEquals(AnswerStatus.INSUFFICIENT_DATA, answer.status)
        assertEquals("لم أجد معلومات موثوقة كافية للإجابة عن هذا السؤال.", answer.directAnswer)
    }
}
