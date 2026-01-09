import json, os
from mcp_clients import MCPServerNotify
from ._utils import find_config

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	ncfg = cfg.get("mcp_server_notify",{})
	if not ncfg.get("enabled", False): print("notify disabled"); return
	notify = MCPServerNotify(exe_path=ncfg.get("exe_path") or None)
	print("Available:", notify.is_available())
	if ncfg.get("test_notify_on_start", False):
		ok = notify.send_notification("FocusMotherFocus","Test initialization")
		print("test notification sent:", ok)

if __name__ == "__main__":
	main()
