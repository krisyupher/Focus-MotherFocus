package com.focusmother.android.ui.conversation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focusmother.android.data.api.ClaudeApiService
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.repository.ConversationRepository
import com.focusmother.android.domain.ContextBuilder
import com.focusmother.android.domain.ConversationContext
import com.focusmother.android.domain.PromptBuilder
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.ui.avatar.Avatar3DView
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import com.focusmother.android.util.SecureApiKeyProvider
import kotlinx.coroutines.launch

/**
 * Full-screen activity for AI conversation with Zordon.
 *
 * Layout structure:
 * - Top (1/3): 3D avatar or static Zordon image
 * - Middle (1/2): LazyColumn with message history
 * - Bottom (1/6): Text input field + Send button
 *
 * Features:
 * - Real-time conversation with Claude AI
 * - Zordon personality and appearance
 * - Quick reply buttons
 * - Typing indicator
 * - Message history persistence
 * - Error handling and retry
 *
 * Intent extras:
 * - EXTRA_CURRENT_APP: String? (package name)
 * - EXTRA_INTERVENTION_REASON: String (why triggered)
 * - EXTRA_CONVERSATION_ID: Long (conversation ID, default 1L)
 */
class ConversationActivity : ComponentActivity() {

    private lateinit var viewModel: ConversationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get intent extras
        val currentApp = intent.getStringExtra(EXTRA_CURRENT_APP)
        val interventionReason = intent.getStringExtra(EXTRA_INTERVENTION_REASON) ?: "General usage intervention"
        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, 1L)

        // Initialize dependencies
        val database = FocusMotherDatabase.getDatabase(this)
        val usageMonitor = UsageMonitor(this)
        val categoryManager = com.focusmother.android.domain.CategoryManager(database.appCategoryDao())

        // Build context
        val contextBuilder = ContextBuilder(
            usageMonitor = usageMonitor,
            categoryManager = categoryManager,
            agreementDao = database.agreementDao()
        )

        // Build conversation context
        lateinit var context: ConversationContext
        Thread {
            context = kotlinx.coroutines.runBlocking {
                contextBuilder.buildContext(
                    currentApp = currentApp,
                    interventionReason = interventionReason
                )
            }

            // Initialize repository and ViewModel
            val repository = ConversationRepository(
                conversationDao = database.conversationDao(),
                claudeApiService = ClaudeApiService.create(),
                apiKeyProvider = SecureApiKeyProvider(this),
                promptBuilder = PromptBuilder()
            )

            viewModel = ConversationViewModel(
                repository = repository,
                conversationId = conversationId,
                context = context
            )

            // Set content on main thread
            runOnUiThread {
                setContent {
                    FocusMotherFocusTheme {
                        ConversationScreen(
                            viewModel = viewModel,
                            context = context,
                            onFinish = { finish() }
                        )
                    }
                }
            }
        }.start()
    }

    companion object {
        const val EXTRA_CURRENT_APP = "extra_current_app"
        const val EXTRA_INTERVENTION_REASON = "extra_intervention_reason"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    }
}

/**
 * Main conversation screen composable.
 *
 * @param viewModel ViewModel managing conversation state
 * @param context Conversation context for display
 * @param onFinish Callback when user finishes conversation
 */
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    context: ConversationContext,
    onFinish: () -> Unit
) {
    val messages by viewModel.messages.observeAsState(emptyList())
    val conversationState by viewModel.conversationState.observeAsState(ConversationState.Idle)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)), // Dark background
        containerColor = Color(0xFF0A0A1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top: Avatar (1/3 of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.33f)
                    .background(Color(0xFF0A0A1A)),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Get actual avatar ID from database/preferences
                // For now, show fallback
                Avatar3DView(
                    avatarId = "placeholder",
                    showFallback = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Middle: Messages (1/2 of screen)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f),
                state = listState
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }

                // Show typing indicator
                item {
                    if (conversationState is ConversationState.WaitingForResponse) {
                        TypingIndicator()
                    }
                }
            }

            // Quick replies (if appropriate)
            if (conversationState is ConversationState.ShowingResponse ||
                conversationState is ConversationState.Idle
            ) {
                QuickReplies(
                    onReplySelected = { reply ->
                        viewModel.selectQuickReply(reply)
                    }
                )
            }

            // Bottom: Input field (1/6 of screen)
            MessageInputField(
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                },
                enabled = conversationState !is ConversationState.WaitingForResponse,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.17f)
                    .imePadding()
            )
        }
    }
}

/**
 * Message input field with send button.
 *
 * @param onSendMessage Callback when user sends a message
 * @param enabled Whether input is enabled
 * @param modifier Optional modifier
 */
@Composable
fun MessageInputField(
    onSendMessage: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = {
                Text("Type your message...", color = Color(0xFF808080))
            },
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE0E0E0),
                unfocusedTextColor = Color(0xFFE0E0E0),
                disabledTextColor = Color(0xFF606060),
                focusedBorderColor = Color(0xFF7C0EDA),
                unfocusedBorderColor = Color(0xFF4A4A5E),
                focusedContainerColor = Color(0xFF1A1A2E),
                unfocusedContainerColor = Color(0xFF1A1A2E),
                disabledContainerColor = Color(0xFF1A1A2E)
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 3
        )

        IconButton(
            onClick = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                }
            },
            enabled = enabled && messageText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = if (enabled && messageText.isNotBlank()) {
                    Color(0xFF7C0EDA)
                } else {
                    Color(0xFF4A4A5E)
                }
            )
        }
    }
}
