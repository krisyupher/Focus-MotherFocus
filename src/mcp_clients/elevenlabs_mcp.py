import json, base64, os
from typing import Any, Dict, Optional
from urllib import request, parse, error

class ElevenLabsMCP:
	def __init__(self, api_url: str = "http://localhost:5200", api_key: Optional[str] = None, timeout: int = 15):
		self.api_url = api_url.rstrip("/")
		self.api_key = api_key
		self.timeout = timeout

	def _url(self, path: str, params: Optional[Dict[str,str]] = None) -> str:
		url = f"{self.api_url}/{path.lstrip('/')}"
		if params: url += "?" + parse.urlencode(params)
		return url

	def _request(self, method: str, path: str, payload: Optional[Dict] = None, params: Optional[Dict] = None, expect_binary: bool = False, extra_headers: Optional[Dict[str,str]] = None) -> Any:
		url = self._url(path, params); data=None; headers={"Accept":"application/json"}
		if self.api_key: headers["Authorization"]=f"Bearer {self.api_key}"
		if extra_headers: headers.update(extra_headers)
		if payload is not None:
			data = json.dumps(payload).encode("utf-8"); headers["Content-Type"]="application/json"
		req = request.Request(url, data=data, headers=headers, method=method)
		try:
			with request.urlopen(req, timeout=self.timeout) as resp:
				ct = resp.headers.get("Content-Type","")
				raw = resp.read()
				if expect_binary or ct.startswith("audio/") or ct=="application/octet-stream": return raw
				try: return json.loads(raw.decode("utf-8", errors="ignore"))
				except Exception: return raw.decode("utf-8", errors="ignore")
		except error.HTTPError as e:
			raise RuntimeError(f"HTTP {e.code} {e.reason}: {e.read().decode(errors='ignore')}")
		except Exception as e:
			raise RuntimeError(f"Request failed: {e}")

	def is_available(self) -> bool:
		for p in ("/health","/"):
			try: self._request("GET", p); return True
			except Exception: continue
		return False

	def list_voices(self) -> Any:
		for c in ("/voices","/v1/voices","/api/voices"):
			try: return self._request("GET", c)
			except Exception: continue
		raise RuntimeError("Failed to list voices")

	def synthesize_text(self, text: str, voice: Optional[str] = None, fmt: str = "mp3", save_path: Optional[str] = None) -> bytes:
		candidates = []
		if voice:
			candidates.append((f"/v1/text-to-speech/{voice}", {"Accept": f"audio/{fmt}"}))
		candidates += [("/synthesize", None), ("/tts", None), (f"/v1/text-to-speech/{voice or ''}", {"Accept": f"audio/{fmt}"})]
		for path, headers in candidates:
			try:
				payload={"text": text}
				if voice: payload["voice"] = voice
				if headers is None: headers={"Accept": f"audio/{fmt}"}
				raw = self._request("POST", path, payload=payload, expect_binary=True, extra_headers=headers)
				if isinstance(raw, (bytes,bytearray)):
					if save_path:
						os.makedirs(os.path.dirname(save_path) or ".", exist_ok=True)
						with open(save_path,"wb") as f: f.write(raw)
					return bytes(raw)
			except Exception:
				continue
		raise RuntimeError("Text-to-speech failed")

	def clone_voice(self, name: str, sample_audio_path: str) -> Any:
		if not os.path.exists(sample_audio_path): raise FileNotFoundError(sample_audio_path)
		with open(sample_audio_path,"rb") as f: b=f.read()
		payload={"name": name, "audio_base64": base64.b64encode(b).decode("ascii")}
		for c in ("/voices/clone","/voices","/v1/voices/clone"):
			try: return self._request("POST", c, payload=payload)
			except Exception: continue
		raise RuntimeError("clone_voice failed")
