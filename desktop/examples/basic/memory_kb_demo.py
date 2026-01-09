import os
import json
from datetime import datetime
from memory_kb import MemoryKB

def ts():
    return datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")

def main():
    cfg_path = os.path.join(os.path.dirname(__file__), "mcp_client_config.json")
    if not os.path.exists(cfg_path):
        print("Config not found:", cfg_path); return
    cfg = json.load(open(cfg_path, "r", encoding="utf-8"))
    mcfg = cfg.get("memory_mcp", {})
    if not mcfg.get("enabled", False):
        print("memory_mcp disabled in config."); return

    api_url = mcfg.get("api_url", "http://localhost:5000")
    api_key = mcfg.get("api_key") or None
    kb = MemoryKB(api_url=api_url, api_key=api_key)
    print("Memory KB available:", kb.is_available())

    log_dir = os.path.expandvars(mcfg.get("output", {}).get("log_dir", "./memory_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    try:
        agr = kb.store_agreement("agree-001", ["user:alice", "user:bob"], "Do not visit distracting sites during focus hours", confidence=0.95)
        pat = kb.store_pattern("pattern-yt-social", "Frequent YouTube visits between 3-4pm", evidence=[{"url":"youtube.com", "count": 12}], confidence=0.8)
        link = kb.link_entities("agree-001", "pattern-yt-social", relation="informs")
        print("Stored agreement, pattern and link.")
        with open(os.path.join(log_dir, f"kb_store_{ts()}.json"), "w", encoding="utf-8") as f:
            json.dump({"agreement": agr, "pattern": pat, "link": link}, f, indent=2)
    except Exception as e:
        print("kb store error:", e)

    try:
        patterns = kb.query_patterns(q="youtube", limit=20)
        out = os.path.join(log_dir, f"kb_patterns_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(patterns, f, indent=2)
        print("Saved patterns to", out)
    except Exception as e:
        print("query_patterns error:", e)

    try:
        snap = kb.export_snapshot(save_path=os.path.join(log_dir, f"graph_snapshot_{ts()}.bin"))
        print("Exported snapshot bytes:", len(snap))
    except Exception as e:
        print("export_snapshot error:", e)

if __name__ == "__main__":
    main()
