import os
import tempfile
from windows_mcp import WindowsMCP

def main():
	mcp = WindowsMCP()  # or WindowsMCP(exe_path=r"C:\path\to\MCPGet.exe")
	if not mcp.is_available():
		print("MCPGet not found on PATH. Set exe_path in WindowsMCP or install Windows-MCP.")
		return

	print("Active window:")
	try:
		print(mcp.get_active_window())
	except Exception as e:
		print("  Error:", e)

	print("\nRunning apps/process list:")
	try:
		print(mcp.get_running_apps())
	except Exception as e:
		print("  Error:", e)

	print("\nUI elements (root):")
	try:
		print(mcp.get_ui_elements())
	except Exception as e:
		print("  Error:", e)

	print("\nCapture screenshot to temp file:")
	try:
		tmp = os.path.join(tempfile.gettempdir(), "mcp_screenshot.png")
		data = mcp.capture_screenshot(save_path=tmp)
		print("  Saved screenshot to", tmp, "bytes:", len(data))
	except Exception as e:
		print("  Error:", e)

if __name__ == "__main__":
	main()
