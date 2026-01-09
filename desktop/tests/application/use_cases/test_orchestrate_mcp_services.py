"""
Tests for Orchestrate MCP Services Use Case.
"""
import pytest
from unittest.mock import Mock

from src.application.use_cases.orchestrate_mcp_services import OrchestrateMCPServicesUseCase
from src.application.interfaces.i_mcp_service_registry import (
    ServiceType,
    ServiceStatus,
    ServiceHealth,
    ServiceCapability
)
from datetime import datetime


@pytest.fixture
def mock_registry():
    """Create mock service registry."""
    return Mock()


@pytest.fixture
def orchestrator(mock_registry):
    """Create orchestrator use case."""
    return OrchestrateMCPServicesUseCase(mock_registry)


@pytest.fixture
def sample_health():
    """Create sample service health."""
    return ServiceHealth(
        service_type=ServiceType.BROWSER_TOOLS,
        status=ServiceStatus.AVAILABLE,
        last_check=datetime.now(),
        response_time_ms=10.0,
        error_count=0,
        metadata={}
    )


@pytest.fixture
def sample_capability():
    """Create sample capability."""
    return ServiceCapability(
        name="test_capability",
        description="Test capability",
        parameters=["param1"],
        fallback_services=[ServiceType.FILESYSTEM]
    )


