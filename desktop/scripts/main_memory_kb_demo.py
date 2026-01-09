import os, json
from mcp_clients import MemoryKB
from ._utils import find_config
from datetime import datetime
def ts(): return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
	cfg_path = find_config()
	if not cfg_path: print("Config not found"); return
	cfg = json.load(open(cfg_path,"r",encoding="utf-8"))
	mcfg = cfg.get("memory_mcp",{})
	if not mcfg.get("enabled", False): print("memory disabled"); return
	kb = MemoryKB(api_url=mcfg.get("api_url"), api_key=mcfg.get("api_key"))
	print("KB available:", kb.is_available())
	try:
		kb.store_agreement("agree-001", ["user:alice"], "No distracting sites after 9pm", confidence=0.9)
		print("stored agreement")
	except Exception as e: print("error", e)

if __name__ == "__main__":
	main()
