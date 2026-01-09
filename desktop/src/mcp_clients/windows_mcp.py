import shutil, subprocess, json, base64, os
from typing import Optional, Any, Dict, List

class WindowsMCP:
	def __init__(self, exe_path: Optional[str] = None):
		self.exe = exe_path or shutil.which("MCPGet") or shutil.which("MCPGet.exe")
		if self.exe:
			self.exe = os.path.expanduser(self.exe)

	def is_available(self) -> bool:
		return bool(self.exe)

	def _run_json(self, args: List[str], timeout: int = 10) -> Any:
		if not self.exe:
			raise FileNotFoundError("MCPGet executable not found.")
		cmd = [self.exe] + args
		p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
		if p.returncode != 0:
			raise RuntimeError(p.stderr.decode("utf-8", errors="replace"))
		out = p.stdout.decode("utf-8", errors="replace").strip()
		try:
			return json.loads(out)
		except json.JSONDecodeError:
			return out

	def _run_raw(self, args: List[str], timeout: int = 10) -> bytes:
		if not self.exe:
			raise FileNotFoundError("MCPGet executable not found.")
		cmd = [self.exe] + args
		p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
		if p.returncode != 0:
			raise RuntimeError(p.stderr.decode("utf-8", errors="replace"))
		return p.stdout

	def get_active_window(self) -> Dict[str, Any]:
		for c in [["active-window"], ["get-active-window"], ["window","active"]]:
			try:
				return {"command": c, "result": self._run_json(c)}
			except Exception:
				continue
		raise RuntimeError("Failed to get active window.")

	def get_running_apps(self) -> Dict[str, Any]:
		for c in [["process-list"], ["running-apps"], ["processes"]]:
			try:
				return {"command": c, "result": self._run_json(c)}
			except Exception:
				continue
		raise RuntimeError("Failed to get running apps.")

	def get_ui_elements(self, target: Optional[str] = None) -> Dict[str, Any]:
		for base in [["ui-tree"], ["uia-tree"], ["ui"]]:
			args = base + ([target] if target else [])
			try:
				return {"command": args, "result": self._run_json(args)}
			except Exception:
				continue
		raise RuntimeError("Failed to collect UI elements.")

	def capture_screenshot(self, save_path: Optional[str] = None) -> bytes:
		for c in [["screenshot"], ["capture-screenshot"], ["screencap"]]:
			try:
				raw = self._run_raw(c)
				try:
					js = json.loads(raw.decode("utf-8", errors="ignore"))
					for key in ("png_base64","image_base64","screenshot_base64"):
						if key in js:
							png = base64.b64decode(js[key])
							if save_path:
								with open(save_path,"wb") as f: f.write(png)
							return png
				except Exception:
					if save_path:
						with open(save_path,"wb") as f: f.write(raw)
					return raw
			except Exception:
				continue
		raise RuntimeError("Failed to capture screenshot.")
