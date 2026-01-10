package com.focusmother.android.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusmother.android.data.entity.ConversationMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays a chat message bubble.
 *
 * Features:
 * - User messages: Right-aligned, purple background
 * - Assistant messages: Left-aligned, dark background
 * - Timestamp display
 * - Material Design 3 styling
 * - Responsive layout
 *
 * @param message The conversation message to display
 * @param modifier Optional modifier for customization
 */
@Composable
fun MessageBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ConversationMessage.ROLE_USER

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUser) {
                            Color(0xFF7C0EDA) // Purple for user
                        } else {
                            Color(0xFF1A1A2E) // Dark for assistant
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color(0xFFE0E0E0), // Light gray text
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            }

            // Timestamp
            Text(
                text = formatTimestamp(message.timestamp),
                color = Color(0xFF808080), // Gray
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

/**
 * Formats timestamp for display.
 *
 * Shows time in HH:mm format for messages from today,
 * and includes date for older messages.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted time string
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()

    // Check if message is from today
    val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) ==
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)

    return if (isToday) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A1A)
@Composable
fun MessageBubblePreview_User() {
    val message = ConversationMessage.userMessage(
        conversationId = 1L,
        content = "I need 5 more minutes on Instagram to finish messaging my friend."
    )
    MessageBubble(message = message)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A1A)
@Composable
fun MessageBubblePreview_Assistant() {
    val message = ConversationMessage.assistantMessage(
        conversationId = 1L,
        content = "Young warrior, I sense you've been in the digital realm for quite some time. Tell me, what task are you hoping to accomplish?"
    )
    MessageBubble(message = message)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A1A)
@Composable
fun MessageBubblePreview_LongMessage() {
    val message = ConversationMessage.assistantMessage(
        conversationId = 1L,
        content = "I understand your need to connect with friends, ranger. A focused 5-minute session should serve your purpose well. Complete your message, then step away and restore your focus. Do we have an agreement?"
    )
    MessageBubble(message = message)
}
