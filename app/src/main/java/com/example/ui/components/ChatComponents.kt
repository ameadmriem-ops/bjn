package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnswerStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.SourceInfo
import com.example.data.model.SynthesizedAnswer
import com.example.ui.theme.DarkUserBubble
import com.example.ui.theme.TrustConflictAmber
import com.example.ui.theme.TrustVerifiedGreen
import com.example.viewmodel.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("user_message_bubble")
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: ChatMessage,
    language: String,
    modifier: Modifier = Modifier
) {
    var sourcesExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 18.dp
                    )
                )
                .padding(14.dp)
                .testTag("assistant_message_bubble")
        ) {
            Column {
                // Header: Avatar, Name & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trusted Search",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }

                    if (!message.isSearching && message.answer != null) {
                        val status = message.answer.status
                        val (chipBg, chipFg, chipText) = when {
                            status == AnswerStatus.CONFLICT -> Triple(
                                TrustConflictAmber.copy(alpha = 0.15f),
                                TrustConflictAmber,
                                if (language == "ar") "⚠ اختلاف بين المصادر" else "⚠ Discrepancy"
                            )
                            status == AnswerStatus.INSUFFICIENT_DATA -> Triple(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.outline,
                                if (language == "ar") "! مصادر غير كافية" else "! Insufficient Sources"
                            )
                            else -> Triple(
                                TrustVerifiedGreen.copy(alpha = 0.15f),
                                TrustVerifiedGreen,
                                if (language == "ar") "✓ موثوق" else "✓ Verified"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = chipText,
                                color = chipFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Searching state: Minimal subtle pulse with ZERO distracting text
                if (message.isSearching) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Subtle 3 animated dots
                        val transition = rememberInfiniteTransition(label = "dots")
                        val alpha1 by transition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1"
                        )
                        val alpha2 by transition.animateFloat(
                            initialValue = 0.5f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "d2"
                        )
                        val alpha3 by transition.animateFloat(
                            initialValue = 0.2f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "d3"
                        )
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1)))
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2)))
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha3)))
                    }
                } else {
                    // Direct Answer Text (Short and Punchy)
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 23.sp
                    )

                    val answer = message.answer
                    if (answer != null && answer.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Conflict breakdown if present
                        if (answer.status == AnswerStatus.CONFLICT && answer.conflictViews.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TrustConflictAmber.copy(alpha = 0.08f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                answer.conflictViews.forEach { view ->
                                    Text(
                                        text = "• ${view.claim} (${view.sourceName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Compact Sources Summary matching user request
                        val sourceNamesSummary = answer.sources.take(3).joinToString(" · ") {
                            it.domain.replace(".org", "").replace(".com", "").replace("www.", "").capitalize(Locale.ROOT)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TrustVerifiedGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "ar") "موثوق" else "Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TrustVerifiedGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "·  ${if (language == "ar") "المصادر الموثوقة" else "Trusted Sources"} · ${answer.sources.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                OutlinedButton(
                                    onClick = { sourcesExpanded = !sourcesExpanded },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (sourcesExpanded) {
                                            if (language == "ar") "إخفاء المصادر" else "Hide Sources"
                                        } else {
                                            if (language == "ar") "عرض المصادر" else "Show Sources"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = sourceNamesSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Expanded Source Details
                        AnimatedVisibility(
                            visible = sourcesExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                answer.sources.forEach { source ->
                                    SourceListItem(source = source, language = language)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceListItem(
    source: SourceInfo,
    language: String
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = source.domain,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TrustBadge(tier = source.trustTier, language = language)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = source.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (source.snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = source.snippet,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "ar") "فتح المصدر" else "Open Source",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatWelcomeCenter(
    language: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = when (language) {
        "ar" -> listOf(
            "ما عاصمة المغرب؟",
            "ما أكبر محيط في العالم؟",
            "من أول إنسان وصل إلى القمر؟",
            "ما أعلى قمة جبلية في العالم؟"
        )
        "fr" -> listOf(
            "Quelle est la capitale du Maroc ?",
            "Quel est le plus grand océan du monde ?",
            "Qui est le premier homme sur la Lune ?"
        )
        else -> listOf(
            "What is the capital of Morocco?",
            "What is the largest ocean in the world?",
            "Who was the first person on the Moon?"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Minimal emblem
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (language == "ar") "مرحبًا بك في Trusted Search" else "Welcome to Trusted Search",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (language == "ar")
                "ابحث في مصادر موثوقة واحصل على إجابات موثقة بدون أي ذكاء اصطناعي."
            else
                "Search trusted sources and get verified answers with zero AI hallucinations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Suggestions Pills
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { prompt ->
                SuggestionChip(
                    onClick = { onSuggestionClick(prompt) },
                    label = { Text(prompt, fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBottomComposer(
    inputQuery: String,
    language: String,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = inputQuery.isNotBlank() && !isSearching

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pill container for the entire composer
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "+" Button to start fresh chat
            IconButton(
                onClick = onNewChatClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("composer_new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Text input
            BasicTextField(
                value = inputQuery,
                onValueChange = onQueryChanged,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("chat_input_field"),
                decorationBox = { innerTextField ->
                    if (inputQuery.isEmpty()) {
                        Text(
                            text = when (language) {
                                "ar" -> "اكتب سؤالك هنا..."
                                "fr" -> "Posez votre question..."
                                else -> "Ask a question..."
                            },
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )

            // Voice Search Microphone Button
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("composer_voice_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = canSend) { onSend(inputQuery) }
                    .testTag("composer_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
