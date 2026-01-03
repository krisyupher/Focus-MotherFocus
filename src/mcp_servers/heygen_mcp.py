import json
import os
from urllib import request, parse, error
from typing import Any, Dict, Optional


class HeyGenMCP:
    def __init__(self, api_url: str = "http://localhost:5300", api_key: Optional[str] = None, timeout: int = 30):
        self.api_url = api_url.rstrip("/")
        self.api_key = api_key
        self.timeout = timeout

    def _url(self, path: str, params: Optional[Dict[str, str]] = None) -> str:
        url = f"{self.api_url}/{path.lstrip('/')}"
        if params:
            url += "?" + parse.urlencode(params)
        return url

    def _request(self, method: str, path: str, payload: Optional[Dict] = None, expect_binary: bool = False) -> Any:
        url = self._url(path)
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
                ctype = resp.headers.get("Content-Type", "")
                if expect_binary or ctype.startswith("video/") or ctype == "application/octet-stream":
                    return raw
                try:
                    return json.loads(raw.decode("utf-8", errors="ignore"))
                except Exception:
                    return raw.decode("utf-8", errors="ignore")
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

    def list_avatars(self) -> Any:
        candidates = ["/avatars", "/v1/avatars", "/api/avatars"]
        for c in candidates:
            try:
                return self._request("GET", c)
            except Exception:
                continue
        raise RuntimeError("Failed to list avatars; check server/endpoint")

    def generate_video_from_text(self, text: str, avatar: Optional[str] = None, style: Optional[str] = None, video_opts: Optional[Dict] = None) -> Dict:
        # Try a couple endpoint patterns; expect JSON with job_id or video_id
        payload = {"text": text}
        if avatar:
            payload["avatar"] = avatar
        if style:
            payload["style"] = style
        if video_opts:
            payload["video"] = video_opts

        candidates = ["/generate", "/v1/generate", "/video/generate"]
        for c in candidates:
            try:
                return self._request("POST", c, payload=payload)
            except Exception:
                continue
        raise RuntimeError("Failed to start generation job")

    def get_status(self, job_id: str) -> Any:
        candidates = [f"/status/{parse.quote(job_id)}", f"/v1/status/{parse.quote(job_id)}", f"/jobs/{parse.quote(job_id)}"]
        for c in candidates:
            try:
                return self._request("GET", c)
            except Exception:
                continue
        raise RuntimeError("Failed to get job status")

    def download_video(self, video_id: str, save_path: Optional[str] = None) -> bytes:
        candidates = [f"/download/{parse.quote(video_id)}", f"/videos/{parse.quote(video_id)}/download"]
        for c in candidates:
            try:
                data = self._request("GET", c, expect_binary=True)
                if save_path:
                    os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
                    with open(save_path, "wb") as f:
                        f.write(data)
                return data
            except Exception:
                continue
        raise RuntimeError("Failed to download video")
