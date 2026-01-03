import os, json
from mcp_clients import WebcamMCP
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	wcfg = cfg.get("webcam_mcp",{})
	if not wcfg.get("enabled", False): print("webcam disabled"); return
	client = WebcamMCP(exe_path=wcfg.get("exe_path") or None, api_url=wcfg.get("api_url") or None)
	print("Webcam available:", client.is_available())

if __name__ == "__main__":
	main()
