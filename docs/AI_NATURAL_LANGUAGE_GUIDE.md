# AI Natural Language Command Guide

## 🎉 AI-Powered GUI

Your FocusMotherFocus app now has an **AI-powered natural language interface**!

Instead of clicking buttons, just **type what you want** in plain English and the AI will figure out what to do.

## ✅ Setup Complete

- ✅ OpenAI API Key configured in `.env`
- ✅ AI Command Processor integrated in GUI
- ✅ Natural language input field added to interface
- ✅ 5 AI-powered commands available

## 🚀 How to Use

### 1. Launch the App

```bash
python main_v2.py
```

### 2. Find the AI Assistant Section

At the top of the window, you'll see:
```
🤖 AI Assistant - Type Natural Language Commands
```

### 3. Type Your Command

Just type naturally, like you're talking to someone:

**Examples:**
- "Monitor Netflix and YouTube"
- "Remove Facebook"
- "Start monitoring"
- "Stop monitoring"
- "Show me what I'm monitoring"

### 4. Press Enter or Click "🚀 Execute"

The AI will:
1. Understand your command
2. Figure out which action(s) to take
3. Execute them
4. Show you the result

## 💡 Natural Language Examples

### Adding Targets

**What you type:**
```
Monitor Netflix and YouTube
```

**What happens:**
- AI calls `add_target` twice
- Adds Netflix to monitoring
- Adds YouTube to monitoring
- Shows: "✅ ✓ add_target: Added Netflix / ✓ add_target: Added YouTube"

**More examples:**
- "I want to monitor Facebook"
- "Add Instagram and Twitter to my list"
- "Track Spotify for me"
- "Monitor Calculator app"

### Removing Targets

**What you type:**
```
Remove Facebook
```

**What happens:**
- AI calls `remove_target`
- Removes Facebook from monitoring
- Shows: "✅ ✓ remove_target: Removed Facebook"

**More examples:**
- "Delete Netflix from my monitoring"
- "Stop tracking YouTube"
- "Remove Instagram"

### Starting Monitoring

**What you type:**
```
Start monitoring
```

**What happens:**
- AI calls `start_monitoring`
- Begins checking every second
- Shows: "✅ ✓ start_monitoring: Monitoring started"

**More examples:**
- "Begin monitoring"
- "Start watching for procrastination"
- "Turn on monitoring"

### Stopping Monitoring

**What you type:**
```
Stop monitoring
```

**What happens:**
- AI calls `stop_monitoring`
- Stops checking
- Clears all alerts
- Shows: "✅ ✓ stop_monitoring: Monitoring stopped"

**More examples:**
- "Stop watching"
- "Turn off monitoring"
- "Pause monitoring"

### Listing Targets

**What you type:**
```
Show me what I'm monitoring
```

**What happens:**
- AI calls `list_targets`
- Returns list of all targets
- Shows: "✅ Monitoring 3 targets: Netflix, YouTube, Facebook"

**More examples:**
- "What targets am I tracking?"
- "List my monitored sites"
- "What am I watching?"

## 🎯 How It Works

### Architecture

```
You type natural language
    ↓
GUI captures input
    ↓
AICommandProcessor
    ↓
OpenAI GPT-4 (function calling)
    ↓
AI determines which tools to use
    ↓
Callbacks execute use cases
    ↓
Result shown in GUI
```

### Under the Hood

1. **Your input**: "Monitor Netflix and YouTube"

2. **Sent to OpenAI** with tool definitions:
   ```json
   tools: [
     "add_target",
     "remove_target",
     "start_monitoring",
     "stop_monitoring",
     "list_targets"
   ]
   ```

3. **OpenAI decides**: "User wants to add 2 targets"

4. **Tool calls**:
   ```javascript
   [
     { function: "add_target", args: { name: "Netflix" } },
     { function: "add_target", args: { name: "YouTube" } }
   ]
   ```

5. **GUI executes**:
   - Calls `_ai_add_target("Netflix")`
   - Calls `_ai_add_target("YouTube")`

6. **Results displayed**:
   ```
   ✅ ✓ add_target: Added Netflix
   ✓ add_target: Added YouTube
   ```

## 🛠️ Available Commands

The AI understands these intents and maps them to tools:

