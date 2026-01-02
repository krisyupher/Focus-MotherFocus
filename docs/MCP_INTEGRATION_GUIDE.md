# MCP Integration Guide - FocusMotherFocus + OpenAI

## Overview

This guide explains how to use **Model Context Protocol (MCP)** to connect **OpenAI** (or other AI assistants) to your **FocusMotherFocus** monitoring system.

With MCP integration, you can control your productivity monitor using natural language through OpenAI's GPT-4!

## What is MCP?

**Model Context Protocol (MCP)** is an open protocol that allows AI assistants to:
- Access external tools and data sources
- Execute actions in your applications
- Maintain context across interactions

Think of it as a universal API for AI assistants to interact with your software.

## Architecture

```
┌─────────────┐
│   OpenAI    │
│   GPT-4     │
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
│    mcp_server.py        │  ← MCP Server
│                         │
│  - Exposes 7 tools      │
│  - Wraps use cases      │
│  - Returns results      │
└──────┬──────────────────┘
       │
       │ (Direct function calls)
       │
┌──────▼──────────────────┐
│  FocusMotherFocus       │  ← Your App
│  - Use Cases            │
│  - Domain Logic         │
│  - Monitoring System    │
└─────────────────────────┘
```

## Available Tools

The MCP server exposes 7 tools that OpenAI can call:

### 1. `add_target`
Add a new monitoring target (website, application, or both).

**Parameters**:
- `name` (string): Target name (auto-resolves to URL/process)

**Examples**:
- "Netflix" → Resolves to netflix.com + Netflix.exe
- "facebook.com" → Monitors Facebook website
- "Calculator" → Monitors Windows Calculator app

### 2. `remove_target`
Remove a monitoring target by name.

**Parameters**:
- `name` (string): Name of target to remove

### 3. `list_targets`
List all currently monitored targets.

**Parameters**: None

**Returns**: List of targets with URLs and process names

### 4. `start_monitoring`
Start the monitoring system (checks every 1 second).

**Parameters**: None

**Effects**:
- Starts checking targets every second
- Triggers alerts when targets are active
- Runs until stopped

### 5. `stop_monitoring`
Stop the monitoring system and clear all alerts.

**Parameters**: None

**Effects**:
- Stops periodic checking
- Closes all active alert windows
- Resets monitoring state

### 6. `get_status`
Get current monitoring system status.

**Parameters**: None

**Returns**:
- Running state (RUNNING/STOPPED)
- Number of targets
- List of monitored targets

### 7. `check_now`
Manually trigger a check of all targets (for testing).

**Parameters**: None

**Effects**: Immediately checks all targets and triggers alerts if active

## Setup Instructions

### Prerequisites

1. **Python 3.11+** installed
2. **OpenAI API Key** - Get from https://platform.openai.com/api-keys
3. **FocusMotherFocus** - This project

### Installation

1. **Install dependencies**:
```bash
pip install mcp openai
```

2. **Set OpenAI API Key**:

**Windows**:
```cmd
set OPENAI_API_KEY=sk-your-api-key-here
```

**Or** create a `.env` file:
```
OPENAI_API_KEY=sk-your-api-key-here
```

### Running the System

#### Option 1: Interactive Chat (Recommended)

Start an interactive chat session with OpenAI:

```bash
python openai_mcp_client.py
```

**What happens**:
1. MCP server starts automatically
2. Connects to OpenAI GPT-4
3. Enters chat loop
4. Type natural language commands
5. OpenAI calls tools as needed

**Example conversation**:
```
You: Add Netflix and YouTube to my monitoring
[Assistant uses add_target tool twice]
Assistant: I've added Netflix and YouTube to your monitoring targets.

You: Start monitoring
[Assistant uses start_monitoring tool]
Assistant: Monitoring started! I'll alert you every second when you're on Netflix or YouTube.

You: What's my status?
[Assistant uses get_status tool]
Assistant: You're currently monitoring 2 targets: Netflix and YouTube. Monitoring is RUNNING.
```

#### Option 2: Example Usage

Run predefined examples:

```bash
python openai_mcp_client.py example
```

This demonstrates:
- Adding targets
- Checking status
- Starting monitoring
- Listing targets

### Direct MCP Server (Advanced)

You can also run the MCP server directly for use with other MCP clients:

```bash
python mcp_server.py
```

This starts the server in stdio mode, waiting for MCP protocol messages.

## Usage Examples

### Example 1: Setup Your Monitoring

```
You: I want to monitor Netflix, Facebook, and Instagram