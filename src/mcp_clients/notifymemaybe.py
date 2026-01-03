import shutil, subprocess, json
from typing import List, Optional

class NotifyMeMaybe:
	def __init__(self, exe_path: Optional[str] = None):
		self.exe = exe_path or shutil.which("notifymemaybe") or shutil.which("notifymemaybe.exe")

	def is_available(self) -> bool:
		return bool(self.exe)

	def _run(self, args: List[str], timeout: int = 30) -> str:
		if not self.exe: raise FileNotFoundError("notifymemaybe not found")
		cmd=[self.exe]+args
		p=subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
		if p.returncode!=0: raise RuntimeError(p.stderr.decode("utf-8", errors="ignore"))
		return p.stdout.decode("utf-8", errors="ignore").strip()

	def prompt_confirm(self, title: str, message: str, yes_label: str = "Yes", no_label: str = "No", timeout: Optional[int] = None) -> bool:
		if self.exe:
			args=["confirm","--title",title,"--message",message,"--yes",yes_label,"--no",no_label]
			if timeout: args += ["--timeout", str(timeout)]
			try:
				out = self._run(args, timeout=timeout or 30)
				try:
					j = json.loads(out); return bool(j.get("result"))
				except Exception:
					return out.lower().startswith(yes_label.lower()[0])
			except Exception:
				pass
		# fallback console
		resp = input(f"{title}\n{message}\n[{yes_label}/{no_label}]: ").strip().lower()
		return resp.startswith(yes_label.lower()[0])

	def prompt_select(self, title: str, message: str, choices: List[str], timeout: Optional[int] = None) -> Optional[str]:
		if not choices: return None
		if self.exe:
			args=["select","--title",title,"--message",message,"--choices", json.dumps(choices)]
			if timeout: args += ["--timeout", str(timeout)]
			try:
				out = self._run(args, timeout=timeout or 30)
				try:
					j = json.loads(out); return j.get("selection")
				except Exception:
					sel = out.strip()
					if sel.isdigit():
						idx = int(sel)
						if 0<=idx<len(choices): return choices[idx]
					return sel or None
			except Exception:
				pass
		print(title); print(message)
		for i,c in enumerate(choices): print(f"{i}: {c}")
		resp = input("Select index: ").strip()
		if resp.isdigit(): idx = int(resp); return choices[idx] if 0<=idx<len(choices) else None
		return None

	def prompt_input(self, title: str, message: str, placeholder: str = "", timeout: Optional[int] = None) -> Optional[str]:
		if self.exe:
			args=["input","--title",title,"--message",message]
			if placeholder: args += ["--placeholder", placeholder]
			if timeout: args += ["--timeout", str(timeout)]
			try:
				out = self._run(args, timeout=timeout or 30)
				try: j = json.loads(out); return j.get("value")
				except Exception: return out or None
			except Exception:
				pass
		return input(f"{title}\n{message}\n> ") or None
