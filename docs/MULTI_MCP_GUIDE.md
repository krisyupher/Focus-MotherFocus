# Multi-MCP Server Integration Guide

## Overview

Connect OpenAI to **multiple MCP servers** simultaneously:
- **Playwright** - Browser automation
- **Filesystem** - File and directory operations

Control both your browser AND files using natural language!

## Configuration

### MCP Client Config

Your [mcp_client_config.json](../mcp_client_config.json) now has TWO servers:

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest"]
    },
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "C:\\Users\\crist\\Documents\\Develop\\FocusMotherFocus",
        "C:\\Users\\crist\\Desktop"
      ]
    }
  }
}
```

### Allowed Directories

The filesystem server can access:
- `C:\Users\crist\Documents\Develop\FocusMotherFocus` - Your project
- `C:\Users\crist\Desktop` - Your desktop

**Add more directories** by adding paths to the args array.

## Usage

### Run Multi-Server Client

```bash
python openai_multi_mcp_client.py
```

### Interactive Mode

```
You: List all Python files in my project
[Assistant] Found 42 Python files: main_v2.py, ...

You: Open google.com in the browser
[Assistant] Navigating to google.com...

You: Take a screenshot and save it to my Desktop
[Assistant] Screenshot saved to Desktop/screenshot.png
```

### Example Commands

**File Operations:**
- "List all Python files in the project"
- "Read the contents of main_v2.py"
- "Create a new file called test.txt with 'Hello World'"
- "Search for files containing 'playwright'"
- "Show me the directory structure"

**Browser Operations:**
- "Open youtube.com"
- "Take a screenshot of the current page"
- "Click the login button"
- "Extract all links from the page"

**Combined Operations:**
- "Open github.com, take a screenshot, and save it to Desktop"
- "Read the README.md file and summarize it"
- "List all markdown files and open the first one in browser"

## Available Tools

### Playwright Server Tools

- `playwright_navigate` - Navigate to URL
- `playwright_screenshot` - Capture screenshot
- `playwright_click` - Click element
- `playwright_fill` - Fill form field
- `playwright_extract` - Extract page content
- And more...

### Filesystem Server Tools

- `read_file` - Read file contents
- `write_file` - Write/create file
- `list_directory` - List directory contents
- `search_files` - Search for files
- `get_file_info` - Get file metadata
- `create_directory` - Create new directory
- `move_file` - Move/rename file
- `delete_file` - Delete file
- And more...

## Setup

### Prerequisites

1. **Node.js** (for npx):
   ```bash
   node --version  # Should be 18+
   ```

2. **Python packages**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Playwright browsers**:
   ```bash
   playwright install chromium
   ```

### First Run

**Step 1: Install MCP servers**

The first time you run it, npx will download the servers automatically.

**Step 2: Run the client**

```bash
python openai_multi_mcp_client.py
```

You'll see:
```
[MCP Client] Found 2 MCP servers in config
[MCP Client] Connecting to playwright...
[MCP Client] ✓ Connected to playwright
[MCP Client] Loaded 15 tools from playwright:
  • playwright_navigate: Navigate to URL
  • playwright_screenshot: Take screenshot
  ...

[MCP Client] Connecting to filesystem...
[MCP Client] ✓ Connected to filesystem
[MCP Client] Loaded 12 tools from filesystem:
  • read_file: Read file contents
  • write_file: Write file
  ...

Connected! You can now use browser automation and file operations.
```

## Security

### Filesystem Access

The filesystem server can ONLY access directories you specify in the config:

```json
"args": [
  "-y",
  "@modelcontextprotocol/server-filesystem",
  "C:\\Path\\To\\Allowed\\Directory1",
  "C:\\Path\\To\\Allowed\\Directory2"
]
```

**Add directories carefully** - The AI can read/write/delete files in these locations!

### Recommended Setup

**Safe directories:**
- Your project folder
- A dedicated "AI Workspace" folder
- Desktop (for outputs)

**Avoid:**
- System directories (`C:\Windows`, `C:\Program Files`)
- Entire drives (`C:\`)
- Sensitive data folders

## Example Use Cases

### Web Scraping to File

```
You: Go to example.com, extract all headings, and save them to Desktop/headings.txt
```

AI will:
1. Use Playwright to navigate to example.com
2. Extract headings from page
3. Use Filesystem to write to Desktop/headings.txt

### Code Analysis

```
You: Read all Python files in src/ and count how many classes we have
```

AI will:
1. Use Filesystem to list files in src/
2. Read each Python file
3. Count class definitions
4. Return the total

### Automated Screenshots

```
You: Take screenshots of google.com, github.com, and youtube.com and save them to Desktop
```

AI will:
1. Navigate to each site with Playwright
2. Take screenshots
3. Save each to Desktop with Filesystem

### Project Documentation

```
You: Read all .md files and create a summary in Desktop/project-summary.txt
```

AI will:
1. List all markdown files with Filesystem
2. Read each file
3. Generate summary
4. Write to Desktop

## Programmatic Usage

```python
import asyncio
from openai_multi_mcp_client import OpenAIMultiMCPClient

async def main():
    # Initialize with both servers
    client = OpenAIMultiMCPClient()
    await client.connect_all_servers()

    # Use natural language
    response, _ = await client.chat(
        "List Python files and open the first one in browser"
    )
    print(response)

asyncio.run(main())
```

## Troubleshooting

### "Failed to connect to playwright"

Make sure Chrome is running with debug port:
```batch
start_chrome_debug.bat
```

### "Failed to connect to filesystem"

Check:
1. Node.js is installed: `node --version`
2. npx is available: `npx --version`
3. Paths in config are valid Windows paths with `\\`

### "Permission denied" on file operations

The directory must be in the allowed list in config.

Add it to `mcp_client_config.json`:
```json
"args": [
  "-y",
  "@modelcontextprotocol/server-filesystem",
  "C:\\Your\\Project",
  "C:\\Desktop",
  "C:\\New\\Allowed\\Path"  // Add here
]
```

### Tools not showing up

Check the console output when connecting:
```
[MCP Client] Loaded X tools from playwright:
[MCP Client] Loaded Y tools from filesystem:
```

If 0 tools loaded, the server failed to start.

## Configuration Tips

### Add More Servers

You can add more MCP servers to the config:

```json
{
  "mcpServers": {
    "playwright": { ... },
    "filesystem": { ... },
    "your-custom-server": {
      "command": "npx",
      "args": ["@your/mcp-server"]
    }
  }
}
```

The client will connect to all of them!

### Different Directories per Project

Create multiple config files:

```bash
python openai_multi_mcp_client.py  # Uses mcp_client_config.json
```

Or specify in code:
```python
client = OpenAIMultiMCPClient(config_file="custom_config.json")
```

## Cost

Using GPT-4o-mini with multi-server:
- Simple file operation: ~$0.0001
- Browser + file operation: ~$0.0002
- Complex multi-step task: ~$0.0005

Still very affordable!

## Limitations

- Filesystem server limited to specified directories
- Playwright requires Chrome with debug port
- Both servers run separately (no direct communication)
- All communication goes through OpenAI

## Resources

- **Playwright MCP**: https://github.com/microsoft/playwright-mcp
- **Filesystem MCP**: https://github.com/modelcontextprotocol/servers
- **MCP Protocol**: https://modelcontextprotocol.io

## Summary

With multi-MCP integration, you can:
- ✅ Control browser AND files with one client
- ✅ Combine operations (screenshot → save to file)
- ✅ Use natural language for everything
- ✅ Safely restrict filesystem access
- ✅ Extend with more MCP servers

**Start automating with both browser and filesystem!** 🚀📂
