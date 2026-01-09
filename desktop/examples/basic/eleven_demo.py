import os
import json
from datetime import datetime
from elevenlabs_mcp import ElevenLabsMCP

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
    ecfg = cfg.get("elevenlabs_mcp", {})
    if not ecfg.get("enabled", False):
        print("elevenlabs_mcp disabled in config."); return

    api_url = ecfg.get("api_url", "http://localhost:5200")
    api_key = ecfg.get("api_key") or None
    client = ElevenLabsMCP(api_url=api_url, api_key=api_key)
    print("ElevenLabs backend available:", client.is_available())

    log_dir = os.path.expandvars(ecfg.get("output", {}).get("log_dir", "./eleven_mcp_logs"))
    os.makedirs(log_dir, exist_ok=True)

    try:
        voices = client.list_voices()
        out = os.path.join(log_dir, f"voices_{ts()}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(voices, f, indent=2)
        print("Saved voices to", out)
    except Exception as e:
        print("list_voices error:", e)

    # TTS sample
    try:
        voice = ecfg.get("tts", {}).get("voice") or ecfg.get("default_voice")
        fmt = ecfg.get("tts", {}).get("format", "mp3")
        sample_text = "Hello. This is a FocusMotherFocus ElevenLabs test."
        save_path = os.path.join(log_dir, f"tts_sample_{ts()}.{fmt}")
        data = client.synthesize_text(sample_text, voice=voice, fmt=fmt, save_path=save_path)
        print("Saved TTS to", save_path, "bytes:", len(data))
    except Exception as e:
        print("tts error:", e)

    # Optional clone
    try:
        clone_cfg = ecfg.get("clone", {})
        if clone_cfg.get("enabled") and clone_cfg.get("sample_audio_path"):
            res = client.clone_voice(clone_cfg.get("new_voice_name", "fmf_clone"), clone_cfg.get("sample_audio_path"))
            print("clone_voice response:", res)
    except Exception as e:
        print("clone error:", e)

if __name__ == "__main__":
    main()
