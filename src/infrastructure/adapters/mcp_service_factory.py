"""
MCP Service Factory.

Automatically discovers, initializes, and registers all available MCP services.
"""
from typing import Optional, Dict, Any
import os
import json

from src.application.interfaces.i_mcp_service_registry import (
    ServiceType,
    ServiceCapability
)
from src.infrastructure.adapters.mcp_service_registry import MCPServiceRegistry

# Import all MCP wrappers - gracefully handle missing modules
try:
    from src.mcp_servers.browser_tools_mcp import BrowserToolsMCP
except (ImportError, RuntimeError):
    BrowserToolsMCP = None

try:
    from src.mcp_servers.webcam_mcp import WebcamMCP
except (ImportError, RuntimeError):
    WebcamMCP = None

try:
    from src.mcp_servers.heygen_mcp import HeyGenMCP
except (ImportError, RuntimeError):
    HeyGenMCP = None

try:
    from src.mcp_servers.elevenlabs_mcp import ElevenLabsMCP
except (ImportError, RuntimeError):
    ElevenLabsMCP = None

try:
    from src.mcp_servers.memory_mcp import MemoryMCP
except (ImportError, RuntimeError):
    MemoryMCP = None

try:
    from src.mcp_servers.filesystem_mcp import FilesystemMCP
except (ImportError, RuntimeError):
    FilesystemMCP = None

try:
    from src.mcp_servers.windows_mcp import WindowsMCP
except (ImportError, RuntimeError):
    WindowsMCP = None

try:
    from src.mcp_servers.notifymemaybe import NotifyMeMaybe
except (ImportError, RuntimeError):
    NotifyMeMaybe = None

try:
    from src.infrastructure.adapters.playwright_browser_controller import PlaywrightBrowserController
except (ImportError, RuntimeError):
    PlaywrightBrowserController = None


