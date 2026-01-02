"""
MCP Server for FocusMotherFocus - Exposes monitoring tools via Model Context Protocol.

This server allows AI assistants (like OpenAI) to control the monitoring system through MCP.
"""
import asyncio
import json
from typing import Any, Sequence
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import (
    Tool,
    TextContent,
    ImageContent,
    EmbeddedResource,
)

# Import your application components
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.use_cases.add_target import AddTargetUseCase
from src.application.use_cases.remove_target import RemoveTargetUseCase
from src.application.use_cases.check_targets import CheckTargetsUseCase
from src.application.use_cases.start_monitoring_v2 import StartMonitoringV2UseCase
from src.application.use_cases.stop_monitoring_v2 import StopMonitoringV2UseCase
from src.infrastructure.adapters import (
    RequestsHttpChecker,
    WindowsAlertNotifier,
    ThreadedScheduler
)
from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector
from src.infrastructure.adapters.windows_process_detector import WindowsProcessDetector
from src.infrastructure.persistence.json_config_repository_v2 import JsonConfigRepositoryV2


# Initialize application components
def setup_monitoring_system():
    """Initialize all monitoring system components."""
    config_repository = JsonConfigRepositoryV2(config_file_path="config.json")
    http_checker = RequestsHttpChecker(timeout=5)
    browser_detector = WindowsBrowserDetector()
    process_detector = WindowsProcessDetector()
    alert_notifier = WindowsAlertNotifier(parent_window=None)
    scheduler = ThreadedScheduler()

    session = config_repository.load_session()

    add_target_use_case = AddTargetUseCase(
        session=session,
        config_repository=config_repository
    )

    remove_target_use_case = RemoveTargetUseCase(
        session=session,
        config_repository=config_repository,
        alert_notifier=alert_notifier
    )

    check_targets_use_case = CheckTargetsUseCase(
        session=session,
        http_checker=http_checker,
        browser_detector=browser_detector,
        process_detector=process_detector,
        alert_notifier=alert_notifier
    )

    start_monitoring_use_case = StartMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler
    )

    stop_monitoring_use_case = StopMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler,
        alert_notifier=alert_notifier
    )

    return {
        'session': session,
        'add_target': add_target_use_case,
        'remove_target': remove_target_use_case,
        'check_targets': check_targets_use_case,
        'start_monitoring': start_monitoring_use_case,
        'stop_monitoring': stop_monitoring_use_case,
        'scheduler': scheduler
    }


# Global components
components = setup_monitoring_system()

# Create MCP server
app = Server("focus-mother-focus")


@app.list_tools()
async def list_tools() -> list[Tool]:
    """List available monitoring tools."""
    return [
        Tool(
            name="add_target",
            description="Add a new monitoring target (website, application, or both). "
                       "Examples: 'Netflix' (auto-resolves), 'facebook.com', 'Calculator'",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string",
                        "description": "Target name (will be auto-resolved to URL/process)"
                    }
                },
                "required": ["name"]
            }
        ),
        Tool(
            name="remove_target",
            description="Remove a monitoring target by name",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string",
                        "description": "Name of the target to remove"
                    }
                },
                "required": ["name"]
            }
        ),
        Tool(
            name="list_targets",
            description="List all currently monitored targets",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="start_monitoring",
            description="Start the monitoring system. Will check targets every 1 second and trigger alerts.",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="stop_monitoring",
            description="Stop the monitoring system and clear all active alerts",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="get_status",
            description="Get current monitoring system status (running/stopped, target count)",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="check_now",
            description="Manually trigger a check of all targets (useful for testing)",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        )
    ]


