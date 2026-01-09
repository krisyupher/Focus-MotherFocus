import json
import os
import time
from typing import Any, Dict, Optional
from urllib import request, parse, error


class FilesystemMCP:
    def __init__(self, api_url: str = "http://localhost:5100", api_key: Optional[str] = None, timeout: int = 10):
        self.api_url = api_url.rstrip("/")
        self.api_key = api_key
        self.timeout = timeout

    def _url(self, path: str, params: Optional[Dict[str, str]] = None) -> str:
        url = f"{self.api_url}/{path.lstrip('/')}"
        if params:
            url += "?" + parse.urlencode(params)
        return url

    def _request(self, method: str, path: str, payload: Optional[Dict] = None, params: Optional[Dict] = None) -> Any:
        url = self._url(path, params)
        data = None
        headers = {"Accept": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = request.Request(url, data=data, headers=headers, method=method)
        try:
            with request.urlopen(req, timeout=self.timeout) as resp:
                raw = resp.read()
                if not raw:
                    return None
                try:
                    return json.loads(raw.decode("utf-8", errors="ignore"))
                except Exception:
                    return raw
        except error.HTTPError as e:
            raise RuntimeError(f"HTTP {e.code} {e.reason}: {e.read().decode(errors='ignore')}")
        except Exception as e:
            raise RuntimeError(f"Request failed: {e}")

    def is_available(self) -> bool:
        for p in ("/health", "/"):
            try:
                self._request("GET", p)
                return True
            except Exception:
                continue
        return False

    def secure_read(self, path: str) -> bytes:
        body = {"path": path}
        res = self._request("POST", "/files/read", payload=body)
        # Expect {"content_base64": "..."} or raw bytes
        if isinstance(res, dict) and "content_base64" in res:
            import base64
            return base64.b64decode(res["content_base64"])
        if isinstance(res, (bytes, bytearray)):
            return bytes(res)
        raise RuntimeError("Unexpected read response format")

    def secure_write(self, path: str, content: bytes, mode: str = "wb") -> Any:
        import base64
        body = {"path": path, "content_base64": base64.b64encode(content).decode("ascii")}
        body["mode"] = mode
        return self._request("POST", "/files/write", payload=body)

    def list_files(self, directory: str, recursive: bool = False) -> Any:
        params = {"path": directory, "recursive": "1" if recursive else "0"}
        return self._request("GET", "/files/list", params=params)

    def audit_event(self, event_type: str, details: Dict) -> Any:
        body = {"type": event_type, "details": details, "timestamp": time.time()}
        return self._request("POST", "/audit", payload=body)

    def export_snapshot(self, save_path: Optional[str] = None) -> bytes:
        url = self._url("/export")
        req = request.Request(url, headers={"Authorization": f"Bearer {self.api_key}"} if self.api_key else {})
        try:
            with request.urlopen(req, timeout=self.timeout) as resp:
                data = resp.read()
                if save_path:
                    os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
                    with open(save_path, "wb") as f:
                        f.write(data)
                return data
        except Exception as e:
            raise RuntimeError(f"Export failed: {e}")
