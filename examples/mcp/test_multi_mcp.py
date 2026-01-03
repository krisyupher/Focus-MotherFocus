"""
Test script for Multi-MCP integration (Playwright + Filesystem).

This demonstrates how to use both browser automation and file operations
through OpenAI with natural language.
"""
import asyncio
from openai_multi_mcp_client import OpenAIMultiMCPClient


async def test_filesystem():
    """Test filesystem operations."""
    print("=" * 70)
    print("TEST 1: Filesystem Operations")
    print("=" * 70)

    client = OpenAIMultiMCPClient()
    await client.connect_all_servers()

    # List Python files
    print("\n📂 Listing Python files...")
    response, _ = await client.chat("List all Python files in the current directory")
    print(f"Result: {response}\n")


async def test_playwright():
    """Test browser automation."""
    print("=" * 70)
    print("TEST 2: Browser Automation")
    print("=" * 70)

    client = OpenAIMultiMCPClient()
    await client.connect_all_servers()

    # Navigate to site
    print("\n🌐 Opening website...")
    response, _ = await client.chat("Open example.com")
    print(f"Result: {response}\n")


async def test_combined():
    """Test combined filesystem + browser operations."""
    print("=" * 70)
    print("TEST 3: Combined Operations")
    print("=" * 70)

    client = OpenAIMultiMCPClient()
    await client.connect_all_servers()

    # Read README and open a site
    print("\n🔄 Combined operation...")
    response, _ = await client.chat(
        "Read the README.md file and tell me the project name, "
        "then open google.com in the browser"
    )
    print(f"Result: {response}\n")


async def test_all():
    """Run all tests."""
    print("\n" + "🧪 MULTI-MCP INTEGRATION TESTS 🧪".center(70))
    print("=" * 70)
    print("\nThis will test:")
    print("✓ Filesystem MCP server (file operations)")
    print("✓ Playwright MCP server (browser automation)")
    print("✓ Combined operations (filesystem + browser)")
    print("\n" + "=" * 70 + "\n")

    try:
        # Test filesystem
        await test_filesystem()

        # Test Playwright (only if Chrome is running with debug port)
        print("\n⚠️  Skipping browser test - start Chrome with debug port first")
        print("   Run: start_chrome_debug.bat\n")

        # Test combined
        await test_combined()

        print("=" * 70)
        print("✅ Tests completed!")
        print("=" * 70)

    except Exception as e:
        print(f"\n❌ Test failed: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(test_all())
