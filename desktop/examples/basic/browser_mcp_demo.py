import os
import json
import time
from datetime import datetime
from browser_tools_mcp import BrowserToolsMCP

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
    bcfg = cfg.get("browser_tools_mcp", {})
    if not bcfg.get("enabled", False):
        print("browser_tools_mcp disabled in config."); return

    exe_path = bcfg.get("exe_path") or None
    bt = BrowserToolsMCP(exe_path=exe_path)
    print("Backend available:", bt.is_available())

    log_dir = os.path.expandvars(bcfg.get("output", {}).get("log_dir", "./browser_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    # List tabs
    try:
        tabs = bt.list_tabs()
        outfile = os.path.join(log_dir, f"tabs_{ts()}.json")
        with open(outfile, "w", encoding="utf-8") as f:
            json.dump(tabs, f, indent=2)
        print("Saved tabs to", outfile)
    except Exception as e:
        print("list_tabs error:", e)

    # Determine targets
    targets = []
    default = bcfg.get("targets", {}).get("default", "active_tab")
    if default == "all_tabs":
        try:
            raw = tabs.get("result", []) if isinstance(tabs, dict) else []
            targets = [t.get("id") or t.get("tabId") for t in raw][: bcfg.get("targets", {}).get("max_tabs", 5)]
        except Exception:
            targets = []
    else:
        # try to pick foreground/active tab if available
        try:
            raw = tabs.get("result", [])
            active = next((t for t in raw if t.get("active") or t.get("isActive")), None)
            if active:
                targets = [active.get("id") or active.get("tabId")]
        except Exception:
            targets = []

    if not targets:
        print("No targets found, aborting capture steps."); return

    for tab in targets:
        # console logs
        if "console_logs" in bcfg.get("capture_types", []):
            try:
                logs = bt.get_console_logs(tab)
                out = os.path.join(log_dir, f"console_{tab}_{ts()}.json")
                with open(out, "w", encoding="utf-8") as f:
                    json.dump(logs, f, indent=2)
                print("Saved console logs to", out)
            except Exception as e:
                print("console logs error for", tab, ":", e)

        # network
        if "network" in bcfg.get("capture_types", []):
            try:
                net = bt.get_network_activity(tab)
                out = os.path.join(log_dir, f"network_{tab}_{ts()}.json")
                with open(out, "w", encoding="utf-8") as f:
                    json.dump(net, f, indent=2)
                print("Saved network to", out)
            except Exception as e:
                print("network error for", tab, ":", e)

        # screenshot
        if "screenshot" in bcfg.get("capture_types", []) and bcfg.get("screenshot", {}).get("enabled", True):
            try:
                save_path = os.path.join(log_dir, f"screenshot_{tab}_{ts()}.png")
                data = bt.capture_tab_screenshot(tab_id=tab, save_path=save_path)
                print("Saved screenshot to", save_path, "bytes:", len(data))
            except Exception as e:
                print("screenshot error for", tab, ":", e)

if __name__ == "__main__":
    main()
