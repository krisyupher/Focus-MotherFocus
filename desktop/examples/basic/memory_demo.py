import json
import os
import time
from datetime import datetime
from memory_mcp import MemoryMCP

def load_config(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def ts():
    return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
    cfg_path = os.path.join(os.path.dirname(__file__), "mcp_client_config.json")
    if not os.path.exists(cfg_path):
        print("Config not found:", cfg_path); return
    cfg = load_config(cfg_path)
    mcfg = cfg.get("memory_mcp", {})
    if not mcfg.get("enabled", False):
        print("memory_mcp disabled in config."); return

    api_url = mcfg.get("api_url", "http://localhost:5000")
    api_key = mcfg.get("api_key") or None
    mem = MemoryMCP(api_url=api_url, api_key=api_key)

    print("Memory backend available:", mem.is_available())

    log_dir = os.path.expandvars(mcfg.get("output", {}).get("log_dir", "./memory_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    # Send sample events
    try:
        e1 = mem.add_event("active_window", {"title": "YouTube - funny vids", "app": "chrome"})
        e2 = mem.add_event("visited_url", {"url": "https://youtube.com/watch?v=xyz", "tab": "tab1"})
        e3 = mem.add_event("distraction_detected", {"reason": "social_media", "score": 0.9})
        print("Sent sample events:", e1, e2, e3)
    except Exception as exc:
        print("add_event error:", exc)

    # Query patterns (example)
    try:
        patterns = mem.query_patterns(q="distraction", limit=20)
        out = os.path.join(log_dir, f"patterns_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(patterns, f, indent=2)
        print("Saved patterns to", out)
    except Exception as exc:
        print("query_patterns error:", exc)

    # Export graph
    try:
        save_path = os.path.join(log_dir, f"graph_{ts()}.bin")
        data = mem.export_graph(save_path=save_path)
        print("Exported graph to", save_path, "bytes:", len(data))
    except Exception as exc:
        print("export_graph error:", exc)

if __name__ == "__main__":
    main()