class MCPServiceFactory:
    """
    Factory for discovering and initializing MCP services.

    Features:
    - Auto-discovery from config
    - Lazy initialization
    - Capability registration
    - Error handling and fallbacks
    """

    # Service capability definitions
    CAPABILITIES = {
        ServiceType.BROWSER_TOOLS: [
            ServiceCapability(
                name="detect_browser_tabs",
                description="Detect open browser tabs and URLs",
                parameters=["url_pattern"],
                fallback_services=[]
            ),
            ServiceCapability(
                name="get_active_tab",
                description="Get currently active browser tab",
                parameters=[],
                fallback_services=[]
            )
        ],
        ServiceType.WEBCAM: [
            ServiceCapability(
                name="capture_frame",
                description="Capture single frame from webcam",
                parameters=[],
                fallback_services=[]
            ),
            ServiceCapability(
                name="start_stream",
                description="Start webcam video stream",
                parameters=[],
                fallback_services=[]
            )
        ],
        ServiceType.HEYGEN: [
            ServiceCapability(
                name="generate_avatar",
                description="Generate animated avatar video",
                parameters=["text", "avatar_id"],
                fallback_services=[]
            )
        ],
        ServiceType.ELEVENLABS: [
            ServiceCapability(
                name="synthesize_speech",
                description="Convert text to speech",
                parameters=["text", "voice"],
                fallback_services=[ServiceType.WINDOWS]  # Windows TTS fallback
            )
        ],
        ServiceType.MEMORY: [
            ServiceCapability(
                name="store_event",
                description="Store event in memory",
                parameters=["event_data"],
                fallback_services=[ServiceType.FILESYSTEM]
            ),
            ServiceCapability(
                name="query_events",
                description="Query stored events",
                parameters=["query"],
                fallback_services=[ServiceType.FILESYSTEM]
            )
        ],
        ServiceType.FILESYSTEM: [
            ServiceCapability(
                name="read_file",
                description="Read file contents",
                parameters=["path"],
                fallback_services=[]
            ),
            ServiceCapability(
                name="write_file",
                description="Write file contents",
                parameters=["path", "content"],
                fallback_services=[]
            )
        ],
        ServiceType.WINDOWS: [
            ServiceCapability(
                name="synthesize_speech",
                description="Windows TTS speech synthesis",
                parameters=["text"],
                fallback_services=[]
            ),
            ServiceCapability(
                name="show_notification",
                description="Show Windows notification",
                parameters=["title", "message"],
                fallback_services=[]
            )
        ],
        ServiceType.NOTIFY: [
            ServiceCapability(
                name="interactive_prompt",
                description="Show interactive notification dialog",
                parameters=["message", "options"],
                fallback_services=[]
            )
        ],
        ServiceType.PLAYWRIGHT: [
            ServiceCapability(
                name="close_browser_tab",
                description="Close browser tab by URL",
                parameters=["url"],
                fallback_services=[]
            ),
            ServiceCapability(
                name="control_browser",
                description="Full browser automation control",
                parameters=["commands"],
                fallback_services=[]
            )
        ]
    }

    def __init__(self, config_path: str = "config/mcp_client_config.json"):
        """
        Initialize service factory.

        Args:
            config_path: Path to MCP client configuration file
        """
        self.config_path = config_path
        self.config = self._load_config()

    def create_registry(self) -> MCPServiceRegistry:
        """
        Create and populate service registry with all available services.

        Returns:
            Populated service registry
        """
        registry = MCPServiceRegistry(health_check_interval=30.0)

        # Register all available services
        self._register_browser_tools(registry)
        self._register_webcam(registry)
        self._register_heygen(registry)
        self._register_elevenlabs(registry)
        self._register_memory(registry)
        self._register_filesystem(registry)
        self._register_windows(registry)
        self._register_notify(registry)
        # Playwright disabled - causes issues, tab auto-close not critical
        # self._register_playwright(registry)

        return registry

    def _load_config(self) -> Dict[str, Any]:
        """Load MCP client configuration."""
        if not os.path.exists(self.config_path):
            print(f"[Factory] Config not found: {self.config_path}")
            return {}

        try:
            with open(self.config_path, 'r') as f:
                return json.load(f)
        except Exception as e:
            print(f"[Factory] Error loading config: {e}")
            return {}

    def _register_browser_tools(self, registry: MCPServiceRegistry) -> None:
        """Register Browser Tools MCP if available."""
        if BrowserToolsMCP is None:
            return

        try:
            service = BrowserToolsMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.BROWSER_TOOLS,
                    service,
                    self.CAPABILITIES[ServiceType.BROWSER_TOOLS]
                )
        except Exception as e:
            print(f"[Factory] Failed to register Browser Tools: {e}")

    def _register_webcam(self, registry: MCPServiceRegistry) -> None:
        """Register Webcam MCP if available."""
        if WebcamMCP is None:
            return

        try:
            service = WebcamMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.WEBCAM,
                    service,
                    self.CAPABILITIES[ServiceType.WEBCAM]
                )
        except Exception as e:
            print(f"[Factory] Failed to register Webcam: {e}")

    def _register_heygen(self, registry: MCPServiceRegistry) -> None:
        """Register HeyGen MCP if available."""
        if HeyGenMCP is None:
            return

        try:
            service = HeyGenMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.HEYGEN,
                    service,
                    self.CAPABILITIES[ServiceType.HEYGEN]
                )
        except Exception as e:
            print(f"[Factory] Failed to register HeyGen: {e}")

    def _register_elevenlabs(self, registry: MCPServiceRegistry) -> None:
        """Register ElevenLabs MCP if available."""
        if ElevenLabsMCP is None:
            return

        try:
            service = ElevenLabsMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.ELEVENLABS,
                    service,
                    self.CAPABILITIES[ServiceType.ELEVENLABS]
                )
        except Exception as e:
            print(f"[Factory] Failed to register ElevenLabs: {e}")

    def _register_memory(self, registry: MCPServiceRegistry) -> None:
        """Register Memory MCP if available."""
        if MemoryMCP is None:
            return

        try:
            service = MemoryMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.MEMORY,
                    service,
                    self.CAPABILITIES[ServiceType.MEMORY]
                )
        except Exception as e:
            print(f"[Factory] Failed to register Memory: {e}")

    def _register_filesystem(self, registry: MCPServiceRegistry) -> None:
        """Register Filesystem MCP if available."""
        if FilesystemMCP is None:
            return

        try:
            service = FilesystemMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.FILESYSTEM,
                    service,
                    self.CAPABILITIES[ServiceType.FILESYSTEM]
                )
        except Exception as e:
            print(f"[Factory] Failed to register Filesystem: {e}")

    def _register_windows(self, registry: MCPServiceRegistry) -> None:
        """Register Windows MCP if available."""
        if WindowsMCP is None:
            return

        try:
            service = WindowsMCP()
            if service.is_available():
                registry.register_service(
                    ServiceType.WINDOWS,
                    service,
                    self.CAPABILITIES[ServiceType.WINDOWS]
                )
        except Exception as e:
            print(f"[Factory] Failed to register Windows: {e}")

    def _register_notify(self, registry: MCPServiceRegistry) -> None:
        """Register NotifyMeMaybe if available."""
        if NotifyMeMaybe is None:
            return

        try:
            service = NotifyMeMaybe()
            if service.is_available():
                registry.register_service(
                    ServiceType.NOTIFY,
                    service,
                    self.CAPABILITIES[ServiceType.NOTIFY]
                )
        except Exception as e:
            print(f"[Factory] Failed to register NotifyMeMaybe: {e}")

    def _register_playwright(self, registry: MCPServiceRegistry) -> None:
        """Register Playwright browser controller if available."""
        if PlaywrightBrowserController is None:
            return

        try:
            service = PlaywrightBrowserController()
            # Playwright availability check is async, so we'll register it anyway
            registry.register_service(
                ServiceType.PLAYWRIGHT,
                service,
                self.CAPABILITIES[ServiceType.PLAYWRIGHT]
            )
        except Exception as e:
            print(f"[Factory] Failed to register Playwright: {e}")
