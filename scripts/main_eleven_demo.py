import os, json
from mcp_clients import ElevenLabsMCP
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	ecfg = cfg.get("elevenlabs_mcp",{})
	if not ecfg.get("enabled", False): print("elevenlabs disabled"); return
	client = ElevenLabsMCP(api_url=ecfg.get("api_url"), api_key=ecfg.get("api_key"))
	print("ElevenLabs available:", client.is_available())

if __name__ == "__main__":
	main()
