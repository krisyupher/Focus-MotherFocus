import shutil, subprocess, json, base64, os
from typing import Optional, Any, List

class BrowserToolsMCP:
	def __init__(self, exe_path: Optional[str] = None):
		self.exe = exe_path or shutil.which("browser-tools-mcp") or shutil.which("browser-tools-mcp.exe")
		if self.exe: self.exe = os.path.expanduser(self.exe)

	def is_available(self) -> bool:
		return bool(self.exe)

	def _run_json(self, args: List[str], timeout: int = 10):
		if not self.exe: raise FileNotFoundError("browser-tools-mcp not found")
		cmd=[self.exe]+args
		p=subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
		if p.returncode!=0: raise RuntimeError(p.stderr.decode(errors='ignore'))
		txt=p.stdout.decode("utf-8", errors="ignore").strip()
		try: return json.loads(txt)
		except json.JSONDecodeError: return txt

	def _run_raw(self,args:List[str],timeout:int=15):
		if not self.exe: raise FileNotFoundError("browser-tools-mcp not found")
		cmd=[self.exe]+args
		p=subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
		if p.returncode!=0: raise RuntimeError(p.stderr.decode(errors='ignore'))
		return p.stdout

	def list_tabs(self): 
		for c in [["list-tabs"],["tabs"],["list_tabs"]]:
			try: return {"command":c,"result":self._run_json(c)}
			except Exception: continue
		raise RuntimeError("list tabs failed")

	def get_console_logs(self, tab_id: str, since: Optional[int]=None): 
		args = ["console-logs", tab_id] if tab_id else ["console-logs"]
		if since: args += ["--since", str(since)]
		return self._run_json(args)

	def get_network_activity(self, tab_id: str, since: Optional[int]=None):
		args = ["network-activity", tab_id] if tab_id else ["network-activity"]
		if since: args += ["--since", str(since)]
		return self._run_json(args)

	def capture_tab_screenshot(self, tab_id: Optional[str]=None, save_path: Optional[str]=None):
		for base in [["capture-screenshot"],["tab-screenshot"],["screenshot"]]:
			args = base + ([tab_id] if tab_id else [])
			try:
				raw = self._run_raw(args)
				try:
					txt = raw.decode("utf-8",errors="ignore").strip()
					js = json.loads(txt)
					for k in ("png_base64","image_base64","screenshot_base64"):
						if k in js:
							img = base64.b64decode(js[k])
							if save_path:
								with open(save_path,"wb") as f: f.write(img)
							return img
				except Exception:
					if save_path:
						with open(save_path,"wb") as f: f.write(raw)
					return raw
			except Exception:
				continue
		raise RuntimeError("capture screenshot failed")
