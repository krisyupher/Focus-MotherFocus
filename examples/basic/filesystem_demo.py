import json
import os
from datetime import datetime
from filesystem_mcp import FilesystemMCP

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
    fcfg = cfg.get("filesystem_mcp", {})
    if not fcfg.get("enabled", False):
        print("filesystem_mcp disabled in config."); return

    api_url = fcfg.get("api_url", "http://localhost:5100")
    api_key = fcfg.get("api_key") or None
    fs = FilesystemMCP(api_url=api_url, api_key=api_key)

    print("Filesystem backend available:", fs.is_available())

    log_dir = os.path.expandvars(fcfg.get("output", {}).get("log_dir", "./filesystem_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    test_path = os.path.join(log_dir, f"test_file_{ts()}.txt")
    try:
        fs.secure_write(test_path, b"FocusMotherFocus test content\n")
        print("Wrote test file:", test_path)
        content = fs.secure_read(test_path)
        print("Read bytes:", len(content))
        out = os.path.join(log_dir, f"read_{ts()}.txt")
        with open(out, "wb") as f:
            f.write(content)
        print("Saved read content to", out)
    except Exception as e:
        print("read/write error:", e)

    try:
        listing = fs.list_files(os.path.dirname(test_path))
        out = os.path.join(log_dir, f"listing_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(listing, f, indent=2)
        print("Saved listing to", out)
    except Exception as e:
        print("list_files error:", e)

    try:
        aud = fs.audit_event("file_write", {"path": test_path, "user": os.getlogin()})
        print("Audit event sent:", aud)
    except Exception as e:
        print("audit_event error:", e)

if __name__ == "__main__":
    main()
