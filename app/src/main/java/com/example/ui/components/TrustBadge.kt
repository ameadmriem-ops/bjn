package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrustTier
import com.example.ui.theme.TrustAcademicPurple
import com.example.ui.theme.TrustNoticeBlue
import com.example.ui.theme.TrustOfficialGold
import com.example.ui.theme.TrustVerifiedGreen

@Composable
fun TrustBadge(
    tier: TrustTier,
    language: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (tier) {
        TrustTier.GOVERNMENT_OFFICIAL -> Triple(
            TrustOfficialGold.copy(alpha = 0.15f),
            TrustOfficialGold,
            Icons.Default.AccountBalance
        )
        TrustTier.INTERNATIONAL_ORG -> Triple(
            TrustNoticeBlue.copy(alpha = 0.15f),
            TrustNoticeBlue,
            Icons.Default.Public
        )
        TrustTier.ACADEMIC_SCIENTIFIC -> Triple(
            TrustAcademicPurple.copy(alpha = 0.15f),
            TrustAcademicPurple,
            Icons.Default.School
        )
        TrustTier.OFFICIAL_MEDICAL -> Triple(
            TrustVerifiedGreen.copy(alpha = 0.15f),
            TrustVerifiedGreen,
            Icons.Default.LocalHospital
        )
        TrustTier.OFFICIAL_TECH -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Code
        )
        TrustTier.OFFICIAL_CORPORATE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Business
        )
        TrustTier.REPUTABLE_NEWS -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.Newspaper
        )
        TrustTier.GENERAL_REFERENCE -> Triple(
            TrustVerifiedGreen.copy(alpha = 0.15f),
            TrustVerifiedGreen,
            Icons.Default.MenuBook
        )
        TrustTier.WIKIPEDIA_AUXILIARY -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.MenuBook
        )
        TrustTier.GENERAL_WEB -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Verified
        )
        TrustTier.UNVERIFIED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            Icons.Default.Public
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = tier.getLabel(language),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