| What You Want | AI Tool Used | Example Input |
|---------------|--------------|---------------|
| Add target(s) | `add_target` | "Monitor Netflix" |
| Remove target(s) | `remove_target` | "Remove Facebook" |
| Start monitoring | `start_monitoring` | "Start monitoring" |
| Stop monitoring | `stop_monitoring` | "Stop monitoring" |
| List targets | `list_targets` | "Show my targets" |

## ⚙️ Configuration

### API Key

Stored in `.env`:
```
OPENAI_API_KEY=sk-proj-...
```

### Model

Using `gpt-4o-mini` (defined in `ai_command_processor.py`)

To change model:
```python
# In ai_command_processor.py, line ~155
response = self.client.chat.completions.create(
    model="gpt-4o-mini",  # ← Change here
    ...
)
```

Options:
- `gpt-4o-mini` - Fast and affordable (recommended)
- `gpt-4o` - Most capable, higher cost
- `gpt-4-turbo` - Legacy model
- `gpt-3.5-turbo` - Fastest, cheapest (less accurate)

## 💰 Cost

**Approximate costs per command:**
- Input tokens: ~200 tokens = $0.002
- Output tokens: ~50 tokens = $0.001
- **Total per command**: ~$0.003 (less than a penny!)

**100 commands = ~$0.30**

## 🔒 Privacy

- Commands sent to OpenAI API
- No conversation history stored
- Each command is independent
- Your targets/data never leave your machine (only commands sent)

## ❌ Troubleshooting

### "AI Not Available" Error

**Cause**: OpenAI API key not set or invalid

**Solution**:
1. Check `.env` file exists
2. Verify `OPENAI_API_KEY=sk-...` is correct
3. Restart application

### "Error processing command"

**Cause**: Network issue or API error

**Solution**:
1. Check internet connection
2. Verify API key is valid
3. Check OpenAI service status

### Commands Not Working

**Cause**: AI misunderstood intent

**Solution**:
- Be more specific: "Add Netflix" instead of "Netflix"
- Use action words: "Monitor", "Remove", "Start", "Stop", "Show"
- Check response for what AI actually did

## 🎓 Tips for Best Results

### ✅ Good Commands

- "Monitor Netflix" ← Clear action
- "Remove YouTube and Facebook" ← Specific targets
- "Start monitoring now" ← Direct command
- "Show all my targets" ← Clear intent

### ❌ Unclear Commands

- "Netflix" ← No action specified
- "Can you help?" ← Too vague
- "Maybe add something" ← Uncertain
- "What about YouTube?" ← Ambiguous

### 🌟 Pro Tips

1. **Be specific**: Use action verbs (monitor, remove, start, stop, show)
2. **Multiple targets**: List them: "Monitor X, Y, and Z"
3. **Check result**: Read the green response to see what happened
4. **Trial and error**: If it doesn't work, rephrase and try again

## 🚀 Advanced Usage

### Batch Operations

**Add multiple targets at once:**
```
Monitor Netflix, YouTube, Facebook, Instagram, and Twitter
```

AI will call `add_target` 5 times automatically!

### Mixed Commands

**Add and start in one command:**
```
Add Netflix and start monitoring
```

AI will:
1. Call `add_target` for Netflix
2. Call `start_monitoring`

### Complex Requests

```
I need to focus. Monitor Netflix, YouTube, and Facebook, then start watching for procrastination.
```

AI understands and executes all steps!

## 📊 Comparison

### Before (Manual)
1. Type "Netflix" in input
2. Click "➕ Add" button
3. Type "YouTube" in input
4. Click "➕ Add" button
5. Click "▶️ Start" button

**5 steps, 2 inputs, 3 clicks**

### After (AI)
1. Type "Monitor Netflix and YouTube and start monitoring"
2. Press Enter

**1 step, 1 input, 0 clicks** ✨

## 🎉 Summary

You can now control your entire productivity monitoring system using **natural language**!

Just type what you want, and the AI figures out how to do it.

**Examples to try right now:**
- "Monitor Netflix"
- "Start monitoring"
- "Show what I'm tracking"
- "Remove all targets"
- "Add YouTube, Facebook, and Instagram"

It's like having a **smart assistant** for your productivity! 🤖✨
