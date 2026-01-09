import os, json, tempfile
from pathlib import Path
from mcp_clients import WindowsMCP
from ._utils import find_config

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	wcfg = cfg.get("windows_mcp",{})
	mcp = WindowsMCP(exe_path=wcfg.get("exe_path") or None)
	if not mcp.is_available(): print("WindowsMCP backend not found"); return
	print("Active window:", mcp.get_active_window())
	print("Running apps:", mcp.get_running_apps())
	tmp = Path(tempfile.gettempdir()) / "mcp_screenshot.png"
	data = mcp.capture_screenshot(save_path=str(tmp))
	print("Saved screenshot to", tmp, "bytes:", len(data))

if __name__ == "__main__":
	main()
