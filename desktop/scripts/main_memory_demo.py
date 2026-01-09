import os, json
from mcp_clients import MemoryMCP
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	mcfg = cfg.get("memory_mcp",{})
	if not mcfg.get("enabled", False): print("memory disabled"); return
	mem = MemoryMCP(api_url=mcfg.get("api_url"), api_key=mcfg.get("api_key"))
	print("Memory available:", mem.is_available())
	log_dir = os.path.expandvars(mcfg.get("output",{}).get("log_dir","./memory_mcp_logs"))
	os.makedirs(log_dir, exist_ok=True)
	try:
		mem.add_event("active_window", {"title":"Demo","app":"demo"})
		print("posted demo events")
	except Exception as e: print("error posting events", e)

if __name__ == "__main__":
	main()
