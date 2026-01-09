import json
import os
import time
from typing import Any, Dict, Optional
from urllib import request, parse, error


class MemoryMCP:
    def __init__(self, api_url: str = "http://localhost:5000", api_key: Optional[str] = None, timeout: int = 10):
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
        # Try /health then /
        for p in ("/health", "/"):
            try:
                r = self._request("GET", p)
                return True
            except Exception:
                continue
        return False

    def add_event(self, event_type: str, payload: Dict, timestamp: Optional[float] = None) -> Any:
        ts = timestamp or time.time()
        body = {"type": event_type, "payload": payload, "timestamp": ts}
        return self._request("POST", "/events", payload=body)

    def query_patterns(self, q: Optional[str] = None, limit: int = 50) -> Any:
        params = {"q": q or "", "limit": str(limit)}
        return self._request("GET", "/patterns", params=params)

    def get_node(self, node_id: str) -> Any:
        return self._request("GET", f"/nodes/{parse.quote(node_id)}")

    def create_node(self, properties: Dict) -> Any:
        return self._request("POST", "/nodes", payload=properties)

    def export_graph(self, save_path: Optional[str] = None) -> bytes:
        # Expect binary or JSON export from /export
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
