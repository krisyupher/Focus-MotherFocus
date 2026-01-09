"""Unified JSON Configuration Repository for MonitoringSessionV2"""
from dataclasses import dataclass
import json
import os
from typing import Dict, Any, List, Optional
from ...application.interfaces.config_repository import IConfigRepository
from ...core.entities.monitoring_session_v2 import MonitoringSessionV2
from ...core.value_objects.url import URL
from ...core.value_objects.process_name import ProcessName


@dataclass
class JsonConfigRepositoryV2(IConfigRepository):
    """
    Configuration repository for unified monitoring targets.

    Persists MonitoringSessionV2 to JSON with format:
    {
        "targets": [
            {
                "id": "uuid",
                "name": "Netflix",
                "url": "https://netflix.com",
                "process_name": "Netflix.exe"
            }
        ],
        "monitoring_interval": 10
    }
    """
    config_file_path: str = "config.json"

    def save_session(self, session: MonitoringSessionV2) -> bool:
        """
        Save the unified monitoring session to JSON.

        Args:
            session: MonitoringSessionV2 to persist

        Returns:
            True if save succeeded, False otherwise
        """
        try:
            config_data = self._session_to_dict(session)

            with open(self.config_file_path, 'w') as f:
                json.dump(config_data, f, indent=4)

            return True
        except Exception as e:
            print(f"Error saving configuration: {e}")
            return False

    def load_session(self) -> MonitoringSessionV2:
        """
        Load the unified monitoring session from JSON.

        Returns:
            MonitoringSessionV2 loaded from persistence, or empty session if none exists
        """
        try:
            if not os.path.exists(self.config_file_path):
                return MonitoringSessionV2()

            with open(self.config_file_path, 'r') as f:
                config_data = json.load(f)

            return self._dict_to_session(config_data)

        except Exception as e:
            print(f"Error loading configuration: {e}")
            return MonitoringSessionV2()

    def _session_to_dict(self, session: MonitoringSessionV2) -> Dict[str, Any]:
        """
        Convert monitoring session to dictionary for JSON serialization.

        Args:
            session: MonitoringSessionV2 to convert

        Returns:
            Dictionary representation
        """
        return {
            "targets": [
                {
                    "id": target.id,
                    "name": target.name,
                    "url": str(target.url) if target.url else None,
                    "process_name": str(target.process_name) if target.process_name else None
                }
                for target in session.get_all_targets()
            ],
            "monitoring_interval": session.monitoring_interval
        }

    def _dict_to_session(self, data: Dict[str, Any]) -> MonitoringSessionV2:
        """
        Convert dictionary from JSON to monitoring session.

        Supports both:
        - New unified format (targets array)
        - Old format (websites + applications arrays) for backward compatibility

        Args:
            data: Dictionary from JSON

        Returns:
            MonitoringSessionV2 instance
        """
        interval = data.get("monitoring_interval", 10)
        session = MonitoringSessionV2(monitoring_interval=interval)

        # New unified format
        if "targets" in data:
            self._load_targets(session, data["targets"])
        # Old format - migrate to unified
        else:
            self._migrate_old_format(session, data)

        return session

    def _load_targets(self, session: MonitoringSessionV2, targets_data: List[Dict[str, Any]]) -> None:
        """Load targets from unified format."""
        for target_data in targets_data:
            try:
                name = target_data["name"]
                url_str = target_data.get("url")
                process_name_str = target_data.get("process_name")

                url = URL(url_str) if url_str else None
                process_name = ProcessName(process_name_str) if process_name_str else None

                target = session.add_target(name, url, process_name)

                # Restore original ID if available
                if "id" in target_data:
                    # Remove from dict and re-add with original ID
                    session.targets.pop(target.id)
                    target.id = target_data["id"]
                    session.targets[target.id] = target

            except (ValueError, KeyError) as e:
                print(f"Skipping invalid target {target_data}: {e}")
                continue

    def _migrate_old_format(self, session: MonitoringSessionV2, data: Dict[str, Any]) -> None:
        """
        Migrate old format (separate websites/applications) to unified format.

        Old format:
        {
            "websites": ["https://google.com"],
            "applications": [{"process_name": "calc.exe", "display_name": "Calculator"}]
        }

        Creates unified targets from old data.
        """
        # Migrate websites
        websites = data.get("websites", [])
        for url_string in websites:
            try:
                url = URL.from_string(url_string)
                # Extract simple name from URL for display
                name = url.value.replace("https://", "").replace("http://", "").split("/")[0]
                name = name.replace("www.", "").title()
                session.add_target(name, url=url)
            except ValueError as e:
                print(f"Skipping invalid URL {url_string}: {e}")
                continue

        # Migrate applications
        applications = data.get("applications", [])
        for app_data in applications:
            try:
                process_name = ProcessName(app_data["process_name"])
                display_name = app_data.get("display_name", process_name.get_base_name().title())
                session.add_target(display_name, process_name=process_name)
            except (ValueError, KeyError) as e:
                print(f"Skipping invalid application {app_data}: {e}")
                continue
