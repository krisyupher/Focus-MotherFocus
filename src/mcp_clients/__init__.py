# Re-export client classes for stable imports: from mcp_clients import WindowsMCP, MemoryMCP, ...
__all__ = [
    "WindowsMCP","MCPServerNotify","BrowserToolsMCP","MemoryMCP","MemoryKB",
    "FilesystemMCP","WebcamMCP","ElevenLabsMCP","HeyGenMCP","NotifyMeMaybe"
]

from .windows_mcp import WindowsMCP
from .mcp_server_notify import MCPServerNotify
from .browser_tools_mcp import BrowserToolsMCP
from .memory_mcp import MemoryMCP
from .memory_kb import MemoryKB
from .filesystem_mcp import FilesystemMCP
from .webcam_mcp import WebcamMCP
from .elevenlabs_mcp import ElevenLabsMCP
from .heygen_mcp import HeyGenMCP
from .notifymemaybe import NotifyMeMaybe
