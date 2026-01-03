"""
Orchestrate MCP Services Use Case.

Coordinates all MCP services with automatic discovery, health monitoring, and fallbacks.
"""
from typing import Optional, Dict, Any, Callable
from datetime import datetime

from src.application.interfaces.i_mcp_service_registry import (
    IMCPServiceRegistry,
    ServiceType,
    ServiceHealth,
    ServiceCapability
)


class OrchestrateMCPServicesUseCase:
    """
    Use case for orchestrating all MCP services.

    Responsibilities:
    - Initialize and configure all MCP services
    - Monitor service health continuously
    - Execute operations with automatic fallbacks
    - Provide unified access to all capabilities
    - Handle service failures gracefully
    """

    def __init__(self, registry: IMCPServiceRegistry):
        """
        Initialize MCP orchestrator.

        Args:
            registry: Service registry for discovery and health
        """
        self.registry = registry
        self._health_monitor_active = False

    def execute_with_fallback(
        self,
        capability_name: str,
        operation: Callable[[Any], Any],
        fallback_services: Optional[list[ServiceType]] = None,
        on_error: Optional[Callable[[Exception], None]] = None
    ) -> Optional[Any]:
        """
        Execute operation with automatic fallback.

        Args:
            capability_name: Name of capability needed
            operation: Function to execute with service (receives service instance)
            fallback_services: Optional explicit fallback chain
            on_error: Callback for error handling

        Returns:
            Operation result or None if all services failed
        """
        # Find service for capability
        primary_service = self.registry.find_service_for_capability(capability_name)

        if not primary_service:
            print(f"[Orchestrator] No service found for capability: {capability_name}")
            return None

        # Build fallback chain
        if fallback_services is None:
            # Use capability-defined fallbacks
            capabilities = self.registry.get_service_capabilities(primary_service)
            for cap in capabilities:
                if cap.name == capability_name:
                    fallback_services = cap.fallback_services
                    break

        if fallback_services is None:
            fallback_services = []

        # Try primary service
        service = self.registry.get_service(primary_service)
        if service:
            try:
                result = operation(service)
                return result
            except Exception as e:
                print(f"[Orchestrator] Primary service {primary_service.value} failed: {e}")
                if on_error:
                    on_error(e)

        # Try fallbacks
        for fallback_type in fallback_services:
            service = self.registry.get_service(fallback_type)
            if service:
                try:
                    print(f"[Orchestrator] Trying fallback: {fallback_type.value}")
                    result = operation(service)
                    return result
                except Exception as e:
                    print(f"[Orchestrator] Fallback {fallback_type.value} failed: {e}")
                    if on_error:
                        on_error(e)

        print(f"[Orchestrator] All services failed for capability: {capability_name}")
        return None

    def get_service_status(self) -> Dict[str, Any]:
        """
        Get status of all MCP services.

        Returns:
            Dictionary with service statuses and health info
        """
        stats = self.registry.get_registry_stats()
        available_services = self.registry.get_available_services()

        return {
            'timestamp': datetime.now().isoformat(),
            'summary': {
                'total': stats['total_services'],
                'available': stats['available'],
                'degraded': stats['degraded'],
                'unavailable': stats['unavailable']
            },
            'services': stats['services'],
            'available_capabilities': self._get_available_capabilities(available_services)
        }

    def refresh_all_health(self) -> Dict[ServiceType, ServiceHealth]:
        """
        Refresh health status of all services.

        Returns:
            Dictionary mapping service types to health info
        """
        return self.registry.check_all_health()

    def get_recommended_service(
        self,
        capability_name: str
    ) -> Optional[ServiceType]:
        """
        Get recommended service for a capability based on health.

        Args:
            capability_name: Name of capability needed

        Returns:
            Recommended service type or None
        """
        service_type = self.registry.find_service_for_capability(capability_name)
        if not service_type:
            return None

        # Check health
        health = self.registry.check_health(service_type)
        if health.is_healthy():
            return service_type

        # Find healthier fallback
        capabilities = self.registry.get_service_capabilities(service_type)
        for cap in capabilities:
            if cap.name == capability_name:
                for fallback in cap.fallback_services:
                    fallback_health = self.registry.check_health(fallback)
                    if fallback_health.is_healthy():
                        return fallback

        # Return original even if degraded
        return service_type

    def subscribe_to_service_changes(
        self,
        callback: Callable[[ServiceType, ServiceHealth], None]
    ) -> None:
        """
        Subscribe to service health changes.

        Args:
            callback: Function to call when service health changes
        """
        self.registry.subscribe_to_health_changes(callback)

    def _get_available_capabilities(
        self,
        available_services: list[ServiceType]
    ) -> list[str]:
        """
        Get list of all available capabilities.

        Args:
            available_services: List of available service types

        Returns:
            List of capability names
        """
        capabilities = set()
        for service_type in available_services:
            service_caps = self.registry.get_service_capabilities(service_type)
            for cap in service_caps:
                capabilities.add(cap.name)
        return sorted(list(capabilities))

    def diagnose_service(self, service_type: ServiceType) -> Dict[str, Any]:
        """
        Diagnose issues with a specific service.

        Args:
            service_type: Service to diagnose

        Returns:
            Diagnostic information
        """
        health = self.registry.check_health(service_type)
        capabilities = self.registry.get_service_capabilities(service_type)

        diagnosis = {
            'service': service_type.value,
            'status': health.status.value,
            'healthy': health.is_healthy(),
            'degraded': health.is_degraded(),
            'error_count': health.error_count,
            'response_time_ms': health.response_time_ms,
            'last_check': health.last_check.isoformat(),
            'metadata': health.metadata,
            'capabilities': [cap.name for cap in capabilities],
            'recommendations': []
        }

        # Add recommendations
        if not health.is_healthy():
            if health.error_count > 0:
                diagnosis['recommendations'].append(
                    f"Service has {health.error_count} consecutive errors - check service configuration"
                )
            if health.status.value == "unavailable":
                diagnosis['recommendations'].append(
                    "Service is unavailable - verify service is running and accessible"
                )

            # Suggest fallbacks
            fallback_available = []
            for cap in capabilities:
                for fallback in cap.fallback_services:
                    fallback_health = self.registry.check_health(fallback)
                    if fallback_health.is_healthy():
                        fallback_available.append(fallback.value)

            if fallback_available:
                diagnosis['recommendations'].append(
                    f"Available fallback services: {', '.join(fallback_available)}"
                )

        return diagnosis