@app.call_tool()
async def call_tool(name: str, arguments: Any) -> Sequence[TextContent | ImageContent | EmbeddedResource]:
    """Handle tool calls."""

    try:
        if name == "add_target":
            target_name = arguments.get("name", "").strip()
            if not target_name:
                return [TextContent(
                    type="text",
                    text="Error: Target name is required"
                )]

            # Auto-resolve target
            from src.core.services.target_resolver import TargetResolver
            url, process_name, display_name = TargetResolver.resolve(target_name)

            # Add target
            try:
                components['add_target'].execute(
                    name=display_name,
                    url_str=url.value if url else None,
                    process_name_str=process_name.value if process_name else None
                )

                result = f"✓ Added target: {display_name}\n"
                if url:
                    result += f"  Website: {url.value}\n"
                if process_name:
                    result += f"  Application: {process_name.value}\n"

                return [TextContent(type="text", text=result)]

            except Exception as e:
                return [TextContent(
                    type="text",
                    text=f"Error adding target: {str(e)}"
                )]

        elif name == "remove_target":
            target_name = arguments.get("name", "").strip()
            if not target_name:
                return [TextContent(
                    type="text",
                    text="Error: Target name is required"
                )]

            try:
                components['remove_target'].execute(name=target_name)
                return [TextContent(
                    type="text",
                    text=f"✓ Removed target: {target_name}"
                )]
            except Exception as e:
                return [TextContent(
                    type="text",
                    text=f"Error removing target: {str(e)}"
                )]

        elif name == "list_targets":
            targets = components['session'].get_all_targets()

            if not targets:
                return [TextContent(
                    type="text",
                    text="No targets currently being monitored."
                )]

            result = f"Monitoring {len(targets)} target(s):\n\n"
            for target in targets:
                result += f"• {target.name}\n"
                if target.url:
                    result += f"  └─ Website: {target.url.value}\n"
                if target.process_name:
                    result += f"  └─ Application: {target.process_name.value}\n"

            return [TextContent(type="text", text=result)]

        elif name == "start_monitoring":
            try:
                components['start_monitoring'].execute(
                    check_callback=components['check_targets'].execute,
                    interval_seconds=1
                )
                return [TextContent(
                    type="text",
                    text="✓ Monitoring started! Checking targets every 1 second.\n"
                         "Alerts will trigger when you open monitored sites/apps."
                )]
            except Exception as e:
                return [TextContent(
                    type="text",
                    text=f"Error starting monitoring: {str(e)}"
                )]

        elif name == "stop_monitoring":
            try:
                components['stop_monitoring'].execute()
                return [TextContent(
                    type="text",
                    text="✓ Monitoring stopped. All alerts cleared."
                )]
            except Exception as e:
                return [TextContent(
                    type="text",
                    text=f"Error stopping monitoring: {str(e)}"
                )]

        elif name == "get_status":
            is_running = components['session'].is_running
            target_count = len(components['session'].get_all_targets())

            status = "🟢 RUNNING" if is_running else "🔴 STOPPED"
            result = f"Monitoring Status: {status}\n"
            result += f"Targets: {target_count}\n"

            if target_count > 0:
                result += "\nMonitored targets:\n"
                for target in components['session'].get_all_targets():
                    result += f"  • {target.name}\n"

            return [TextContent(type="text", text=result)]

        elif name == "check_now":
            try:
                components['check_targets'].execute()
                return [TextContent(
                    type="text",
                    text="✓ Manual check completed. Alerts triggered if targets are active."
                )]
            except Exception as e:
                return [TextContent(
                    type="text",
                    text=f"Error checking targets: {str(e)}"
                )]

        else:
            return [TextContent(
                type="text",
                text=f"Unknown tool: {name}"
            )]

    except Exception as e:
        import traceback
        error_details = traceback.format_exc()
        return [TextContent(
            type="text",
            text=f"Error executing {name}: {str(e)}\n\nDetails:\n{error_details}"
        )]


async def main():
    """Run the MCP server."""
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )


if __name__ == "__main__":
    print("[MCP Server] Starting FocusMotherFocus MCP Server...")
    asyncio.run(main())
