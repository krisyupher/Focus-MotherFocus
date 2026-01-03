import os
import json
from datetime import datetime
from webcam_mcp import WebcamMCP

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
    wcfg = cfg.get("webcam_mcp", {})
    if not wcfg.get("enabled", False):
        print("webcam_mcp disabled in config."); return

    exe_path = wcfg.get("exe_path") or None
    api_url = wcfg.get("api_url") or None
    client = WebcamMCP(exe_path=exe_path, api_url=api_url)
    print("Webcam backend available:", client.is_available())

    log_dir = os.path.expandvars(wcfg.get("output", {}).get("log_dir", "./webcam_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    try:
        devices = client.list_devices()
        out = os.path.join(log_dir, f"devices_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(devices, f, indent=2)
        print("Saved device list to", out)
    except Exception as e:
        print("list_devices error:", e)

    if wcfg.get("image", {}).get("enabled", True):
        try:
            fmt = wcfg.get("image", {}).get("format", "png")
            res = wcfg.get("image", {}).get("resolution")
            save_path = os.path.join(log_dir, f"webcam_{ts()}.{fmt}")
            data = client.capture_image(resolution=res, save_path=save_path)
            print("Saved image to", save_path, "bytes:", len(data))
        except Exception as e:
            print("capture_image error:", e)

if __name__ == "__main__":
    main()
