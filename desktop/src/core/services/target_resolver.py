"""Service to automatically resolve target names to URLs and process names."""
from typing import Optional, Tuple
from ..value_objects.url import URL
from ..value_objects.process_name import ProcessName


class TargetResolver:
    """
    Resolves simple names (like "Netflix") to website URLs and process names.

    This allows users to just type "Netflix" and automatically get:
    - URL: netflix.com
    - Process: Netflix.exe
    """

    # Known mappings: name -> (url, process_name)
    KNOWN_TARGETS = {
        # Streaming Services
        "netflix": ("netflix.com", "Netflix.exe"),
        "spotify": ("open.spotify.com", "Spotify.exe"),
        "youtube": ("youtube.com", None),
        "twitch": ("twitch.tv", None),
        "hulu": ("hulu.com", None),
        "disney": ("disneyplus.com", None),
        "prime": ("primevideo.com", None),

        # Social Media
        "facebook": ("facebook.com", None),
        "instagram": ("instagram.com", None),
        "twitter": ("twitter.com", None),
        "reddit": ("reddit.com", None),
        "tiktok": ("tiktok.com", None),
        "linkedin": ("linkedin.com", None),

        # Productivity
        "slack": ("slack.com", "slack.exe"),
        "discord": ("discord.com", "Discord.exe"),
        "teams": ("teams.microsoft.com", "Teams.exe"),
        "zoom": ("zoom.us", "Zoom.exe"),

        # Gaming
        "steam": ("store.steampowered.com", "steam.exe"),
        "epicgames": ("epicgames.com", "EpicGamesLauncher.exe"),
        "origin": ("origin.com", "Origin.exe"),

        # Search Engines
        "google": ("google.com", None),
        "bing": ("bing.com", None),

        # Email
        "gmail": ("gmail.com", None),
        "outlook": ("outlook.com", None),

        # Development
        "github": ("github.com", None),
        "stackoverflow": ("stackoverflow.com", None),

        # Windows Apps (app only)
        "notepad": (None, "notepad.exe"),
        "calculator": (None, "calc.exe"),
        "paint": (None, "mspaint.exe"),
        "explorer": (None, "explorer.exe"),
    }

    @classmethod
    def resolve(cls, name: str) -> Tuple[Optional[URL], Optional[ProcessName], str]:
        """
        Resolve a simple name to URL and process name.

        Args:
            name: Simple name like "Netflix", "Spotify", etc.

        Returns:
            Tuple of (url, process_name, display_name)
            - url: URL object or None
            - process_name: ProcessName object or None
            - display_name: Formatted name for display

        Examples:
            >>> url, proc, display = TargetResolver.resolve("netflix")
            >>> # Returns: (URL("netflix.com"), ProcessName("Netflix.exe"), "Netflix")
        """
        name_lower = name.strip().lower()

        # Check known targets
        if name_lower in cls.KNOWN_TARGETS:
            url_str, proc_str = cls.KNOWN_TARGETS[name_lower]

            url = URL(url_str) if url_str else None
            process_name = ProcessName(proc_str) if proc_str else None
            display_name = name.strip().title()

            return url, process_name, display_name

        # Unknown target - try to auto-generate
        # Assume it's a website and try to create both
        return cls._auto_generate(name)

    @classmethod
    def _auto_generate(cls, name: str) -> Tuple[Optional[URL], Optional[ProcessName], str]:
        """
        Auto-generate URL and process name from unknown input.

        Strategy:
        - Try as website: name.com
        - Try as process: Name.exe
        - Use original name as display

        Args:
            name: Input name

        Returns:
            Tuple of (url, process_name, display_name)
        """
        name_clean = name.strip()
        name_lower = name_clean.lower()

        # Try to create URL (assume .com domain)
        url = None
        try:
            # If it looks like a URL already
            if "." in name_lower:
                url = URL(name_clean)
            else:
                # Auto-add .com
                url = URL(f"{name_lower}.com")
        except ValueError:
            pass  # Invalid URL, leave as None

        # Try to create process name
        process_name = None
        try:
            # If it has .exe already, use as-is
            if name_lower.endswith(".exe"):
                process_name = ProcessName(name_clean)
            else:
                # Try to create process name (capitalize first letter)
                process_name = ProcessName(f"{name_clean.title()}.exe")
        except ValueError:
            pass  # Invalid process name, leave as None

        display_name = name_clean.title()

        return url, process_name, display_name

    @classmethod
    def add_custom_mapping(cls, name: str, url: Optional[str], process: Optional[str]) -> None:
        """
        Add a custom mapping for a target.

        Args:
            name: Target name (e.g., "MyApp")
            url: Website URL or None
            process: Process name or None
        """
        cls.KNOWN_TARGETS[name.lower()] = (url, process)
