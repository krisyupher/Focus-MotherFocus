"""Playwright-based browser controller for closing tabs."""
import asyncio
import re
from typing import Optional
from playwright.async_api import async_playwright, Browser, BrowserContext, Page
from src.application.interfaces.i_browser_controller import IBrowserController
from src.core.value_objects.url import URL


class PlaywrightBrowserController(IBrowserController):
    """
    Browser controller using Playwright to close tabs.

    Connects to existing browser instances and can close tabs matching URLs.
    """

    def __init__(self):
        """Initialize the Playwright browser controller."""
        self._playwright = None
        self._browser: Optional[Browser] = None
        self._context: Optional[BrowserContext] = None
        self._initialized = False

    async def _initialize(self):
        """Initialize Playwright connection."""
        if self._initialized:
            return

        try:
            self._playwright = await async_playwright().start()
            # Connect to existing Chrome instance on debugging port
            # User needs to start Chrome with: chrome.exe --remote-debugging-port=9222
            self._browser = await self._playwright.chromium.connect_over_cdp("http://localhost:9222")

            # Get or create default context
            contexts = self._browser.contexts
            if contexts:
                self._context = contexts[0]
            else:
                self._context = await self._browser.new_context()

            self._initialized = True
            print("[PlaywrightController] Connected to browser")
        except Exception as e:
            print(f"[PlaywrightController] Failed to initialize: {e}")
            self._initialized = False

    async def _close(self):
        """Close Playwright connection."""
        if self._playwright:
            await self._playwright.stop()
            self._playwright = None
            self._browser = None
            self._context = None
            self._initialized = False

    def _normalize_url_for_matching(self, url_string: str) -> str:
        """Normalize URL for matching (remove protocol, www, trailing slash)."""
        normalized = url_string.lower()
        normalized = re.sub(r'^https?://', '', normalized)
        normalized = re.sub(r'^www\.', '', normalized)
        normalized = normalized.rstrip('/')
        return normalized

    async def _close_tabs_async(self, url: URL) -> bool:
        """Async implementation of close_tab_with_url."""
        try:
            await self._initialize()

            if not self._initialized or not self._context:
                return False

            target_url = self._normalize_url_for_matching(url.value)
            closed_any = False

            # Get all pages (tabs)
            pages = self._context.pages

            for page in pages:
                try:
                    page_url = page.url
                    normalized_page_url = self._normalize_url_for_matching(page_url)

                    # Check if this page matches the target URL
                    if target_url in normalized_page_url or normalized_page_url in target_url:
                        await page.close()
                        closed_any = True
                except Exception as e:
                    continue  # Silently skip pages that can't be closed

            return closed_any

        except Exception as e:
            return False  # Silently fail

    def close_tab_with_url(self, url: URL) -> bool:
        """
        Close all browser tabs that match the given URL.

        This is a synchronous wrapper around the async implementation.

        Args:
            url: The URL to match and close

        Returns:
            True if any tabs were closed, False otherwise
        """
        try:
            # Run async code in new event loop
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            try:
                result = loop.run_until_complete(self._close_tabs_async(url))
                return result
            finally:
                loop.close()
        except Exception as e:
            return False  # Silently fail

    def close_tab_by_url(self, url_string: str) -> bool:
        """
        Close browser tab by URL string (convenience method).

        Args:
            url_string: URL string to match and close

        Returns:
            True if tab closed successfully
        """
        try:
            url = URL.from_string(url_string)
            return self.close_tab_with_url(url)
        except Exception as e:
            print(f"[PlaywrightController] Error closing tab: {e}")
            return False

    def is_available(self) -> bool:
        """
        Check if browser control is available.

        Returns:
            True if browser control is available, False otherwise
        """
        try:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            try:
                loop.run_until_complete(self._initialize())
                return self._initialized
            finally:
                loop.close()
        except Exception as e:
            print(f"[PlaywrightController] Availability check failed: {e}")
            return False
