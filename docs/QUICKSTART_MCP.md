# Quick Start - OpenAI MCP Integration

## ✅ Setup Complete!

Your API key is configured and ready to use!

## What You Have

✅ **API Key**: Stored in `.env` file
✅ **MCP Server**: `mcp_server.py` - Exposes 7 monitoring tools
✅ **OpenAI Client**: `openai_mcp_client.py` - Connects to GPT-4
✅ **Configuration**: `mcp_client_config.json` - MCP settings

## 🚀 How to Use

### Method 1: Claude Desktop (Recommended)

The easiest way is to use Claude Desktop with MCP:

1. **Install Claude Desktop**: https://claude.ai/download
2. **Configure MCP**: Add this to your Claude config:

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "focus-mother-focus": {
      "command": "python",
      "args": ["c:\\Users\\crist\\Documents\\Develop\\FocusMotherFocus\\mcp_server.py"]
    }
  }
}
```

3. **Restart Claude Desktop**
4. **Chat with Claude**: "Add Netflix to my monitoring"

### Method 2: OpenAI API (Python)

Use the Python client to chat with OpenAI GPT-4:

```bash
python openai_mcp_client.py
```

Then chat naturally:
```
You: Monitor Netflix and YouTube
You: Start monitoring
You: What's my status?
```

### Method 3: Direct MCP Server

Run the MCP server for other MCP clients:

```bash
python mcp_server.py
```

The server will run in stdio mode, waiting for MCP protocol messages.

## 🛠️ Available Commands

Just talk naturally! Examples:

**Add Targets:**
- "Monitor Netflix"
- "Add Facebook and Instagram to my monitoring"
- "I want to track YouTube"

**Control Monitoring:**
- "Start monitoring"
- "Stop monitoring"
- "Begin watching for procrastination"

**Check Status:**
- "What am I monitoring?"
- "Show my targets"
- "Is monitoring active?"

**Remove Targets:**
- "Remove Netflix"
- "Stop watching YouTube"

## 🎯 7 Available Tools

The MCP server exposes these tools to AI assistants:

1. **add_target** - Add monitoring targets
2. **remove_target** - Remove targets
3. **list_targets** - Show all targets
4. **start_monitoring** - Start checking (1 sec interval)
5. **stop_monitoring** - Stop and clear alerts
6. **get_status** - Check running status
7. **check_now** - Manual check

## 📝 Example Session

```
You: I need help staying productive. Monitor Netflix and Facebook.