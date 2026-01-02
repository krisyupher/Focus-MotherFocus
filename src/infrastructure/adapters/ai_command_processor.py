"""
AI Command Processor - Process natural language commands using OpenAI.

This allows users to type natural language like "Monitor Netflix" or
"Remove YouTube" and the AI will automatically execute the right actions.
"""
import os
import json
from typing import Optional, Callable, Any
from openai import OpenAI
from dotenv import load_dotenv

# Load API key
load_dotenv()


class AICommandProcessor:
    """
    Process natural language commands using OpenAI function calling.

    This enables GUI users to type commands like:
    - "Monitor Netflix and YouTube"
    - "Remove Facebook"
    - "Start monitoring"
    - "Show me what I'm monitoring"
    """

    def __init__(self):
        """Initialize OpenAI client."""
        self.api_key = os.getenv("OPENAI_API_KEY")
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY not found in environment variables")

        self.client = OpenAI(api_key=self.api_key)

        # Tool definitions for OpenAI
        self.tools = [
            {
                "type": "function",
                "function": {
                    "name": "add_target",
                    "description": "Add a monitoring target (website, app, or both). Examples: Netflix, Facebook, Calculator",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string",
                                "description": "Target name to monitor"
                            }
                        },
                        "required": ["name"]
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "remove_target",
                    "description": "Remove a monitoring target by name",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string",
                                "description": "Target name to remove"
                            }
                        },
                        "required": ["name"]
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "start_monitoring",
                    "description": "Start the monitoring system",
                    "parameters": {
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "stop_monitoring",
                    "description": "Stop the monitoring system",
                    "parameters": {
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "list_targets",
                    "description": "List all monitored targets",
                    "parameters": {
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                }
            }
        ]

    def process_command(
        self,
        user_input: str,
        callbacks: dict[str, Callable]
    ) -> str:
        """
        Process a natural language command.

        Args:
            user_input: Natural language command from user
            callbacks: Dictionary mapping tool names to callback functions
                      e.g., {"add_target": lambda name: add_target_use_case.execute(...)}

        Returns:
            Human-readable response message

        Example:
            callbacks = {
                "add_target": lambda name: self.add_target(name),
                "remove_target": lambda name: self.remove_target(name),
                "start_monitoring": lambda: self.start_monitoring(),
                "stop_monitoring": lambda: self.stop_monitoring(),
                "list_targets": lambda: self.list_targets()
            }

            response = processor.process_command("Monitor Netflix", callbacks)
        """
        try:
            # Call OpenAI with function calling
            messages = [
                {
                    "role": "system",
                    "content": (
                        "You are a helpful assistant that controls a productivity monitoring system. "
                        "When the user gives a command, use the appropriate function to execute it. "
                        "Be concise and friendly in your responses."
                    )
                },
                {
                    "role": "user",
                    "content": user_input
                }
            ]

            response = self.client.chat.completions.create(
                model="gpt-4o-mini",
                messages=messages,
                tools=self.tools,
                tool_choice="auto"
            )

            assistant_message = response.choices[0].message

            # Check if tools were called
            if assistant_message.tool_calls:
                results = []

                # Execute each tool call
                for tool_call in assistant_message.tool_calls:
                    tool_name = tool_call.function.name
                    tool_args = json.loads(tool_call.function.arguments)

                    # Get callback for this tool
                    if tool_name not in callbacks:
                        results.append(f"Error: No callback for {tool_name}")
                        continue

                    # Execute callback
                    try:
                        callback = callbacks[tool_name]

                        # Call with args if present, otherwise no args
                        if tool_args:
                            # Extract first argument value (usually "name")
                            arg_value = list(tool_args.values())[0] if tool_args else None
                            if arg_value:
                                result = callback(arg_value)
                            else:
                                result = callback()
                        else:
                            result = callback()

                        results.append(f"✓ {tool_name}: {result if result else 'Done'}")

                    except Exception as e:
                        results.append(f"✗ {tool_name}: {str(e)}")

                # Return combined results
                return "\n".join(results)

            else:
                # No tools called, return text response
                return assistant_message.content or "Command processed"

        except Exception as e:
            return f"Error processing command: {str(e)}"

    def process_command_simple(
        self,
        user_input: str,
        add_target_fn: Optional[Callable[[str], Any]] = None,
        remove_target_fn: Optional[Callable[[str], Any]] = None,
        start_monitoring_fn: Optional[Callable[[], Any]] = None,
        stop_monitoring_fn: Optional[Callable[[], Any]] = None,
        list_targets_fn: Optional[Callable[[], Any]] = None
    ) -> str:
        """
        Simplified version - pass functions directly.

        Args:
            user_input: Natural language command
            add_target_fn: Function to add target (takes name)
            remove_target_fn: Function to remove target (takes name)
            start_monitoring_fn: Function to start monitoring
            stop_monitoring_fn: Function to stop monitoring
            list_targets_fn: Function to list targets

        Returns:
            Response message

        Example:
            response = processor.process_command_simple(
                "Monitor Netflix",
                add_target_fn=self._on_add_target_ai
            )
        """
        callbacks = {}

        if add_target_fn:
            callbacks["add_target"] = add_target_fn
        if remove_target_fn:
            callbacks["remove_target"] = remove_target_fn
        if start_monitoring_fn:
            callbacks["start_monitoring"] = start_monitoring_fn
        if stop_monitoring_fn:
            callbacks["stop_monitoring"] = stop_monitoring_fn
        if list_targets_fn:
            callbacks["list_targets"] = list_targets_fn

        return self.process_command(user_input, callbacks)
