"""HTTP client for the SwingMCP server API."""

import requests


class SwingClient:
    """Client for interacting with the SwingMCP HTTP API."""

    def __init__(self, base_url: str = "http://localhost:9222") -> None:
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()

    def get_tree(
        self,
        interactable: bool = True,
        types: str | None = None,
        depth: int | None = None,
    ) -> list[dict]:
        """Get the UI component tree.

        Args:
            interactable: If True, return only interactive components.
            types: Comma-separated component types to filter.
            depth: Max depth to traverse.

        Returns:
            List of component node dicts.
        """
        params: dict[str, str] = {}
        if interactable is not None:
            params["interactable"] = str(interactable).lower()
        if types:
            params["types"] = types
        if depth is not None:
            params["depth"] = str(depth)

        resp = self.session.get(f"{self.base_url}/tree", params=params)
        resp.raise_for_status()
        return resp.json()

    def get_component(self, target: str, rows: str | None = None) -> dict:
        """Get detailed state of a specific component.

        Args:
            target: Component name or numeric ID.
            rows: Row range for tables, e.g. '0-9'.

        Returns:
            Component state dict.
        """
        params: dict[str, str] = {}
        if rows:
            params["rows"] = rows

        resp = self.session.get(
            f"{self.base_url}/component/{target}", params=params
        )
        resp.raise_for_status()
        return resp.json()

    def action(self, action: str, target: str | None = None, **kwargs: object) -> dict:
        """Execute an interaction action.

        Args:
            action: Action type (click, type, select_combo, etc.).
            target: Component name or ID.
            **kwargs: Additional action parameters (text, value, index, row, path).

        Returns:
            Action result dict.
        """
        body: dict[str, object] = {"action": action}
        if target is not None:
            body["target"] = target
        body.update(kwargs)

        resp = self.session.post(f"{self.base_url}/action", json=body)
        resp.raise_for_status()
        return resp.json()

    def screenshot(self, component: str | None = None) -> tuple[str, int, int]:
        """Capture a screenshot.

        Args:
            component: Optional component name/ID to capture.

        Returns:
            Tuple of (base64_data, width, height).
        """
        params: dict[str, str] = {"format": "base64"}
        if component:
            params["component"] = component

        resp = self.session.get(f"{self.base_url}/screenshot", params=params)
        resp.raise_for_status()
        data = resp.json()

        # Strip the data URI prefix if present
        image_b64 = data["image"]
        if image_b64.startswith("data:"):
            image_b64 = image_b64.split(",", 1)[1]

        return image_b64, data["width"], data["height"]

    def contrast_check(self) -> dict:
        """Check all text-bearing components for WCAG contrast compliance.

        Returns:
            Dict with issues, totalChecked, and totalIssues.
        """
        resp = self.session.get(f"{self.base_url}/contrast")
        resp.raise_for_status()
        return resp.json()

    def health(self) -> dict:
        """Check server health.

        Returns:
            Dict with status, components count, and uptime.
        """
        resp = self.session.get(f"{self.base_url}/health")
        resp.raise_for_status()
        return resp.json()
