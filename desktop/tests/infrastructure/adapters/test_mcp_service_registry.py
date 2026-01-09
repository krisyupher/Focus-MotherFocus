"""
Tests for MCP Service Registry.
"""
import pytest
from unittest.mock import Mock
from datetime import datetime

from src.infrastructure.adapters.mcp_service_registry import MCPServiceRegistry
from src.application.interfaces.i_mcp_service_registry import (
    ServiceType,
    ServiceStatus,
    ServiceCapability
)


@pytest.fixture
def registry():
    """Create service registry."""
    return MCPServiceRegistry(health_check_interval=60.0)


@pytest.fixture
def mock_service():
    """Create mock service with is_available method."""
    service = Mock()
    service.is_available.return_value = True
    return service


@pytest.fixture
def sample_capabilities():
    """Create sample service capabilities."""
    return [
        ServiceCapability(
            name="test_capability",
            description="Test capability",
            parameters=["param1"],
            fallback_services=[ServiceType.FILESYSTEM]
        )
    ]


class TestMCPServiceRegistry:
    """Test service registry functionality."""

    def test_register_service(self, registry, mock_service, sample_capabilities):
        """Service can be registered."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        service = registry.get_service(ServiceType.BROWSER_TOOLS)
        assert service is mock_service

    def test_unregister_service(self, registry, mock_service, sample_capabilities):
        """Service can be unregistered."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        registry.unregister_service(ServiceType.BROWSER_TOOLS)
        service = registry.get_service(ServiceType.BROWSER_TOOLS)
        assert service is None

    def test_get_service_returns_none_for_unavailable(self, registry, sample_capabilities):
        """Get service returns None when service unavailable."""
        unavailable_service = Mock()
        unavailable_service.is_available.return_value = False

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            unavailable_service,
            sample_capabilities
        )

        service = registry.get_service(ServiceType.BROWSER_TOOLS)
        assert service is None

    def test_get_service_with_fallback_returns_primary(self, registry, mock_service, sample_capabilities):
        """Fallback returns primary service when available."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        service = registry.get_service_with_fallback(
            ServiceType.BROWSER_TOOLS,
            [ServiceType.FILESYSTEM]
        )

        assert service is mock_service

    def test_get_service_with_fallback_uses_fallback(self, registry, sample_capabilities):
        """Fallback uses fallback service when primary unavailable."""
        # Primary unavailable
        unavailable_service = Mock()
        unavailable_service.is_available.return_value = False

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            unavailable_service,
            sample_capabilities
        )

        # Fallback available
        fallback_service = Mock()
        fallback_service.is_available.return_value = True

        registry.register_service(
            ServiceType.FILESYSTEM,
            fallback_service,
            sample_capabilities
        )

        service = registry.get_service_with_fallback(
            ServiceType.BROWSER_TOOLS,
            [ServiceType.FILESYSTEM]
        )

        assert service is fallback_service

    def test_check_health_returns_available(self, registry, mock_service, sample_capabilities):
        """Health check returns available for healthy service."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        health = registry.check_health(ServiceType.BROWSER_TOOLS)

        assert health.status == ServiceStatus.AVAILABLE
        assert health.error_count == 0

    def test_check_health_returns_unavailable(self, registry, sample_capabilities):
        """Health check returns unavailable for failed service."""
        failing_service = Mock()
        failing_service.is_available.return_value = False

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            failing_service,
            sample_capabilities
        )

        health = registry.check_health(ServiceType.BROWSER_TOOLS)

        assert health.status == ServiceStatus.UNAVAILABLE
        assert health.error_count > 0

    def test_check_all_health(self, registry, mock_service, sample_capabilities):
        """Check all health returns all services."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        registry.register_service(
            ServiceType.WEBCAM,
            mock_service,
            sample_capabilities
        )

        all_health = registry.check_all_health()

        assert ServiceType.BROWSER_TOOLS in all_health
        assert ServiceType.WEBCAM in all_health
        assert len(all_health) == 2

    def test_get_available_services(self, registry, mock_service, sample_capabilities):
        """Get available services returns only available ones."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        unavailable = Mock()
        unavailable.is_available.return_value = False

        registry.register_service(
            ServiceType.WEBCAM,
            unavailable,
            sample_capabilities
        )

        available = registry.get_available_services()

        assert ServiceType.BROWSER_TOOLS in available
        assert ServiceType.WEBCAM not in available

    def test_get_service_capabilities(self, registry, mock_service, sample_capabilities):
        """Service capabilities can be retrieved."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        capabilities = registry.get_service_capabilities(ServiceType.BROWSER_TOOLS)

        assert len(capabilities) == 1
        assert capabilities[0].name == "test_capability"

    def test_find_service_for_capability(self, registry, mock_service, sample_capabilities):
        """Service can be found by capability name."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        found = registry.find_service_for_capability("test_capability")

        assert found == ServiceType.BROWSER_TOOLS

    def test_find_service_for_capability_returns_none(self, registry):
        """Find service returns None when capability not found."""
        found = registry.find_service_for_capability("nonexistent")
        assert found is None

    def test_subscribe_to_health_changes(self, registry, sample_capabilities):
        """Health change subscriptions work."""
        callback = Mock()
        registry.subscribe_to_health_changes(callback)

        # Register service (triggers initial health check)
        service = Mock()
        service.is_available.return_value = True

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            service,
            sample_capabilities
        )

        # Change health status
        service.is_available.return_value = False
        registry.check_health(ServiceType.BROWSER_TOOLS)

        # Callback should be called
        assert callback.call_count > 0

    def test_get_registry_stats(self, registry, mock_service, sample_capabilities):
        """Registry stats can be retrieved."""
        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            mock_service,
            sample_capabilities
        )

        stats = registry.get_registry_stats()

        assert stats['total_services'] == 1
        assert stats['available'] == 1
        assert ServiceType.BROWSER_TOOLS.value in stats['services']

    def test_health_check_handles_exception(self, registry, sample_capabilities):
        """Health check handles service exceptions."""
        failing_service = Mock()
        failing_service.is_available.side_effect = Exception("Test error")

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            failing_service,
            sample_capabilities
        )

        health = registry.check_health(ServiceType.BROWSER_TOOLS)

        assert health.status == ServiceStatus.UNAVAILABLE
        assert "error" in health.metadata

    def test_service_without_is_available_assumed_available(self, registry, sample_capabilities):
        """Service without is_available method is assumed available."""
        simple_service = Mock(spec=[])  # No is_available method

        registry.register_service(
            ServiceType.BROWSER_TOOLS,
            simple_service,
            sample_capabilities
        )

        health = registry.check_health(ServiceType.BROWSER_TOOLS)

        assert health.status == ServiceStatus.AVAILABLE
        assert health.metadata.get('assumed_available') is True
