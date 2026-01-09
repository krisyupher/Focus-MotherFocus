import os
import sys
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CAND_CONFIGS = [ROOT / "config" / "mcp_client_config.json", ROOT / "mcp_client_config.json"]

DEMO_MAP = {
    "windows_mcp": "main_mcp_demo.py",
    "mcp_server_notify": "main_notify_demo.py",
    "browser_tools_mcp": "main_browser_mcp_demo.py",
    "memory_mcp": "main_memory_demo.py",
    "memory_kb": "main_memory_kb_demo.py",
    "filesystem_mcp": "main_filesystem_demo.py",
    "elevenlabs_mcp": "main_eleven_demo.py",
    "heygen_mcp": "main_heygen_demo.py",
    "webcam_mcp": "main_webcam_demo.py",
    "notifymemaybe": "main_notifymaybe_demo.py"
}

def find_config():
    for p in CAND_CONFIGS:
        if p.exists():
            return p
    return None

def load_config(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def find_script(script_name):
    # look in scripts/ then root
    s1 = ROOT / "scripts" / script_name
    s2 = ROOT / script_name
    if s1.exists():
        return s1
    if s2.exists():
        return s2
    return None

def run_script(path):
    print("Running:", path)
    try:
        subprocess.check_call([sys.executable, str(path)])
    except subprocess.CalledProcessError as e:
        print("Script failed:", path, e)

def main():
    cfg_path = find_config()
    if not cfg_path:
        print("Config not found. Create config/mcp_client_config.json or mcp_client_config.json at repo root.")
        return
    cfg = load_config(cfg_path)
    for key, demo in DEMO_MAP.items():
        section = cfg.get(key, {})
        if section.get("enabled", False):
            script = find_script(demo)
            if script:
                run_script(script)
            else:
                print("Demo script for", key, "not found; expected", demo)
    print("All enabled demos processed.")

if __name__ == "__main__":
    main()
