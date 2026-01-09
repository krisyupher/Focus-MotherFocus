"""Unit tests for ThreadedScheduler"""
import pytest
import time
from src.infrastructure.adapters.threaded_scheduler import ThreadedScheduler


class TestThreadedScheduler:
    """Test suite for ThreadedScheduler"""

    def test_create_scheduler(self):
        """Test creating a scheduler"""
        scheduler = ThreadedScheduler()
        assert scheduler.is_running() is False

    def test_start_scheduler(self):
        """Test starting the scheduler"""
        scheduler = ThreadedScheduler()
        call_count = []

        def callback():
            call_count.append(1)

        scheduler.start(interval=1, callback=callback)

        assert scheduler.is_running() is True
        scheduler.stop()

    def test_scheduler_executes_callback(self):
        """Test that scheduler executes callback periodically"""
        scheduler = ThreadedScheduler()
        call_count = []

        def callback():
            call_count.append(1)

        scheduler.start(interval=1, callback=callback)
        time.sleep(2.5)  # Wait for at least 2 callbacks
        scheduler.stop()

        # Should have called at least 2 times
        assert len(call_count) >= 2

    def test_stop_scheduler(self):
        """Test stopping the scheduler"""
        scheduler = ThreadedScheduler()
        call_count = []

        def callback():
            call_count.append(1)

        scheduler.start(interval=1, callback=callback)
        time.sleep(1.5)
        scheduler.stop()

        count_at_stop = len(call_count)
        time.sleep(1.5)  # Wait more

        # Count should not increase after stop
        assert len(call_count) == count_at_stop
        assert scheduler.is_running() is False

    def test_start_already_running_raises_error(self):
        """Test that starting already running scheduler raises error"""
        scheduler = ThreadedScheduler()
        scheduler.start(interval=1, callback=lambda: None)

        with pytest.raises(ValueError, match="already running"):
            scheduler.start(interval=1, callback=lambda: None)

        scheduler.stop()

    def test_start_with_invalid_interval_raises_error(self):
        """Test that starting with invalid interval raises error"""
        scheduler = ThreadedScheduler()

        with pytest.raises(ValueError, match="greater than 0"):
            scheduler.start(interval=0, callback=lambda: None)

        with pytest.raises(ValueError, match="greater than 0"):
            scheduler.start(interval=-1, callback=lambda: None)

    def test_stop_inactive_scheduler(self):
        """Test that stopping inactive scheduler is safe"""
        scheduler = ThreadedScheduler()
        scheduler.stop()  # Should not raise error
        assert scheduler.is_running() is False

    def test_scheduler_handles_callback_exception(self):
        """Test that scheduler continues after callback exception"""
        scheduler = ThreadedScheduler()
        call_count = []

        def callback():
            call_count.append(1)
            if len(call_count) == 1:
                raise Exception("Test exception")

        scheduler.start(interval=1, callback=callback)
        time.sleep(2.5)
        scheduler.stop()

        # Should have called multiple times despite exception
        assert len(call_count) >= 2
