package com.focusmother.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusmother.android.data.entity.Agreement

/**
 * Displays a card showing all active time agreements.
 *
 * Each agreement shows:
 * - App name or "Phone Usage" for general agreements
 * - App category
 * - Countdown timer showing time remaining
 * - Icon indicating app type
 *
 * The card uses Material Design 3 styling and matches the app theme.
 * Real-time countdown updates are handled by the CountdownTimer component.
 *
 * @param agreements List of active agreements to display
 * @param modifier Optional modifier for styling
 */
@Composable
fun ActiveAgreementsCard(
    agreements: List<Agreement>,
    modifier: Modifier = Modifier
) {
    if (agreements.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active Agreements",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Agreements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // List of agreements
            agreements.forEachIndexed { index, agreement ->
                AgreementItem(agreement = agreement)

                // Add spacing between items (but not after the last one)
                if (index < agreements.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Displays a single agreement item within the card.
 *
 * @param agreement The agreement to display
 */
@Composable
private fun AgreementItem(
    agreement: Agreement
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: App info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon
            Icon(
                imageVector = if (agreement.appPackageName == null) {
                    Icons.Default.Phone
                } else {
                    Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )

            // App name and category
            Column {
                Text(
                    text = agreement.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCategory(agreement.appCategory),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Right side: Countdown timer
        CountdownTimer(
            expiresAt = agreement.expiresAt,
            label = "Time left"
        )
    }
}

/**
 * Formats app category for display.
 *
 * Converts categories like "SOCIAL_MEDIA" to "Social Media".
 *
 * @param category Raw category string
 * @return Formatted category string
 */
private fun formatCategory(category: String): String {
    return category
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
