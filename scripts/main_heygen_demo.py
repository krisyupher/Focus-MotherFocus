import os, json
from mcp_clients import HeyGenMCP
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	hcfg = cfg.get("heygen_mcp",{})
	if not hcfg.get("enabled", False): print("heygen disabled"); return
	client = HeyGenMCP(api_url=hcfg.get("api_url"), api_key=hcfg.get("api_key"))
	print("HeyGen available:", client.is_available())

if __name__ == "__main__":
	main()
