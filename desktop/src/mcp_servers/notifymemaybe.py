import shutil
import subprocess
import json
import os
from typing import List, Optional, Any


class NotifyMeMaybe:
    """
    Wrapper for notifymemaybe CLI with console fallback.
    Methods: is_available(), prompt_confirm(), prompt_select(), prompt_input()
    """
    def __init__(self, exe_path: Optional[str] = None):
        self.exe = exe_path or shutil.which("notifymemaybe") or shutil.which("notifymemaybe.exe")

    def is_available(self) -> bool:
        return bool(self.exe)

    def _run(self, args: List[str], timeout: int = 30) -> str:
        if not self.exe:
            raise FileNotFoundError("notifymemaybe executable not found.")
        cmd = [self.exe] + args
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        if p.returncode != 0:
            raise RuntimeError(f"{cmd} failed: {p.stderr.decode('utf-8', errors='ignore')}")
        return p.stdout.decode("utf-8", errors="ignore").strip()

    def prompt_confirm(self, title: str, message: str, yes_label: str = "Yes", no_label: str = "No", timeout: Optional[int] = None) -> bool:
        # Try CLI first
        if self.exe:
            args = ["confirm", "--title", title, "--message", message, "--yes", yes_label, "--no", no_label]
            if timeout:
                args += ["--timeout", str(timeout)]
            try:
                out = self._run(args, timeout=timeout or 30)
                # Try parse JSON {"result": true} or plain "yes"/"no"
                try:
                    j = json.loads(out)
                    return bool(j.get("result"))
                except Exception:
                    return out.lower().startswith(yes_label.lower()[0])
            except Exception:
                pass
        # Fallback to console prompt
        prompt = f"{title}\n{message}\n[{yes_label}/{no_label}]: "
        try:
            resp = input(prompt).strip().lower()
            return resp.startswith(yes_label.lower()[0])
        except Exception:
            return False

    def prompt_select(self, title: str, message: str, choices: List[str], timeout: Optional[int] = None) -> Optional[str]:
        if not choices:
            return None
        if self.exe:
            args = ["select", "--title", title, "--message", message, "--choices", json.dumps(choices)]
            if timeout:
                args += ["--timeout", str(timeout)]
            try:
                out = self._run(args, timeout=timeout or 30)
                try:
                    j = json.loads(out)
                    return j.get("selection")
                except Exception:
                    # assume plain text of selected index or value
                    sel = out.strip()
                    if sel.isdigit():
                        idx = int(sel)
                        if 0 <= idx < len(choices):
                            return choices[idx]
                    return sel or None
            except Exception:
                pass
        # Console fallback: enumerate and ask
        print(title)
        print(message)
        for i, c in enumerate(choices):
            print(f"{i}: {c}")
        try:
            resp = input("Select index: ").strip()
            if resp.isdigit():
                idx = int(resp)
                if 0 <= idx < len(choices):
                    return choices[idx]
            return None
        except Exception:
            return None

    def prompt_input(self, title: str, message: str, placeholder: str = "", timeout: Optional[int] = None) -> Optional[str]:
        if self.exe:
            args = ["input", "--title", title, "--message", message]
            if placeholder:
                args += ["--placeholder", placeholder]
            if timeout:
                args += ["--timeout", str(timeout)]
            try:
                out = self._run(args, timeout=timeout or 30)
                try:
                    j = json.loads(out)
                    return j.get("value")
                except Exception:
                    return out or None
            except Exception:
                pass
        try:
            return input(f"{title}\n{message}\n> ") or None
        except Exception:
            return None
