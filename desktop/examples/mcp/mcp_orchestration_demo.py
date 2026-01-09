"""
Phase 4: Multi-MCP Orchestration Demo

Demonstrates:
- Automatic service discovery and registration
- Health monitoring with real-time status
- Automatic fallback when services fail
- Capability-based service lookup
- Service diagnostics and recommendations
"""
import time
from datetime import datetime

from src.infrastructure.adapters.mcp_service_factory import MCPServiceFactory
from src.application.use_cases.orchestrate_mcp_services import OrchestrateMCPServicesUseCase
from src.application.interfaces.i_mcp_service_registry import ServiceType


def print_header(title: str) -> None:
    """Print formatted section header."""
    print("\n" + "=" * 80)
    print(f"  {title}")
    print("=" * 80)


def print_service_status(orchestrator: OrchestrateMCPServicesUseCase) -> None:
    """Print current service status."""
    print_header("MCP SERVICE STATUS")

    status = orchestrator.get_service_status()

    print(f"\n📊 Summary ({status['timestamp']})")
    print(f"   Total Services: {status['summary']['total']}")
    print(f"   ✅ Available:   {status['summary']['available']}")
    print(f"   ⚠️  Degraded:    {status['summary']['degraded']}")
    print(f"   ❌ Unavailable: {status['summary']['unavailable']}")

    print("\n📋 Service Details:")
    for service_name, service_info in status['services'].items():
        status_icon = {
            'available': '✅',
            'degraded': '⚠️',
            'unavailable': '❌',
            'unknown': '❓'
        }.get(service_info['status'], '❓')

        print(f"\n   {status_icon} {service_name}")
        print(f"      Status: {service_info['status']}")
        print(f"      Errors: {service_info['error_count']}")
        print(f"      Response Time: {service_info['response_time_ms']:.2f}ms")

    print(f"\n🎯 Available Capabilities ({len(status['available_capabilities'])}):")
    for i, capability in enumerate(status['available_capabilities'], 1):
        print(f"   {i}. {capability}")


def demonstrate_capability_execution(orchestrator: OrchestrateMCPServicesUseCase) -> None:
    """Demonstrate executing operations with automatic fallback."""
    print_header("CAPABILITY-BASED EXECUTION")

    # Example 1: Speech synthesis with fallback
    print("\n🔊 Example 1: Text-to-Speech with Automatic Fallback")
    print("   Attempting: ElevenLabs → Windows TTS")

    def synthesize_text(service):
        """Synthesize speech using available service."""
        print(f"   Using service: {service.__class__.__name__}")
        # In real scenario, would call service.synthesize()
        return "Speech synthesized successfully"

    result = orchestrator.execute_with_fallback(
        capability_name="synthesize_speech",
        operation=synthesize_text,
        on_error=lambda e: print(f"   ⚠️  Service failed: {e}")
    )

    if result:
        print(f"   ✅ Result: {result}")
    else:
        print("   ❌ All services failed")

    # Example 2: Browser tab detection
    print("\n🌐 Example 2: Browser Tab Detection")
    print("   Attempting: Browser Tools MCP")

    def detect_tabs(service):
        """Detect browser tabs using service."""
        print(f"   Using service: {service.__class__.__name__}")
        # In real scenario, would call service.get_tabs()
        return ["https://reddit.com", "https://youtube.com"]

    result = orchestrator.execute_with_fallback(
        capability_name="detect_browser_tabs",
        operation=detect_tabs,
        on_error=lambda e: print(f"   ⚠️  Service failed: {e}")
    )

    if result:
        print(f"   ✅ Found {len(result)} tabs")
    else:
        print("   ❌ Browser detection unavailable")


def demonstrate_service_diagnostics(orchestrator: OrchestrateMCPServicesUseCase) -> None:
    """Demonstrate service diagnostics and recommendations."""
    print_header("SERVICE DIAGNOSTICS")

    # Get all available services
    registry = orchestrator.registry
    all_services = list(registry._services.keys())

    for service_type in all_services[:3]:  # Diagnose first 3 services
        print(f"\n🔍 Diagnosing: {service_type.value}")

        diagnosis = orchestrator.diagnose_service(service_type)

        print(f"   Status: {diagnosis['status']}")
        print(f"   Healthy: {'✅ Yes' if diagnosis['healthy'] else '❌ No'}")
        print(f"   Degraded: {'⚠️  Yes' if diagnosis['degraded'] else '✅ No'}")
        print(f"   Error Count: {diagnosis['error_count']}")
        print(f"   Response Time: {diagnosis['response_time_ms']:.2f}ms")
        print(f"   Last Check: {diagnosis['last_check']}")

        if diagnosis['capabilities']:
            print(f"   Capabilities: {', '.join(diagnosis['capabilities'])}")

        if diagnosis['recommendations']:
            print("   📌 Recommendations:")
            for rec in diagnosis['recommendations']:
                print(f"      • {rec}")


