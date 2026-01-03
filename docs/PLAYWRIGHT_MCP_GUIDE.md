# Playwright MCP Integration Guide

## Overview

This guide explains how to use the **OpenAI MCP Client** with **Playwright** for browser automation controlled by natural language.

With this integration, you can use OpenAI's GPT-4o-mini to control web browsers through Playwright!

## What is MCP?

**Model Context Protocol (MCP)** is an open protocol that allows AI assistants to:
- Access external tools and data sources
- Execute actions in applications
- Maintain context across interactions

## What is Playwright?

**Playwright** is a browser automation framework that allows you to:
- Control web browsers (Chrome, Firefox, Safari)
- Navigate websites
- Click elements, fill forms
- Take screenshots
- Extract information from pages

## Architecture

```
┌─────────────┐
│   OpenAI    │
│  GPT-4o-mini│
└──────┬──────┘
       │
       │ (API calls with tool definitions)
       │
┌──────▼──────────────────┐
│  openai_mcp_client.py   │  ← Python client
│                         │
│  - Converts MCP tools   │
│    to OpenAI format     │
│  - Handles chat loop    │
│  - Executes tool calls  │
└──────┬──────────────────┘
       │
       │ (MCP Protocol - stdio)
       │
┌──────▼──────────────────┐
│ @playwright/mcp@latest  │  ← Playwright MCP Server (via npx)
│                         │
│  - Browser automation   │
│  - Page navigation      │
│  - Element interaction  │
│  - Screenshot capture   │
└──────┬──────────────────┘
       │
       │ (Playwright API)
       │
┌──────▼──────────────────┐
│     Web Browsers        │
│  (Chrome, Firefox, etc) │
└─────────────────────────┘
```

## Prerequisites

### 1. Install Node.js

Playwright MCP server requires Node.js:
- Download from https://nodejs.org
- Version 18+ recommended

### 2. Install Python Dependencies

```bash
pip install openai python-dotenv mcp
```

### 3. OpenAI API Key

Get your API key from https://platform.openai.com/api-keys

Create `.env` file:
```
OPENAI_API_KEY=your_api_key_here
```

## Configuration

The MCP client is configured in `mcp_client_config.json`:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest"
      ]
    }
  }
}
```

This tells the client to:
- Use `npx` to run Playwright MCP server
- Automatically download `@playwright/mcp@latest` if not present
- Connect via stdio (standard input/output)

## Usage

### Interactive Mode

Run the client in interactive mode:

```bash
python openai_mcp_client.py
```

Then chat with the AI:

```
You: Open google.com in the browser
[Assistant] Opening Google...

You: Search for "Python tutorials"
[Assistant] Searching...

You: Take a screenshot
[Assistant] Screenshot saved!

You: What's the page title?
[Assistant] The page title is "Python Tutorials - Google Search"
```

### Example Mode

Run examples:

```bash
python openai_mcp_client.py example
```

### Programmatic Usage

```python
import asyncio
from openai_mcp_client import OpenAIMCPClient

async def main():
    # Initialize client
    client = OpenAIMCPClient()

    # Connect to Playwright MCP server
    await client.connect_mcp_server()

    # Use natural language to control browser
    response, _ = await client.chat("Open youtube.com and take a screenshot")
    print(response)

asyncio.run(main())
```

## Available Playwright Tools

The Playwright MCP server provides tools like:
- `playwright_navigate` - Navigate to a URL
- `playwright_screenshot` - Take a screenshot
- `playwright_click` - Click an element
- `playwright_fill` - Fill a form field
- `playwright_extract` - Extract text/data from page
- And more...

The AI automatically chooses which tools to use based on your request.

## Example Commands

### Navigation
- "Open github.com"
- "Go to youtube.com"
- "Navigate to example.com"

### Interaction
- "Click the login button"
- "Fill the search box with 'Python'"
- "Submit the form"

### Information Extraction
- "What's the page title?"
- "Get all the links on this page"
- "Extract the main heading"

### Screenshots
- "Take a screenshot"
- "Capture the page"
- "Save an image of the current page"

### Complex Tasks
- "Go to google.com, search for 'OpenAI', and take a screenshot"
- "Open reddit.com, find the top post, and tell me the title"
- "Navigate to wikipedia.org and get the featured article"

## Cost

Using GPT-4o-mini:
- Per chat: ~$0.0001-0.001 (very cheap)
- 100 browser automation tasks: ~$0.01-0.10

Much more affordable than GPT-4!

## Troubleshooting

### "npx not found"
Install Node.js: https://nodejs.org

### "Insufficient quota"
Add billing to OpenAI account: https://platform.openai.com/account/billing

### Browser not opening
Playwright MCP server runs headless by default. The browser runs in the background.

### "MCP server not connected"
Make sure you called `await client.connect_mcp_server()` first.

## Advanced Configuration

### Custom Browser

Modify the Playwright MCP configuration to specify browser:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest",
        "--browser=firefox"
      ]
    }
  }
}
```

Options: `chromium`, `firefox`, `webkit`

### Headful Mode (Show Browser)

To see the browser in action:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest",
        "--headless=false"
      ]
    }
  }
}
```

## Use Cases

### Web Scraping
"Go to news.ycombinator.com and get the top 5 post titles"

### Testing
"Open my website at localhost:3000 and verify the homepage loads"

### Automation
"Fill out this form on example.com with my information"

### Research
"Search for 'Python asyncio' on Stack Overflow and summarize the top answer"

### Monitoring
"Check if mywebsite.com is up and what the response time is"

## Files

- `openai_mcp_client.py` - Main MCP client implementation
- `mcp_client_config.json` - MCP server configuration
- `.env` - OpenAI API key

## Resources

- Playwright MCP: https://github.com/microsoft/playwright-mcp
- MCP Protocol: https://modelcontextprotocol.io
- OpenAI API: https://platform.openai.com/docs
- Playwright: https://playwright.dev

## Support

For issues:
1. Check OpenAI API key is valid
2. Verify Node.js is installed (`node --version`)
3. Ensure npx is available (`npx --version`)
4. Check OpenAI account has billing enabled

## Next Steps

1. Try the interactive mode
2. Experiment with different commands
3. Build automation scripts using the client
4. Integrate into your own projects

Happy automating! 🎭🤖
