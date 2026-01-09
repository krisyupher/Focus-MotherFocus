"""
OpenAI MCP Client - Connects OpenAI to Playwright via MCP.

This client allows you to use OpenAI's API with MCP tool calling to control
browser automation via Playwright.
"""
import asyncio
import os
from typing import Optional
from openai import AsyncOpenAI
from mcp import ClientSession
from mcp.client.stdio import stdio_client
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()


class OpenAIMCPClient:
    """OpenAI client with MCP integration for Playwright."""

    def __init__(self, api_key: Optional[str] = None):
        """
        Initialize OpenAI MCP client.

        Args:
            api_key: OpenAI API key (or set OPENAI_API_KEY environment variable)
        """
        self.api_key = api_key or os.getenv("OPENAI_API_KEY")
        if not self.api_key:
            raise ValueError("OpenAI API key required. Set OPENAI_API_KEY or pass api_key parameter.")

        self.client = AsyncOpenAI(api_key=self.api_key)
        self.mcp_session: Optional[ClientSession] = None
        self.available_tools = []

    async def connect_mcp_server(self):
        """Connect to the Playwright MCP server."""
        print("[MCP Client] Connecting to Playwright MCP server...")

        # Start Playwright MCP server via npx
        server_params = {
            "command": "npx",
            "args": ["@playwright/mcp@latest"]
        }

        # Connect via stdio
        async with stdio_client(server_params) as (read, write):
            async with ClientSession(read, write) as session:
                self.mcp_session = session

                # Initialize session
                await session.initialize()
                print("[MCP Client] Connected to Playwright MCP server")

                # List available tools
                tools_result = await session.list_tools()
                self.available_tools = tools_result.tools
                print(f"[MCP Client] Loaded {len(self.available_tools)} tools:")
                for tool in self.available_tools:
                    print(f"  • {tool.name}: {tool.description}")

                return session

    def convert_mcp_tools_to_openai(self):
        """Convert MCP tools to OpenAI function calling format."""
        openai_tools = []

        for tool in self.available_tools:
            openai_tool = {
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description,
                    "parameters": tool.inputSchema
                }
            }
            openai_tools.append(openai_tool)

        return openai_tools

    async def chat(self, user_message: str, conversation_history: list = None):
        """
        Send a message to OpenAI with MCP tool support.

        Args:
            user_message: User's message
            conversation_history: Previous conversation messages

        Returns:
            Assistant's response
        """
        if not self.mcp_session:
            raise RuntimeError("MCP server not connected. Call connect_mcp_server() first.")

        # Initialize conversation
        if conversation_history is None:
            conversation_history = []

        # Add system message
        messages = [
            {
                "role": "system",
                "content": (
                    "You are a helpful assistant that can control web browsers using Playwright. "
                    "You have access to browser automation tools to navigate websites, "
                    "interact with elements, take screenshots, and extract information."
                )
            }
        ]
        messages.extend(conversation_history)
        messages.append({"role": "user", "content": user_message})

        # Convert MCP tools to OpenAI format
        openai_tools = self.convert_mcp_tools_to_openai()

        # Call OpenAI with tools
        print(f"\n[User] {user_message}")

        response = await self.client.chat.completions.create(
            model="gpt-4o-mini",
            messages=messages,
            tools=openai_tools,
            tool_choice="auto"
        )

        assistant_message = response.choices[0].message

        # Check if tool calls were made
        if assistant_message.tool_calls:
            print(f"[Assistant] Using tools: {[tc.function.name for tc in assistant_message.tool_calls]}")

            # Add assistant message to history
            messages.append({
                "role": "assistant",
                "content": assistant_message.content,
                "tool_calls": [
                    {
                        "id": tc.id,
                        "type": "function",
                        "function": {
                            "name": tc.function.name,
                            "arguments": tc.function.arguments
                        }
                    }
                    for tc in assistant_message.tool_calls
                ]
            })

            # Execute tool calls via MCP
            for tool_call in assistant_message.tool_calls:
                import json
                tool_name = tool_call.function.name
                tool_args = json.loads(tool_call.function.arguments)

                print(f"  Calling {tool_name} with {tool_args}")

                # Call MCP tool
                result = await self.mcp_session.call_tool(tool_name, tool_args)

                # Extract text from result
                result_text = ""
                for content in result.content:
                    if hasattr(content, 'text'):
                        result_text += content.text

                print(f"  Result: {result_text[:100]}...")

                # Add tool result to messages
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": result_text
                })

            # Get final response after tool execution
            final_response = await self.client.chat.completions.create(
                model="gpt-4o-mini",
                messages=messages
            )

            final_message = final_response.choices[0].message.content
            print(f"[Assistant] {final_message}\n")
            return final_message, messages

        else:
            # No tool calls, return response directly
            response_text = assistant_message.content
            print(f"[Assistant] {response_text}\n")
            messages.append({"role": "assistant", "content": response_text})
            return response_text, messages


async def interactive_session():
    """Run an interactive chat session with OpenAI + Playwright MCP."""
    print("=" * 70)
    print("OpenAI + Playwright MCP Integration")
    print("=" * 70)
    print("\nStarting interactive session...")
    print("Type 'quit' to exit\n")

    # Initialize client
    client = OpenAIMCPClient()

    # Connect to MCP server
    await client.connect_mcp_server()

    print("\n" + "=" * 70)
    print("Connected! You can now chat with OpenAI to control Playwright.")
    print("=" * 70 + "\n")

    # Conversation loop
    conversation = []

    while True:
        try:
            user_input = input("You: ").strip()

            if not user_input:
                continue

            if user_input.lower() in ['quit', 'exit', 'bye']:
                print("\nGoodbye!")
                break

            # Chat with OpenAI
            response, conversation = await client.chat(user_input, conversation)

        except KeyboardInterrupt:
            print("\n\nGoodbye!")
            break
        except Exception as e:
            print(f"\nError: {e}")
            import traceback
            traceback.print_exc()


async def example_usage():
    """Example demonstrating OpenAI + Playwright MCP integration."""
    print("=" * 70)
    print("OpenAI + Playwright MCP - Example Usage")
    print("=" * 70 + "\n")

    client = OpenAIMCPClient()
    await client.connect_mcp_server()

    print("\nExample 1: Navigate to a website")
    await client.chat("Open google.com in the browser")

    print("\nExample 2: Take a screenshot")
    await client.chat("Take a screenshot of the current page")

    print("\nExample 3: Extract information")
    await client.chat("What's the title of the page?")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "example":
        # Run example
        asyncio.run(example_usage())
    else:
        # Run interactive session
        asyncio.run(interactive_session())
