"""
OpenAI Multi-MCP Client - Connects OpenAI to multiple MCP servers.

This client allows you to use OpenAI's API with multiple MCP servers simultaneously
(Playwright for browser automation + Filesystem for file operations).
"""
import asyncio
import os
import json
from typing import Optional, Dict, List
from openai import AsyncOpenAI
from mcp import ClientSession
from mcp.client.stdio import stdio_client
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()


class OpenAIMultiMCPClient:
    """OpenAI client with multiple MCP server integrations."""

    def __init__(self, api_key: Optional[str] = None, config_file: str = "mcp_client_config.json"):
        """
        Initialize OpenAI Multi-MCP client.

        Args:
            api_key: OpenAI API key (or set OPENAI_API_KEY environment variable)
            config_file: Path to MCP configuration file
        """
        self.api_key = api_key or os.getenv("OPENAI_API_KEY")
        if not self.api_key:
            raise ValueError("OpenAI API key required. Set OPENAI_API_KEY or pass api_key parameter.")

        self.client = AsyncOpenAI(api_key=self.api_key)
        self.config_file = config_file
        self.mcp_sessions: Dict[str, ClientSession] = {}
        self.available_tools = []

    def load_config(self) -> Dict:
        """Load MCP server configuration from JSON file."""
        with open(self.config_file, 'r') as f:
            return json.load(f)

    async def connect_all_servers(self):
        """Connect to all MCP servers defined in config."""
        config = self.load_config()
        servers = config.get("mcpServers", {})

        print(f"[MCP Client] Found {len(servers)} MCP servers in config")

        for server_name, server_config in servers.items():
            await self.connect_server(server_name, server_config)

    async def connect_server(self, server_name: str, server_config: Dict):
        """
        Connect to a single MCP server.

        Args:
            server_name: Name of the server (e.g., "playwright", "filesystem")
            server_config: Server configuration with command and args
        """
        print(f"[MCP Client] Connecting to {server_name}...")

        try:
            # Connect via stdio
            async with stdio_client(server_config) as (read, write):
                async with ClientSession(read, write) as session:
                    # Store session
                    self.mcp_sessions[server_name] = session

                    # Initialize session
                    await session.initialize()
                    print(f"[MCP Client] ✓ Connected to {server_name}")

                    # List available tools
                    tools_result = await session.list_tools()
                    server_tools = tools_result.tools

                    # Add server name prefix to tool names for clarity
                    for tool in server_tools:
                        tool.server = server_name  # Track which server owns this tool
                        self.available_tools.append(tool)

                    print(f"[MCP Client] Loaded {len(server_tools)} tools from {server_name}:")
                    for tool in server_tools:
                        print(f"  • {tool.name}: {tool.description}")

        except Exception as e:
            print(f"[MCP Client] ✗ Failed to connect to {server_name}: {e}")

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

    def get_tool_server(self, tool_name: str) -> Optional[str]:
        """Get the server name that owns a specific tool."""
        for tool in self.available_tools:
            if tool.name == tool_name:
                return getattr(tool, 'server', None)
        return None

    async def call_tool(self, tool_name: str, tool_args: Dict) -> str:
        """
        Call a tool on the appropriate MCP server.

        Args:
            tool_name: Name of the tool to call
            tool_args: Arguments for the tool

        Returns:
            Tool result as string
        """
        # Find which server owns this tool
        server_name = self.get_tool_server(tool_name)
        if not server_name:
            return f"Error: Unknown tool {tool_name}"

        session = self.mcp_sessions.get(server_name)
        if not session:
            return f"Error: Server {server_name} not connected"

        # Call the tool
        result = await session.call_tool(tool_name, tool_args)

        # Extract text from result
        result_text = ""
        for content in result.content:
            if hasattr(content, 'text'):
                result_text += content.text

        return result_text

    async def chat(self, user_message: str, conversation_history: list = None):
        """
        Send a message to OpenAI with MCP tool support.

        Args:
            user_message: User's message
            conversation_history: Previous conversation messages

        Returns:
            Assistant's response and updated conversation history
        """
        if not self.mcp_sessions:
            raise RuntimeError("No MCP servers connected. Call connect_all_servers() first.")

        # Initialize conversation
        if conversation_history is None:
            conversation_history = []

        # Build system message with available capabilities
        server_list = ", ".join(self.mcp_sessions.keys())
        system_content = (
            f"You are a helpful assistant with access to multiple tools from MCP servers: {server_list}.\n\n"
            "Available capabilities:\n"
            "- Browser automation via Playwright (navigate, click, screenshot, etc.)\n"
            "- File system operations (read, write, list files, search, etc.)\n\n"
            "Help the user with their requests using these tools."
        )

        # Add system message
        messages = [{"role": "system", "content": system_content}]
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

            # Execute tool calls via appropriate MCP servers
            for tool_call in assistant_message.tool_calls:
                import json as json_lib
                tool_name = tool_call.function.name
                tool_args = json_lib.loads(tool_call.function.arguments)

                print(f"  Calling {tool_name} with {tool_args}")

                # Call tool on appropriate server
                result_text = await self.call_tool(tool_name, tool_args)

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
    """Run an interactive chat session with OpenAI + Multiple MCP servers."""
    print("=" * 70)
    print("OpenAI + Multi-MCP Integration")
    print("Playwright (Browser) + Filesystem")
    print("=" * 70)
    print("\nStarting interactive session...")
    print("Type 'quit' to exit\n")

    # Initialize client
    client = OpenAIMultiMCPClient()

    # Connect to all MCP servers
    await client.connect_all_servers()

    print("\n" + "=" * 70)
    print("Connected! You can now use browser automation and file operations.")
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
    """Example demonstrating OpenAI + Multi-MCP integration."""
    print("=" * 70)
    print("OpenAI + Multi-MCP - Example Usage")
    print("=" * 70 + "\n")

    client = OpenAIMultiMCPClient()
    await client.connect_all_servers()

    print("\nExample 1: Browser automation")
    await client.chat("Open google.com and take a screenshot")

    print("\nExample 2: File operations")
    await client.chat("List all Python files in the current directory")

    print("\nExample 3: Combined operations")
    await client.chat("Take a screenshot and save the file path to a text file")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "example":
        # Run example
        asyncio.run(example_usage())
    else:
        # Run interactive session
        asyncio.run(interactive_session())
