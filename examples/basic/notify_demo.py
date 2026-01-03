import json
import os
from mcp_server_notify import MCPServerNotify

def load_config(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def main():
    cfg_path = os.path.join(os.path.dirname(__file__), "mcp_client_config.json")
    if not os.path.exists(cfg_path):
        print("Config not found:", cfg_path)
        return

    cfg = load_config(cfg_path)
    notify_cfg = cfg.get("mcp_server_notify", {})
    if not notify_cfg.get("enabled", False):
        print("mcp_server_notify disabled in config.")
        return

    exe_path = notify_cfg.get("exe_path") or None
    notify = MCPServerNotify(exe_path=exe_path)

    print("Notification backend available:", notify.is_available())

    if notify_cfg.get("test_notify_on_start", False):
        title = "FocusMotherFocus"
        message = "Notification system initialized (test)."
        sound = notify_cfg.get("default_sound") or None
        icon = notify_cfg.get("default_icon") or None
        ok = notify.send_notification(title, message, sound=sound, icon=icon)
        print("Test notification sent:", ok)

if __name__ == "__main__":
    main()
