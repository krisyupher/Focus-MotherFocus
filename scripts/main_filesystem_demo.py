import os, json
from mcp_clients import FilesystemMCP
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	fcfg = cfg.get("filesystem_mcp",{})
	if not fcfg.get("enabled", False): print("filesystem disabled"); return
	fs = FilesystemMCP(api_url=fcfg.get("api_url"), api_key=fcfg.get("api_key"))
	print("Filesystem available:", fs.is_available())

if __name__ == "__main__":
	main()
