import shutil, subprocess, platform, os
from typing import Optional

class MCPServerNotify:
	def __init__(self, exe_path: Optional[str] = None):
		self.exe = exe_path or shutil.which("mcp_server_notify") or shutil.which("mcp_server_notify.exe")
		self.platform = platform.system()

	def is_available(self) -> bool:
		if self.exe: return True
		if self.platform == "Darwin": return True
		if self.platform == "Linux": return shutil.which("notify-send") is not None
		if self.platform == "Windows": return True
		return False

	def send_notification(self, title: str, message: str, sound: Optional[str] = None, icon: Optional[str] = None, timeout: int = 5) -> bool:
		if self.exe:
			candidates = [[self.exe,"notify","--title",title,"--message",message,"--timeout",str(timeout)]]
			if sound: candidates = [[*c,"--sound",sound] for c in candidates] + candidates
			if icon: candidates = [[*c,"--icon",icon] for c in candidates] + candidates
			for cmd in candidates:
				try:
					subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE); return True
				except Exception:
					continue
		try:
			if self.platform == "Darwin":
				scmd = ['osascript','-e',f'display notification "{message}" with title "{title}"']
				if sound: scmd = ['osascript','-e',f'display notification "{message}" with title "{title}" sound name "{sound}"']
				subprocess.run(scmd, check=True); return True
			if self.platform == "Linux":
				subprocess.run(["notify-send", title, message], check=False)
				if sound:
					for player in ("paplay","aplay","canberra-gtk-play"):
						p = shutil.which(player)
						if p:
							if player == "canberra-gtk-play":
								subprocess.run([p,"--id",sound], check=False)
							else:
								subprocess.run([p,sound], check=False)
							break
				return True
			if self.platform == "Windows":
				ps = r"""[void][System.Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms')
$notify = New-Object System.Windows.Forms.NotifyIcon
$notify.Icon = [System.Drawing.SystemIcons]::Information
$notify.BalloonTipTitle = '{title}'
$notify.BalloonTipText = '{message}'
$notify.Visible = $true
$notify.ShowBalloonTip({timeout_ms})
Start-Sleep -Seconds 3
$notify.Dispose()
""".format(title=title.replace("'","''"), message=message.replace("'","''"), timeout_ms=int(timeout*1000))
				subprocess.run(["powershell","-NoProfile","-Command",ps], check=False)
				if sound and os.path.exists(sound):
					try: subprocess.run(["powershell","-NoProfile","-Command",f'(New-Object Media.SoundPlayer "{sound}").PlaySync();'], check=False)
					except Exception: pass
				return True
		except Exception:
			return False
		return False
