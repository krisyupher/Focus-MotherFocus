"""Unit tests for ProcessName value object"""
import pytest
from src.core.value_objects.process_name import ProcessName


class TestProcessName:
    """Test suite for ProcessName value object"""

    def test_create_process_name_with_exe(self):
        """Test creating a process name with .exe extension"""
        process_name = ProcessName("chrome.exe")
        assert process_name.value == "chrome.exe"
        assert str(process_name) == "chrome.exe"

    def test_create_process_name_without_extension(self):
        """Test creating a process name without extension"""
        process_name = ProcessName("chrome")
        assert process_name.value == "chrome.exe"

    def test_create_process_name_uppercase(self):
        """Test that process names are normalized to lowercase"""
        process_name = ProcessName("CHROME.EXE")
        assert process_name.value == "chrome.exe"

    def test_create_process_name_mixed_case(self):
        """Test that mixed case process names are normalized"""
        process_name = ProcessName("Chrome.Exe")
        assert process_name.value == "chrome.exe"

    def test_create_process_name_from_string(self):
        """Test creating process name from string using factory method"""
        process_name = ProcessName.from_string("notepad.exe")
        assert process_name.value == "notepad.exe"

    def test_create_process_name_with_path(self):
        """Test creating process name from full path extracts only filename"""
        process_name = ProcessName("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")
        assert process_name.value == "chrome.exe"

    def test_empty_process_name_raises_error(self):
        """Test that empty process name raises ValueError"""
        with pytest.raises(ValueError, match="Process name cannot be empty"):
            ProcessName("")

    def test_whitespace_only_process_name_raises_error(self):
        """Test that whitespace-only process name raises ValueError"""
        with pytest.raises(ValueError, match="Process name cannot be empty"):
            ProcessName("   ")

    def test_process_name_equality(self):
        """Test that process names with same value are equal"""
        process_name1 = ProcessName("chrome.exe")
        process_name2 = ProcessName("chrome.exe")
        assert process_name1 == process_name2

    def test_process_name_equality_case_insensitive(self):
        """Test that process names are equal regardless of case"""
        process_name1 = ProcessName("chrome.exe")
        process_name2 = ProcessName("CHROME.EXE")
        assert process_name1 == process_name2

    def test_process_name_immutable(self):
        """Test that process name is immutable"""
        process_name = ProcessName("chrome.exe")
        with pytest.raises(AttributeError):
            process_name.value = "firefox.exe"

    def test_process_name_get_base_name(self):
        """Test getting base name without extension"""
        process_name = ProcessName("chrome.exe")
        assert process_name.get_base_name() == "chrome"

    def test_process_name_matches_exact(self):
        """Test exact process name matching"""
        process_name = ProcessName("chrome.exe")
        assert process_name.matches("chrome.exe") is True
        assert process_name.matches("CHROME.EXE") is True
        assert process_name.matches("firefox.exe") is False

    def test_process_name_matches_without_extension(self):
        """Test matching without extension"""
        process_name = ProcessName("chrome.exe")
        assert process_name.matches("chrome") is True
        assert process_name.matches("CHROME") is True
