import shutil
import subprocess
import json
import os
import base64
from typing import Optional, Any, Dict, List
from urllib import request, parse, error


class WebcamMCP:
    """
    Wrapper for mcp-webcam: tries CLI executable names, otherwise uses HTTP API.
    Methods return parsed JSON or bytes for images.
    """
    def __init__(self, exe_path: Optional[str] = None, api_url: Optional[str] = None, timeout: int = 10):
        # Try common executable names if exe_path not provided
        self.exe = exe_path or shutil.which("mcp-webcam") or shutil.which("mcp_webcam") or \
                   shutil.which("webcam_mcp") or shutil.which("mcp-webcam.exe") or shutil.which("webcam_mcp.exe")
        self.api_url = (api_url or "").rstrip("/") if api_url else None
        self.timeout = timeout

    def is_available(self) -> bool:
        if self.exe:
            return True
        if self.api_url:
            try:
                req = request.Request(f"{self.api_url}/health", method="GET")
                with request.urlopen(req, timeout=self.timeout) as resp:
                    return resp.status == 200
            except Exception:
                return False
        return False

    def _run_json(self, args: List[str], timeout: int = 10) -> Any:
        if not self.exe:
            raise FileNotFoundError("mcp-webcam executable not found; set exe_path or provide api_url.")
        cmd = [self.exe] + args
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if p.returncode != 0:
            raise RuntimeError(f"{cmd} failed: {p.stderr.decode('utf-8', errors='ignore')}")
        txt = p.stdout.decode("utf-8", errors="ignore").strip()
        try:
            return json.loads(txt)
        except json.JSONDecodeError:
            return txt

    def _run_raw(self, args: List[str], timeout: int = 15) -> bytes:
        if not self.exe:
            raise FileNotFoundError("mcp-webcam executable not found; set exe_path or provide api_url.")
        cmd = [self.exe] + args
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if p.returncode != 0:
            raise RuntimeError(f"{cmd} failed: {p.stderr.decode('utf-8', errors='ignore')}")
        return p.stdout

    def list_devices(self) -> Any:
        # Try executable then HTTP endpoint
        if self.exe:
            candidates = [["list-devices"], ["devices"], ["list_devices"]]
            for c in candidates:
                try:
                    return {"command": c, "result": self._run_json(c)}
                except Exception:
                    continue
            raise RuntimeError("Failed to list devices via executable.")
        if self.api_url:
            try:
                url = f"{self.api_url}/devices"
                req = request.Request(url, method="GET")
                with request.urlopen(req, timeout=self.timeout) as resp:
                    txt = resp.read().decode("utf-8", errors="ignore")
                    try:
                        return json.loads(txt)
                    except Exception:
                        return txt
            except Exception as e:
                raise RuntimeError(f"HTTP list devices failed: {e}")
        raise RuntimeError("No backend available to list devices.")

    def capture_image(self, device: Optional[str] = None, resolution: Optional[str] = None, save_path: Optional[str] = None) -> bytes:
        # Try executable capture subcommands
        if self.exe:
            args = ["capture"]
            if device:
                args += ["--device", str(device)]
            if resolution:
                args += ["--resolution", resolution]
            # Many CLI tools write binary image to stdout
            try:
                raw = self._run_raw(args)
                if save_path:
                    os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
                    with open(save_path, "wb") as f:
                        f.write(raw)
                return raw
            except Exception as e:
                # fallthrough to http if available
                if not self.api_url:
                    raise
        # Try HTTP endpoint: /capture?device=...&resolution=...
        if self.api_url:
            params = {}
            if device:
                params["device"] = device
            if resolution:
                params["resolution"] = resolution
            url = f"{self.api_url}/capture"
            if params:
                url += "?" + parse.urlencode(params)
            try:
                req = request.Request(url, method="GET")
                with request.urlopen(req, timeout=self.timeout) as resp:
                    content_type = resp.headers.get("Content-Type", "")
                    raw = resp.read()
                    # Some servers return JSON with base64 field
                    if content_type.startswith("application/json"):
                        try:
                            js = json.loads(raw.decode("utf-8", errors="ignore"))
                            for key in ("image_base64", "png_base64", "jpg_base64", "image"):
                                if key in js:
                                    img = base64.b64decode(js[key])
                                    if save_path:
                                        os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
                                        with open(save_path, "wb") as f:
                                            f.write(img)
                                    return img
                        except Exception:
                            pass
                    # Otherwise treat as binary image
                    if save_path:
                        os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
                        with open(save_path, "wb") as f:
                            f.write(raw)
                    return raw
            except Exception as e:
                raise RuntimeError(f"HTTP capture failed: {e}")
        raise RuntimeError("No backend available to capture image.")
