import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CAND_CONFIGS = [ROOT / "config" / "mcp_client_config.json", ROOT / "mcp_client_config.json"]

def find_config():
	for p in CAND_CONFIGS:
		if p.exists():
			return p
	return None
