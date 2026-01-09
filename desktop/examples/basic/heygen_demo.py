import json
import os
from datetime import datetime
from heygen_mcp import HeyGenMCP

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
    hcfg = cfg.get("heygen_mcp", {})
    if not hcfg.get("enabled", False):
        print("heygen_mcp disabled in config."); return

    api_url = hcfg.get("api_url", "http://localhost:5300")
    api_key = hcfg.get("api_key") or None
    client = HeyGenMCP(api_url=api_url, api_key=api_key)
    print("HeyGen backend available:", client.is_available())

    log_dir = os.path.expandvars(hcfg.get("output", {}).get("log_dir", "./heygen_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    try:
        avatars = client.list_avatars()
        out = os.path.join(log_dir, f"avatars_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(avatars, f, indent=2)
        print("Saved avatars to", out)
    except Exception as e:
        print("list_avatars error:", e)

    # Generate a short demo video
    try:
        text = "Hello. This is a FocusMotherFocus HeyGen demo."
        avatar = hcfg.get("default_avatar")
        style = hcfg.get("default_style")
        video_opts = hcfg.get("video", {})
        job = client.generate_video_from_text(text, avatar=avatar, style=style, video_opts=video_opts)
        print("Generation started:", job)
        # If server returns an immediate video_id try download, otherwise poll via status if job contains job_id
        video_id = job.get("video_id") or job.get("id")
        job_id = job.get("job_id") or job.get("id")
        if video_id:
            save_path = os.path.join(log_dir, f"heygen_video_{ts()}.{video_opts.get('format','mp4')}")
            data = client.download_video(video_id, save_path=save_path)
            print("Saved video to", save_path, "bytes:", len(data))
        elif job_id:
            # poll status once or twice (simple demo)
            import time
            for _ in range(10):
                st = client.get_status(job_id)
                if st and st.get("status") in ("done", "completed") and st.get("video_id"):
                    vid = st.get("video_id")
                    save_path = os.path.join(log_dir, f"heygen_video_{ts()}.{video_opts.get('format','mp4')}")
                    data = client.download_video(vid, save_path=save_path)
                    print("Saved video to", save_path, "bytes:", len(data))
                    break
                time.sleep(2)
            else:
                print("Generation not completed in demo polling window.")
    except Exception as e:
        print("generate/download error:", e)

if __name__ == "__main__":
    main()
