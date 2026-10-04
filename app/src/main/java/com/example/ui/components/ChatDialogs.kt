package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TrustOfficialGold
import com.example.ui.theme.TrustVerifiedGreen
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    language: String,
    isDarkMode: Boolean,
    maxSources: Int,
    strictTrustOnly: Boolean,
    customApiUrl: String,
    customApiKey: String,
    onLanguageChange: (String) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMaxSourcesChange: (Int) -> Unit,
    onStrictTrustChange: (Boolean) -> Unit,
    onCustomApiSave: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempUrl by remember(customApiUrl) { mutableStateOf(customApiUrl) }
    var tempKey by remember(customApiKey) { mutableStateOf(customApiKey) }
    var sliderVal by remember(maxSources) { mutableFloatStateOf(maxSources.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == "ar") "الإعدادات" else "Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Language selector
                Text(
                    text = if (language == "ar") "اللغة:" else "Language:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ar" to "العربية", "en" to "English", "fr" to "Français").forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { onLanguageChange(code) },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 2. Dark mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "ar") "الوضع الداكن (Dark Mode)" else "Dark Mode",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 3. Max sources slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == "ar") "عدد المصادر المفحوصة:" else "Sources to scan:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${sliderVal.roundToInt()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderVal,
                        onValueChange = {
                            sliderVal = it
                            onMaxSourcesChange(it.roundToInt())
                        },
                        valueRange = 3f..10f,
                        steps = 6
                    )
                }

                // 4. Strict trust filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == "ar") "مستوى موثوقية فائق" else "Strict Trust Level",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (language == "ar") "المواقع الحكومية والعلمية الكبرى فقط" else "Only Gov, Edu and Scientific Institutions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = strictTrustOnly, onCheckedChange = onStrictTrustChange)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 5. Custom API Proxy Endpoint
                Text(
                    text = if (language == "ar") "مزود بحث مخصص (اختياري):" else "Custom Search API (Optional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = {
                        tempUrl = it
                        onCustomApiSave(it, tempKey)
                    },
                    placeholder = { Text("https://my-search-endpoint.com/api", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = {
                        tempKey = it
                        onCustomApiSave(tempUrl, it)
                    },
                    placeholder = { Text("API Key", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 6. Data Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == "ar") "مسح الكاش" else "Clear Cache", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (language == "ar") "مسح السجل" else "Clear History", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "ar") "تم" else "Done")
            }
        }
    )
}

@Composable
fun SourcesInfoDialog(
    language: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == "ar") "المصادر المعتمدة ونظام الثقة" else "Verified Sources Architecture",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (language == "ar")
                        "يعتمد التطبيق على تصنيف هندسي للمصادر لضمان دقة الإجابات:"
                    else
                        "The app relies on a strict domain rating hierarchy:",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = TrustOfficialGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (language == "ar") "الأولوية الأولى (وزن 1.0 - 0.95):" else "Tier 1 (Weight 1.0 - 0.95):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TrustOfficialGold
                        )
                        Text(
                            text = if (language == "ar")
                                "المواقع الحكومية الرسمية (.gov, .gouv.fr, .gov.ma)، الجامعات والمؤسسات الأكاديمية (.edu, .ac.uk)، والمنظمات الدولية (NASA, WHO, UN, UNESCO, Nature)."
                            else
                                "Official Government portals, Universities, and International Scientific Organizations (NASA, WHO, UNESCO, Nature).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = TrustVerifiedGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (language == "ar") "الأولوية الثانية (وزن 0.85 - 0.75):" else "Tier 2 (Weight 0.85 - 0.75):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TrustVerifiedGreen
                        )
                        Text(
                            text = if (language == "ar")
                                "الموسوعات العالمية المحكمة (Wikipedia, Encyclopaedia Britannica, Wikidata) ووكالات الأنباء الدولية الرائدة."
                            else
                                "Peer-reviewed Encyclopedias (Wikipedia, Britannica, Wikidata) and reputable wire agencies.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (language == "ar") "المصادر المحجوبة (وزن 0.0):" else "Blocked Sources (Weight 0.0):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (language == "ar")
                                "المنتديات المفتوحة والشبكات الاجتماعية والمواقع غير الخاضعة للمراجعة التحريرية."
                            else
                                "Open social forums, user-generated boards, and unverified content farms.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "ar") "إغلاق" else "Close")
            }
        }
    )
}

@Composable
fun AboutAppDialog(
    language: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == "ar") "حول محرك Trusted Search" else "About Trusted Search Engine",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (language == "ar")
                        "Trusted Search هو محرك إجابات رقمي فائق الدقة، بُني خصيصاً لتقديم إجابات موثقة وحقائق دقيقة بدون استخدام أي نموذج ذكاء اصطناعي (0% LLM / 0% AI).\n\n" +
                                "✓ خوارزميات لغوية تقليدية (NLP: Tokenization, BM25 / TF-IDF).\n" +
                                "✓ تدقيق توافق المصادر المتعددة برمجياً.\n" +
                                "✓ في حال وجود تضارب، يعرض التطبيق الاختلاف بشفافية تامة.\n" +
                                "✓ روابط أصلية مباشرة تضمن لك التحقق الفوري من كل معلومة."
                    else
                        "Trusted Search is a verified answer engine designed to deliver accurate facts directly from trusted web sources with zero AI hallucinations (0% LLM).\n\n" +
                                "✓ Deterministic NLP (Tokenization, BM25 / TF-IDF Ranking).\n" +
                                "✓ Programmatic multi-source consensus cross-validation.\n" +
                                "✓ Transparent discrepancy highlighting if sources diverge.\n" +
                                "✓ Direct verified sources links.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "ar") "إغلاق" else "Close")
            }
        }
    )
}
