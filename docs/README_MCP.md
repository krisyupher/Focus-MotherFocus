# MCP Integration - Control FocusMotherFocus with OpenAI

## Quick Start

**1. Install dependencies:**
```bash
pip install mcp openai
```

**2. Set your OpenAI API key:**
```bash
set OPENAI_API_KEY=sk-your-key-here
```

**3. Start chatting:**
```bash
python openai_mcp_client.py
```

**4. Try natural language commands:**
```
You: Add Netflix to my monitoring
You: Start monitoring
You: What's my status?
```

That's it! OpenAI will control your productivity monitor through MCP.

## What You Can Do

### Natural Language Control

Talk to OpenAI normally - it will use the right tools automatically:

**Adding Targets:**
- "Monitor Netflix and YouTube"
- "Add Facebook to my blocked list"
- "I want to track Instagram and Twitter"

**Starting/Stopping:**
- "Start monitoring"
- "Stop monitoring"
- "Begin watching for procrastination"

**Checking Status:**
- "What am I monitoring?"
- "Show my targets"
- "Is monitoring active?"

**Removing Targets:**
- "Remove Netflix"
- "Stop watching YouTube"
- "Delete all targets"

## How It Works

```
You type → OpenAI GPT-4 → MCP Server → FocusMotherFocus
                 ↓
         Uses tools like:
         - add_target
         - start_monitoring
         - get_status
```

OpenAI automatically:
1. Understands your intent
2. Calls the right MCP tools
3. Returns results in natural language

## Example Session

```
You: I need help staying productive. Can you monitor Netflix and YouTube?