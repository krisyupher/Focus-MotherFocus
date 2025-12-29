"""JSON Configuration Repository Adapter"""
from dataclasses import dataclass
import json
import os
from typing import Dict, Any
from ...application.interfaces.config_repository import IConfigRepository
from ...core.entities.monitoring_session import MonitoringSession
from ...core.value_objects.url import URL


@dataclass
class JsonConfigRepository(IConfigRepository):
    """
    Configuration repository implementation using JSON file storage.

    This is an infrastructure adapter that implements the IConfigRepository
    interface by persisting configuration to a JSON file.

    Attributes:
        config_file_path: Path to the JSON configuration file
    """
    config_file_path: str = "config.json"

    def save_session(self, session: MonitoringSession) -> bool:
        """
        Save the monitoring session configuration to JSON.

        Args:
            session: MonitoringSession to persist

        Returns:
            True if save succeeded, False otherwise
        """
        try:
            # Convert session to dictionary
            config_data = self._session_to_dict(session)

            # Write to file
            with open(self.config_file_path, 'w') as f:
                json.dump(config_data, f, indent=4)

            return True
        except Exception as e:
            print(f"Error saving configuration: {e}")
            return False

    def load_session(self) -> MonitoringSession:
        """
        Load the monitoring session configuration from JSON.

        Returns:
            MonitoringSession loaded from persistence, or empty session if none exists
        """
        try:
            # Check if file exists
            if not os.path.exists(self.config_file_path):
                return MonitoringSession()

            # Read from file
            with open(self.config_file_path, 'r') as f:
                config_data = json.load(f)

            # Convert dictionary to session
            return self._dict_to_session(config_data)

        except Exception as e:
            print(f"Error loading configuration: {e}")
            return MonitoringSession()

    def _session_to_dict(self, session: MonitoringSession) -> Dict[str, Any]:
        """
        Convert monitoring session to dictionary for JSON serialization.

        Args:
            session: MonitoringSession to convert

        Returns:
            Dictionary representation
        """
        return {
            "websites": [str(website.url) for website in session.get_all_websites()],
            "monitoring_interval": session.monitoring_interval
        }

    def _dict_to_session(self, data: Dict[str, Any]) -> MonitoringSession:
        """
        Convert dictionary from JSON to monitoring session.

        Args:
            data: Dictionary from JSON

        Returns:
            MonitoringSession instance
        """
        # Create session with interval
        interval = data.get("monitoring_interval", 10)
        session = MonitoringSession(monitoring_interval=interval)

        # Add websites
        websites = data.get("websites", [])
        for url_string in websites:
            try:
                url = URL.from_string(url_string)
                session.add_website(url)
            except ValueError as e:
                print(f"Skipping invalid URL {url_string}: {e}")
                continue

        return session
