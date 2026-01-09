import os, json
from pathlib import Path
from mcp_clients import BrowserToolsMCP
from ._utils import find_config
from datetime import datetime

def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	bcfg = cfg.get("browser_tools_mcp",{})
	if not bcfg.get("enabled", False): print("browser_tools_mcp disabled"); return
	bt = BrowserToolsMCP(exe_path=bcfg.get("exe_path") or None)
	if not bt.is_available(): print("backend not available"); return
	log_dir = os.path.expandvars(bcfg.get("output",{}).get("log_dir","./browser_mcp_logs"))
	os.makedirs(log_dir, exist_ok=True)
	tabs = bt.list_tabs()
	out = Path(log_dir) / f"tabs_{ts()}.json"
	json.dump(tabs, open(out,"w",encoding="utf-8"), indent=2)
	print("Saved tabs to", out)

if __name__ == "__main__":
	main()
