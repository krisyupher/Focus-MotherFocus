"""
MCP Service Registry Implementation.

Provides service discovery, health monitoring, and automatic fallbacks for all MCP services.
"""
from typing import Optional, Dict, List, Any, Callable
from datetime import datetime
import time

from src.application.interfaces.i_mcp_service_registry import (
    IMCPServiceRegistry,
    ServiceType,
    ServiceStatus,
    ServiceHealth,
    ServiceCapability
)


class MCPServiceRegistry(IMCPServiceRegistry):
    """
    Centralized registry for all MCP services.

    Features:
    - Service registration and discovery
    - Health monitoring with automatic checks
    - Fallback chain management
    - Capability-based service lookup
    - Health change notifications
    """

    def __init__(self, health_check_interval: float = 30.0):
        """
        Initialize service registry.

        Args:
            health_check_interval: Interval between health checks in seconds
        """
        self._services: Dict[ServiceType, Any] = {}
        self._capabilities: Dict[ServiceType, List[ServiceCapability]] = {}
        self._health: Dict[ServiceType, ServiceHealth] = {}
        self._health_callbacks: List[Callable[[ServiceType, ServiceHealth], None]] = []
        self._health_check_interval = health_check_interval
        self._last_health_check: Dict[ServiceType, datetime] = {}

    def register_service(
        self,
        service_type: ServiceType,
        service_instance: Any,
        capabilities: List[ServiceCapability]
    ) -> None:
        """Register an MCP service."""
        self._services[service_type] = service_instance
        self._capabilities[service_type] = capabilities

        # Initial health check
        health = self._perform_health_check(service_type)
        self._health[service_type] = health
        self._last_health_check[service_type] = datetime.now()

        print(f"[Registry] Registered {service_type.value}: {health.status.value}")

    def unregister_service(self, service_type: ServiceType) -> None:
        """Unregister a service."""
        self._services.pop(service_type, None)
        self._capabilities.pop(service_type, None)
        self._health.pop(service_type, None)
        self._last_health_check.pop(service_type, None)

        print(f"[Registry] Unregistered {service_type.value}")

    def get_service(self, service_type: ServiceType) -> Optional[Any]:
        """Get service instance by type."""
        # Check if health check needed
        self._maybe_refresh_health(service_type)

        service = self._services.get(service_type)
        if service and self._is_service_available(service_type):
            return service
        return None

    def get_service_with_fallback(
        self,
        preferred_service: ServiceType,
        fallback_services: List[ServiceType]
    ) -> Optional[Any]:
        """Get service with automatic fallback."""
        # Try preferred first
        service = self.get_service(preferred_service)
        if service:
            return service

        # Try fallbacks in order
        for fallback in fallback_services:
            service = self.get_service(fallback)
            if service:
                print(f"[Registry] Falling back to {fallback.value} from {preferred_service.value}")
                return service

        return None

    def check_health(self, service_type: ServiceType) -> ServiceHealth:
        """Check health of a specific service."""
        health = self._perform_health_check(service_type)

        # Update stored health
        old_health = self._health.get(service_type)
        self._health[service_type] = health
        self._last_health_check[service_type] = datetime.now()

        # Notify if status changed
        if old_health and old_health.status != health.status:
            self._notify_health_change(service_type, health)

        return health

    def check_all_health(self) -> Dict[ServiceType, ServiceHealth]:
        """Check health of all registered services."""
        results = {}
        for service_type in self._services.keys():
            results[service_type] = self.check_health(service_type)
        return results

    def get_available_services(self) -> List[ServiceType]:
        """Get list of currently available services."""
        available = []
        for service_type in self._services.keys():
            if self._is_service_available(service_type):
                available.append(service_type)
        return available

    def get_service_capabilities(
        self,
        service_type: ServiceType
    ) -> List[ServiceCapability]:
        """Get capabilities of a service."""
        return self._capabilities.get(service_type, [])

    def find_service_for_capability(
        self,
        capability_name: str
    ) -> Optional[ServiceType]:
        """Find service that provides a specific capability."""
        for service_type, capabilities in self._capabilities.items():
            for capability in capabilities:
                if capability.name == capability_name:
                    # Check if service is available
                    if self._is_service_available(service_type):
                        return service_type
        return None

    def subscribe_to_health_changes(
        self,
        callback: Callable[[ServiceType, ServiceHealth], None]
    ) -> None:
        """Subscribe to health status changes."""
        self._health_callbacks.append(callback)

    def get_registry_stats(self) -> Dict[str, Any]:
        """Get registry statistics."""
        total = len(self._services)
        available = len([s for s in self._services.keys() if self._is_service_available(s)])
        degraded = len([
            s for s in self._services.keys()
            if self._health.get(s) and self._health[s].is_degraded()
        ])
        unavailable = total - available

        return {
            'total_services': total,
            'available': available,
            'degraded': degraded,
            'unavailable': unavailable,
            'services': {
                service_type.value: {
                    'status': self._health[service_type].status.value,
                    'error_count': self._health[service_type].error_count,
                    'response_time_ms': self._health[service_type].response_time_ms
                }
                for service_type in self._services.keys()
                if service_type in self._health
            }
        }

    def _perform_health_check(self, service_type: ServiceType) -> ServiceHealth:
        """
        Perform health check on a service.

        Args:
            service_type: Service to check

        Returns:
            Service health information
        """
        service = self._services.get(service_type)
        if not service:
            return ServiceHealth(
                service_type=service_type,
                status=ServiceStatus.UNAVAILABLE,
                last_check=datetime.now(),
                response_time_ms=0.0,
                error_count=0,
                metadata={'reason': 'not_registered'}
            )

        # Check if service has is_available method
        start_time = time.time()
        try:
            if hasattr(service, 'is_available'):
                is_available = service.is_available()
                response_time = (time.time() - start_time) * 1000

                if is_available:
                    status = ServiceStatus.AVAILABLE
                    error_count = 0
                else:
                    status = ServiceStatus.UNAVAILABLE
                    error_count = self._get_error_count(service_type) + 1

                return ServiceHealth(
                    service_type=service_type,
                    status=status,
                    last_check=datetime.now(),
                    response_time_ms=response_time,
                    error_count=error_count,
                    metadata={'checked': True}
                )
            else:
                # Service doesn't have health check - assume available
                return ServiceHealth(
                    service_type=service_type,
                    status=ServiceStatus.AVAILABLE,
                    last_check=datetime.now(),
                    response_time_ms=0.0,
                    error_count=0,
                    metadata={'assumed_available': True}
                )

        except Exception as e:
            error_count = self._get_error_count(service_type) + 1
            return ServiceHealth(
                service_type=service_type,
                status=ServiceStatus.UNAVAILABLE,
                last_check=datetime.now(),
                response_time_ms=0.0,
                error_count=error_count,
                metadata={'error': str(e)}
            )

    def _is_service_available(self, service_type: ServiceType) -> bool:
        """Check if service is currently available."""
        health = self._health.get(service_type)
        if not health:
            return False
        return health.status == ServiceStatus.AVAILABLE

    def _get_error_count(self, service_type: ServiceType) -> int:
        """Get current error count for service."""
        health = self._health.get(service_type)
        if health:
            return health.error_count
        return 0

    def _maybe_refresh_health(self, service_type: ServiceType) -> None:
        """Refresh health check if needed."""
        last_check = self._last_health_check.get(service_type)
        if not last_check:
            return

        elapsed = (datetime.now() - last_check).total_seconds()
        if elapsed >= self._health_check_interval:
            self.check_health(service_type)

    def _notify_health_change(
        self,
        service_type: ServiceType,
        new_health: ServiceHealth
    ) -> None:
        """Notify subscribers of health change."""
        for callback in self._health_callbacks:
            try:
                callback(service_type, new_health)
            except Exception as e:
                print(f"[Registry] Health callback error: {e}")
