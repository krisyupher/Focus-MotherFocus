import shutil
import subprocess
import json
import base64
import os
from typing import Optional, Any, Dict, List


class BrowserToolsMCP:
    """
    Minimal wrapper for Browser-Tools-MCP CLI.
    - Pass exe_path or ensure executable is on PATH.
    - Methods return parsed JSON or raw bytes (screenshots).
    """
    def __init__(self, exe_path: Optional[str] = None):
        self.exe = exe_path or shutil.which("browser-tools-mcp") or shutil.which("browser-tools-mcp.exe")
        if self.exe:
            self.exe = os.path.expanduser(self.exe)

    def is_available(self) -> bool:
        return bool(self.exe)

    def _run_json(self, args: List[str], timeout: int = 10) -> Any:
        if not self.exe:
            raise FileNotFoundError("browser-tools-mcp executable not found; set exe_path or put it on PATH.")
        cmd = [self.exe] + args
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if p.returncode != 0:
            raise RuntimeError(f"{cmd} failed: {p.stderr.decode(errors='ignore')}")
        txt = p.stdout.decode("utf-8", errors="ignore").strip()
        try:
            return json.loads(txt)
        except json.JSONDecodeError:
            return txt

    def _run_raw(self, args: List[str], timeout: int = 15) -> bytes:
        if not self.exe:
            raise FileNotFoundError("browser-tools-mcp executable not found; set exe_path or put it on PATH.")
        cmd = [self.exe] + args
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if p.returncode != 0:
            raise RuntimeError(f"{cmd} failed: {p.stderr.decode(errors='ignore')}")
        return p.stdout

    def list_tabs(self) -> Any:
        candidates = [["list-tabs"], ["tabs"], ["list_tabs"]]
        for c in candidates:
            try:
                return {"command": c, "result": self._run_json(c)}
            except Exception:
                continue
        raise RuntimeError("Failed to list tabs; check executable or subcommands.")

    def get_console_logs(self, tab_id: str, since: Optional[int] = None) -> Any:
        args = ["console-logs", tab_id] if tab_id else ["console-logs"]
        if since:
            args += ["--since", str(since)]
        return self._run_json(args)

    def get_network_activity(self, tab_id: str, since: Optional[int] = None) -> Any:
        args = ["network-activity", tab_id] if tab_id else ["network-activity"]
        if since:
            args += ["--since", str(since)]
        return self._run_json(args)

    def capture_tab_screenshot(self, tab_id: Optional[str] = None, save_path: Optional[str] = None) -> bytes:
        candidates = [["capture-screenshot"], ["tab-screenshot"], ["screenshot"]]
        for base in candidates:
            args = base + ([tab_id] if tab_id else [])
            try:
                raw = self._run_raw(args)
                # Try parse as JSON with base64 image
                try:
                    txt = raw.decode("utf-8", errors="ignore").strip()
                    js = json.loads(txt)
                    for k in ("png_base64", "image_base64", "screenshot_base64"):
                        if k in js:
                            img = base64.b64decode(js[k])
                            if save_path:
                                with open(save_path, "wb") as f:
                                    f.write(img)
                            return img
                except Exception:
                    # raw might already be PNG bytes
                    if save_path:
                        with open(save_path, "wb") as f:
                            f.write(raw)
                    return raw
            except Exception:
                continue
        raise RuntimeError("Failed to capture screenshot; check executable/subcommands.")
