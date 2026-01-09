"""
MCP Service Registry Interface.

Defines contracts for service discovery, health monitoring, and orchestration.
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional, Dict, List, Any, Callable
from datetime import datetime
from enum import Enum


class ServiceStatus(Enum):
    """Service availability status."""
    AVAILABLE = "available"
    UNAVAILABLE = "unavailable"
    DEGRADED = "degraded"
    UNKNOWN = "unknown"


class ServiceType(Enum):
    """Types of MCP services."""
    BROWSER_TOOLS = "browser_tools"
    WEBCAM = "webcam"
    HEYGEN = "heygen"
    ELEVENLABS = "elevenlabs"
    MEMORY = "memory"
    FILESYSTEM = "filesystem"
    WINDOWS = "windows"
    NOTIFY = "notify"
    PLAYWRIGHT = "playwright"


@dataclass(frozen=True)
class ServiceHealth:
    """
    Service health information.

    Attributes:
        service_type: Type of service
        status: Current status
        last_check: Last health check timestamp
        response_time_ms: Response time in milliseconds
        error_count: Number of consecutive errors
        metadata: Additional health metrics
    """
    service_type: ServiceType
    status: ServiceStatus
    last_check: datetime
    response_time_ms: float
    error_count: int
    metadata: Dict[str, Any]

    def is_healthy(self) -> bool:
        """Check if service is healthy."""
        return self.status == ServiceStatus.AVAILABLE and self.error_count == 0

    def is_degraded(self) -> bool:
        """Check if service is degraded."""
        return self.status == ServiceStatus.DEGRADED or (
            self.status == ServiceStatus.AVAILABLE and self.error_count > 0
        )


@dataclass(frozen=True)
class ServiceCapability:
    """
    Service capability description.

    Attributes:
        name: Capability name
        description: What this capability does
        parameters: Required parameters
        fallback_services: Alternative services for this capability
    """
    name: str
    description: str
    parameters: List[str]
    fallback_services: List[ServiceType]


class IMCPServiceRegistry(ABC):
    """
    Interface for MCP service registry.

    Responsibilities:
    - Register and discover MCP services
    - Monitor service health
    - Provide fallback mechanisms
    - Track service capabilities
    """

    @abstractmethod
    def register_service(
        self,
        service_type: ServiceType,
        service_instance: Any,
        capabilities: List[ServiceCapability]
    ) -> None:
        """
        Register an MCP service.

        Args:
            service_type: Type of service
            service_instance: Actual service instance
            capabilities: List of capabilities this service provides
        """
        pass

    @abstractmethod
    def unregister_service(self, service_type: ServiceType) -> None:
        """
        Unregister a service.

        Args:
            service_type: Type of service to unregister
        """
        pass

    @abstractmethod
    def get_service(self, service_type: ServiceType) -> Optional[Any]:
        """
        Get service instance by type.

        Args:
            service_type: Type of service

        Returns:
            Service instance or None if not available
        """
        pass

    @abstractmethod
    def get_service_with_fallback(
        self,
        preferred_service: ServiceType,
        fallback_services: List[ServiceType]
    ) -> Optional[Any]:
        """
        Get service with automatic fallback.

        Args:
            preferred_service: Preferred service type
            fallback_services: Fallback services in priority order

        Returns:
            First available service or None
        """
        pass

    @abstractmethod
    def check_health(self, service_type: ServiceType) -> ServiceHealth:
        """
        Check health of a specific service.

        Args:
            service_type: Type of service

        Returns:
            Service health information
        """
        pass

    @abstractmethod
    def check_all_health(self) -> Dict[ServiceType, ServiceHealth]:
        """
        Check health of all registered services.

        Returns:
            Dictionary mapping service types to health info
        """
        pass

    @abstractmethod
    def get_available_services(self) -> List[ServiceType]:
        """
        Get list of currently available services.

        Returns:
            List of available service types
        """
        pass

    @abstractmethod
    def get_service_capabilities(
        self,
        service_type: ServiceType
    ) -> List[ServiceCapability]:
        """
        Get capabilities of a service.

        Args:
            service_type: Type of service

        Returns:
            List of capabilities
        """
        pass

    @abstractmethod
    def find_service_for_capability(
        self,
        capability_name: str
    ) -> Optional[ServiceType]:
        """
        Find service that provides a specific capability.

        Args:
            capability_name: Name of capability needed

        Returns:
            Service type that provides this capability or None
        """
        pass

    @abstractmethod
    def subscribe_to_health_changes(
        self,
        callback: Callable[[ServiceType, ServiceHealth], None]
    ) -> None:
        """
        Subscribe to health status changes.

        Args:
            callback: Function to call when health changes
        """
        pass

    @abstractmethod
    def get_registry_stats(self) -> Dict[str, Any]:
        """
        Get registry statistics.

        Returns:
            Dictionary with stats (total services, healthy, degraded, etc.)
        """
        pass