class TestOrchestrateMCPServicesUseCase:
    """Test MCP orchestration use case."""

    def test_execute_with_fallback_success(self, orchestrator, mock_registry):
        """Execute with fallback succeeds with primary service."""
        mock_service = Mock()
        mock_service.do_something.return_value = "success"

        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.get_service.return_value = mock_service
        mock_registry.get_service_capabilities.return_value = []

        result = orchestrator.execute_with_fallback(
            "test_capability",
            lambda s: s.do_something()
        )

        assert result == "success"

    def test_execute_with_fallback_uses_fallback(self, orchestrator, mock_registry, sample_capability):
        """Execute with fallback uses fallback when primary fails."""
        primary_service = Mock()
        primary_service.do_something.side_effect = Exception("Primary failed")

        fallback_service = Mock()
        fallback_service.do_something.return_value = "fallback_success"

        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.get_service_capabilities.return_value = [sample_capability]

        # First call (primary) returns primary, second call (fallback) returns fallback
        mock_registry.get_service.side_effect = [primary_service, fallback_service]

        result = orchestrator.execute_with_fallback(
            "test_capability",
            lambda s: s.do_something()
        )

        assert result == "fallback_success"

    def test_execute_with_fallback_returns_none_when_all_fail(self, orchestrator, mock_registry):
        """Execute with fallback returns None when all services fail."""
        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.get_service.return_value = None
        mock_registry.get_service_capabilities.return_value = []

        result = orchestrator.execute_with_fallback(
            "test_capability",
            lambda s: s.do_something()
        )

        assert result is None

    def test_execute_with_fallback_calls_error_handler(self, orchestrator, mock_registry):
        """Execute with fallback calls error handler on failure."""
        failing_service = Mock()
        failing_service.do_something.side_effect = Exception("Test error")

        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.get_service.return_value = failing_service
        mock_registry.get_service_capabilities.return_value = []

        error_callback = Mock()

        orchestrator.execute_with_fallback(
            "test_capability",
            lambda s: s.do_something(),
            on_error=error_callback
        )

        assert error_callback.call_count > 0

    def test_execute_with_fallback_no_capability_found(self, orchestrator, mock_registry):
        """Execute with fallback returns None when capability not found."""
        mock_registry.find_service_for_capability.return_value = None

        result = orchestrator.execute_with_fallback(
            "nonexistent_capability",
            lambda s: s.do_something()
        )

        assert result is None

    def test_get_service_status(self, orchestrator, mock_registry, sample_capability):
        """Service status can be retrieved."""
        mock_registry.get_registry_stats.return_value = {
            'total_services': 2,
            'available': 1,
            'degraded': 0,
            'unavailable': 1,
            'services': {}
        }
        mock_registry.get_available_services.return_value = [ServiceType.BROWSER_TOOLS]
        mock_registry.get_service_capabilities.return_value = [sample_capability]

        status = orchestrator.get_service_status()

        assert status['summary']['total'] == 2
        assert status['summary']['available'] == 1
        assert 'timestamp' in status
        assert 'available_capabilities' in status

    def test_refresh_all_health(self, orchestrator, mock_registry, sample_health):
        """Refresh all health delegates to registry."""
        mock_registry.check_all_health.return_value = {
            ServiceType.BROWSER_TOOLS: sample_health
        }

        result = orchestrator.refresh_all_health()

        assert ServiceType.BROWSER_TOOLS in result
        mock_registry.check_all_health.assert_called_once()

    def test_get_recommended_service_healthy(self, orchestrator, mock_registry, sample_health):
        """Recommended service returns healthy primary."""
        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.check_health.return_value = sample_health

        recommended = orchestrator.get_recommended_service("test_capability")

        assert recommended == ServiceType.BROWSER_TOOLS

    def test_get_recommended_service_fallback_when_degraded(
        self, orchestrator, mock_registry, sample_capability
    ):
        """Recommended service returns healthy fallback when primary degraded."""
        degraded_health = ServiceHealth(
            service_type=ServiceType.BROWSER_TOOLS,
            status=ServiceStatus.DEGRADED,
            last_check=datetime.now(),
            response_time_ms=100.0,
            error_count=3,
            metadata={}
        )

        healthy_fallback = ServiceHealth(
            service_type=ServiceType.FILESYSTEM,
            status=ServiceStatus.AVAILABLE,
            last_check=datetime.now(),
            response_time_ms=10.0,
            error_count=0,
            metadata={}
        )

        mock_registry.find_service_for_capability.return_value = ServiceType.BROWSER_TOOLS
        mock_registry.get_service_capabilities.return_value = [sample_capability]
        mock_registry.check_health.side_effect = [degraded_health, healthy_fallback]

        recommended = orchestrator.get_recommended_service("test_capability")

        assert recommended == ServiceType.FILESYSTEM

    def test_subscribe_to_service_changes(self, orchestrator, mock_registry):
        """Subscribe to service changes delegates to registry."""
        callback = Mock()

        orchestrator.subscribe_to_service_changes(callback)

        mock_registry.subscribe_to_health_changes.assert_called_once_with(callback)

    def test_diagnose_service_healthy(self, orchestrator, mock_registry, sample_health, sample_capability):
        """Diagnose service shows healthy status."""
        mock_registry.check_health.return_value = sample_health
        mock_registry.get_service_capabilities.return_value = [sample_capability]

        diagnosis = orchestrator.diagnose_service(ServiceType.BROWSER_TOOLS)

        assert diagnosis['service'] == ServiceType.BROWSER_TOOLS.value
        assert diagnosis['healthy'] is True
        assert diagnosis['status'] == ServiceStatus.AVAILABLE.value
        assert len(diagnosis['recommendations']) == 0

    def test_diagnose_service_unhealthy_with_recommendations(
        self, orchestrator, mock_registry, sample_capability
    ):
        """Diagnose service shows recommendations when unhealthy."""
        unhealthy = ServiceHealth(
            service_type=ServiceType.BROWSER_TOOLS,
            status=ServiceStatus.UNAVAILABLE,
            last_check=datetime.now(),
            response_time_ms=0.0,
            error_count=5,
            metadata={'error': 'Connection failed'}
        )

        healthy_fallback = ServiceHealth(
            service_type=ServiceType.FILESYSTEM,
            status=ServiceStatus.AVAILABLE,
            last_check=datetime.now(),
            response_time_ms=10.0,
            error_count=0,
            metadata={}
        )

        mock_registry.check_health.side_effect = [unhealthy, healthy_fallback]
        mock_registry.get_service_capabilities.return_value = [sample_capability]

        diagnosis = orchestrator.diagnose_service(ServiceType.BROWSER_TOOLS)

        assert diagnosis['healthy'] is False
        assert diagnosis['error_count'] == 5
        assert len(diagnosis['recommendations']) > 0
        # Should recommend fallback service
        assert any('fallback' in rec.lower() for rec in diagnosis['recommendations'])

    def test_get_recommended_service_returns_none_when_not_found(self, orchestrator, mock_registry):
        """Recommended service returns None when capability not found."""
        mock_registry.find_service_for_capability.return_value = None

        recommended = orchestrator.get_recommended_service("nonexistent")

        assert recommended is None
