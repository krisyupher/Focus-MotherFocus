import os, json
from mcp_clients import NotifyMeMaybe
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	ncfg = cfg.get("notifymemaybe",{})
	if not ncfg.get("enabled", False): print("notify disabled"); return
	nm = NotifyMeMaybe(exe_path=ncfg.get("exe_path") or None)
	print("NotifyMeMaybe available:", nm.is_available())

if __name__ == "__main__":
	main()