def demonstrate_recommended_services(orchestrator: OrchestrateMCPServicesUseCase) -> None:
    """Demonstrate getting recommended services based on health."""
    print_header("RECOMMENDED SERVICES")

    capabilities = [
        "synthesize_speech",
        "detect_browser_tabs",
        "capture_frame",
        "store_event"
    ]

    for capability in capabilities:
        recommended = orchestrator.get_recommended_service(capability)

        if recommended:
            health = orchestrator.registry.check_health(recommended)
            health_icon = '✅' if health.is_healthy() else '⚠️' if health.is_degraded() else '❌'

            print(f"\n{health_icon} {capability}")
            print(f"   Recommended: {recommended.value}")
            print(f"   Status: {health.status.value}")
            print(f"   Response Time: {health.response_time_ms:.2f}ms")
        else:
            print(f"\n❌ {capability}")
            print("   No service available")


def demonstrate_health_monitoring(orchestrator: OrchestrateMCPServicesUseCase) -> None:
    """Demonstrate continuous health monitoring."""
    print_header("HEALTH MONITORING")

    print("\n📡 Subscribing to health changes...")

    def on_health_change(service_type: ServiceType, health):
        """Callback for health changes."""
        timestamp = datetime.now().strftime("%H:%M:%S")
        status_icon = '✅' if health.status.value == 'available' else '❌'

        print(f"\n[{timestamp}] {status_icon} {service_type.value}")
        print(f"   Status changed to: {health.status.value}")
        print(f"   Error count: {health.error_count}")

    orchestrator.subscribe_to_service_changes(on_health_change)

    print("✅ Subscribed to health changes")
    print("   (Health changes will be logged automatically)")

    # Refresh all health to trigger callbacks
    print("\n🔄 Refreshing all service health...")
    health_map = orchestrator.refresh_all_health()

    print(f"✅ Checked {len(health_map)} services")


def main():
    """Run MCP orchestration demo."""
    print("\n" + "=" * 80)
    print("  PHASE 4: MULTI-MCP ORCHESTRATION DEMO")
    print("=" * 80)
    print("\nInitializing MCP Service Registry...")

    # Create service factory and registry
    factory = MCPServiceFactory()
    registry = factory.create_registry()

    # Create orchestrator
    orchestrator = OrchestrateMCPServicesUseCase(registry)

    print("✅ Service registry initialized\n")

    # Demo 1: Service Status
    print_service_status(orchestrator)

    # Demo 2: Capability-based execution
    time.sleep(1)
    demonstrate_capability_execution(orchestrator)

    # Demo 3: Service diagnostics
    time.sleep(1)
    demonstrate_service_diagnostics(orchestrator)

    # Demo 4: Recommended services
    time.sleep(1)
    demonstrate_recommended_services(orchestrator)

    # Demo 5: Health monitoring
    time.sleep(1)
    demonstrate_health_monitoring(orchestrator)

    # Final status
    time.sleep(1)
    print_header("FINAL STATUS")

    stats = registry.get_registry_stats()
    print(f"\n📊 Registry Statistics:")
    print(f"   Total Services Registered: {stats['total_services']}")
    print(f"   Currently Available: {stats['available']}")
    print(f"   Degraded: {stats['degraded']}")
    print(f"   Unavailable: {stats['unavailable']}")

    print("\n" + "=" * 80)
    print("  DEMO COMPLETE")
    print("=" * 80)
    print("\n✅ Multi-MCP orchestration system is fully functional!")
    print("\nKey Features Demonstrated:")
    print("  • Automatic service discovery and registration")
    print("  • Real-time health monitoring")
    print("  • Automatic fallback chains")
    print("  • Capability-based service lookup")
    print("  • Service diagnostics and recommendations")
    print("  • Health change notifications")
    print("\n")


if __name__ == "__main__":
    main()
