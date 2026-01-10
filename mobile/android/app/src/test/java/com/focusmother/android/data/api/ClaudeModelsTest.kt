package com.focusmother.android.data.api

import com.focusmother.android.data.api.models.ClaudeContent
import com.focusmother.android.data.api.models.ClaudeMessage
import com.focusmother.android.data.api.models.ClaudeMessageRequest
import com.focusmother.android.data.api.models.ClaudeMessageResponse
import com.focusmother.android.data.api.models.ClaudeUsage
import com.focusmother.android.data.api.models.getTextContent
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Claude API data models.
 *
 * Tests serialization, deserialization, and helper functions for Claude API models.
 */
class ClaudeModelsTest {

    private val gson = Gson()

    @Test
    fun `test ClaudeMessageRequest serialization with default values`() {
        // Arrange
        val request = ClaudeMessageRequest(
            messages = listOf(
                ClaudeMessage(role = "user", content = "Hello, Claude!")
            )
        )

        // Act
        val json = gson.toJson(request)

        // Assert
        assertNotNull(json)
        assertTrue(json.contains("\"model\":\"claude-3-5-sonnet-20241022\""))
        assertTrue(json.contains("\"max_tokens\":1024"))
        assertTrue(json.contains("\"temperature\":0.7"))
        assertTrue(json.contains("\"stream\":false"))
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"content\":\"Hello, Claude!\""))
    }

    @Test
    fun `test ClaudeMessageRequest serialization with custom values`() {
        // Arrange
        val request = ClaudeMessageRequest(
            model = "claude-3-opus-20240229",
            max_tokens = 2048,
            messages = listOf(
                ClaudeMessage(role = "user", content = "Test message")
            ),
            system = "You are a helpful assistant.",
            temperature = 0.5,
            stream = false
        )

        // Act
        val json = gson.toJson(request)

        // Assert
        assertNotNull(json)
        assertTrue(json.contains("\"model\":\"claude-3-opus-20240229\""))
        assertTrue(json.contains("\"max_tokens\":2048"))
        assertTrue(json.contains("\"temperature\":0.5"))
        assertTrue(json.contains("\"system\":\"You are a helpful assistant.\""))
    }

    @Test
    fun `test ClaudeMessageRequest with conversation history`() {
        // Arrange
        val request = ClaudeMessageRequest(
            messages = listOf(
                ClaudeMessage(role = "user", content = "What is 2+2?"),
                ClaudeMessage(role = "assistant", content = "2+2 equals 4."),
                ClaudeMessage(role = "user", content = "What about 3+3?")
            )
        )

        // Act
        val json = gson.toJson(request)

        // Assert
        assertNotNull(json)
        assertTrue(json.contains("What is 2+2?"))
        assertTrue(json.contains("2+2 equals 4."))
        assertTrue(json.contains("What about 3+3?"))
    }

    @Test
    fun `test ClaudeMessageResponse deserialization`() {
        // Arrange
        val json = """
            {
                "id": "msg_01XYZ123",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Hello! How can I help you today?"
                    }
                ],
                "model": "claude-3-5-sonnet-20241022",
                "stop_reason": "end_turn",
                "usage": {
                    "input_tokens": 10,
                    "output_tokens": 25
                }
            }
        """.trimIndent()

        // Act
        val response = gson.fromJson(json, ClaudeMessageResponse::class.java)

        // Assert
        assertNotNull(response)
        assertEquals("msg_01XYZ123", response.id)
        assertEquals("message", response.type)
        assertEquals("assistant", response.role)
        assertEquals("claude-3-5-sonnet-20241022", response.model)
        assertEquals("end_turn", response.stop_reason)
        assertEquals(1, response.content.size)
        assertEquals("text", response.content[0].type)
        assertEquals("Hello! How can I help you today?", response.content[0].text)
        assertEquals(10, response.usage.input_tokens)
        assertEquals(25, response.usage.output_tokens)
    }

    @Test
    fun `test ClaudeMessageResponse with multiple content blocks`() {
        // Arrange
        val json = """
            {
                "id": "msg_456",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "First part."
                    },
                    {
                        "type": "text",
                        "text": "Second part."
                    }
                ],
                "model": "claude-3-5-sonnet-20241022",
                "stop_reason": "end_turn",
                "usage": {
                    "input_tokens": 5,
                    "output_tokens": 15
                }
            }
        """.trimIndent()

        // Act
        val response = gson.fromJson(json, ClaudeMessageResponse::class.java)

        // Assert
        assertEquals(2, response.content.size)
        assertEquals("First part.", response.content[0].text)
        assertEquals("Second part.", response.content[1].text)
    }

    @Test
    fun `test getTextContent extension function returns first text content`() {
        // Arrange
        val response = ClaudeMessageResponse(
            id = "msg_789",
            type = "message",
            role = "assistant",
            content = listOf(
                ClaudeContent(type = "text", text = "This is the answer."),
                ClaudeContent(type = "text", text = "This is additional text.")
            ),
            model = "claude-3-5-sonnet-20241022",
            stop_reason = "end_turn",
            usage = ClaudeUsage(input_tokens = 10, output_tokens = 20)
        )

        // Act
        val textContent = response.getTextContent()

        // Assert
        assertEquals("This is the answer.", textContent)
    }

    @Test
    fun `test getTextContent extension function returns empty string when no text content`() {
        // Arrange
        val response = ClaudeMessageResponse(
            id = "msg_999",
            type = "message",
            role = "assistant",
            content = emptyList(),
            model = "claude-3-5-sonnet-20241022",
            stop_reason = "end_turn",
            usage = ClaudeUsage(input_tokens = 5, output_tokens = 0)
        )

        // Act
        val textContent = response.getTextContent()

        // Assert
        assertEquals("", textContent)
    }

    @Test
    fun `test getTextContent extension function filters non-text content`() {
        // Arrange
        val response = ClaudeMessageResponse(
            id = "msg_abc",
            type = "message",
            role = "assistant",
            content = listOf(
                ClaudeContent(type = "image", text = "Should be ignored"),
                ClaudeContent(type = "text", text = "Correct answer")
            ),
            model = "claude-3-5-sonnet-20241022",
            stop_reason = "end_turn",
            usage = ClaudeUsage(input_tokens = 15, output_tokens = 10)
        )

        // Act
        val textContent = response.getTextContent()

        // Assert
        assertEquals("Correct answer", textContent)
    }

    @Test
    fun `test ClaudeMessage with user role`() {
        // Arrange
        val message = ClaudeMessage(role = "user", content = "User question")

        // Act
        val json = gson.toJson(message)

        // Assert
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"content\":\"User question\""))
    }

    @Test
    fun `test ClaudeMessage with assistant role`() {
        // Arrange
        val message = ClaudeMessage(role = "assistant", content = "Assistant response")

        // Act
        val json = gson.toJson(message)

        // Assert
        assertTrue(json.contains("\"role\":\"assistant\""))
        assertTrue(json.contains("\"content\":\"Assistant response\""))
    }

    @Test
    fun `test ClaudeUsage serialization and deserialization`() {
        // Arrange
        val usage = ClaudeUsage(input_tokens = 100, output_tokens = 200)

        // Act
        val json = gson.toJson(usage)
        val deserialized = gson.fromJson(json, ClaudeUsage::class.java)

        // Assert
        assertEquals(100, deserialized.input_tokens)
        assertEquals(200, deserialized.output_tokens)
    }

    @Test
    fun `test ClaudeMessageRequest without system message`() {
        // Arrange
        val request = ClaudeMessageRequest(
            messages = listOf(ClaudeMessage(role = "user", content = "Test"))
        )

        // Act
        val json = gson.toJson(request)

        // Assert
        // Gson omits null values by default in some configurations
        assertNotNull(json)
        assertTrue(json.contains("\"messages\""))
    }

    @Test
    fun `test ClaudeMessageResponse with max_tokens stop reason`() {
        // Arrange
        val json = """
            {
                "id": "msg_max",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Response cut off due to max tokens..."
                    }
                ],
                "model": "claude-3-5-sonnet-20241022",
                "stop_reason": "max_tokens",
                "usage": {
                    "input_tokens": 50,
                    "output_tokens": 1024
                }
            }
        """.trimIndent()

        // Act
        val response = gson.fromJson(json, ClaudeMessageResponse::class.java)

        // Assert
        assertEquals("max_tokens", response.stop_reason)
        assertEquals(1024, response.usage.output_tokens)
    }
}
