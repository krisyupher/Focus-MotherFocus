package com.focusmother.android.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a horizontal row of quick reply buttons.
 *
 * Features:
 * - Horizontal scrollable layout
 * - Purple buttons matching theme
 * - Pre-defined common responses
 * - Material Design 3 styling
 *
 * @param onReplySelected Callback when a quick reply is selected
 * @param modifier Optional modifier for customization
 */
@Composable
fun QuickReplies(
    onReplySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickReplies = listOf(
        "I agree",
        "5 more minutes",
        "10 minutes",
        "15 minutes",
        "Not now"
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(quickReplies) { reply ->
            QuickReplyButton(
                text = reply,
                onClick = { onReplySelected(reply) }
            )
        }
    }
}

/**
 * Individual quick reply button.
 *
 * @param text Button text
 * @param onClick Callback when button is clicked
 * @param modifier Optional modifier
 */
@Composable
private fun QuickReplyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7C0EDA), // Purple
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A1A)
@Composable
fun QuickRepliesPreview() {
    QuickReplies(
        onReplySelected = { }
    )
}
