import json
import os
from datetime import datetime
from notifymemaybe import NotifyMeMaybe

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
    ncfg = cfg.get("notifymemaybe", {})
    if not ncfg.get("enabled", False):
        print("notifymemaybe disabled in config."); return

    exe_path = ncfg.get("exe_path") or None
    nm = NotifyMeMaybe(exe_path=exe_path)
    print("NotifyMeMaybe available:", nm.is_available())

    log_dir = os.path.expandvars(ncfg.get("output", {}).get("log_dir", "./notifymemaybe_logs"))
    os.makedirs(log_dir, exist_ok=True)

    # Confirm demo
    try:
        ok = nm.prompt_confirm("FocusMotherFocus", "Do you want to run the demo?", yes_label=ncfg.get("confirm_defaults", {}).get("yes_label","Yes"), no_label=ncfg.get("confirm_defaults", {}).get("no_label","No"), timeout=ncfg.get("default_timeout_seconds"))
        res = {"type": "confirm", "result": bool(ok), "ts": ts()}
        out = os.path.join(log_dir, f"confirm_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(res, f, indent=2)
        print("Confirm result saved to", out)
    except Exception as e:
        print("confirm error:", e)

    # Select demo
    try:
        choices = ["Continue", "Snooze", "Cancel"]
        sel = nm.prompt_select("FocusMotherFocus", "Choose an action:", choices, timeout=ncfg.get("default_timeout_seconds"))
        res = {"type": "select", "selection": sel, "ts": ts()}
        out = os.path.join(log_dir, f"select_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(res, f, indent=2)
        print("Select result saved to", out)
    except Exception as e:
        print("select error:", e)

if __name__ == "__main__":
    main()
