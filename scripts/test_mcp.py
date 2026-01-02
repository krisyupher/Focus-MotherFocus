"""Simple test to verify MCP server works"""
import asyncio
from mcp import ClientSession
from mcp.client.stdio import stdio_client

async def test_mcp_server():
    """Test the MCP server directly."""
    print("Starting MCP server test...")

    server_params = {
        "command": "python",
        "args": ["mcp_server.py"],
        "cwd": "."
    }

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            print("✓ Connected to MCP server")

            # Initialize
            await session.initialize()
            print("✓ Session initialized")

            # List tools
            tools_result = await session.list_tools()
            print(f"✓ Found {len(tools_result.tools)} tools:")
            for tool in tools_result.tools:
                print(f"  - {tool.name}: {tool.description[:50]}...")

            # Test add_target
            print("\nTesting add_target...")
            result = await session.call_tool("add_target", {"name": "Netflix"})
            print(f"Result: {result.content[0].text}")

            # Test list_targets
            print("\nTesting list_targets...")
            result = await session.call_tool("list_targets", {})
            print(f"Result: {result.content[0].text}")

            # Test get_status
            print("\nTesting get_status...")
            result = await session.call_tool("get_status", {})
            print(f"Result: {result.content[0].text}")

            print("\n✓ All tests passed!")

if __name__ == "__main__":
    asyncio.run(test_mcp_server())
