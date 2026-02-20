"""Claude tool schemas for the SwingMCP API."""

TOOLS: list[dict] = [
    {
        "name": "get_component_tree",
        "description": (
            "Get the UI component tree of the Java Swing application. Returns a flat "
            "list of all components with their types, names, text, bounds, and hierarchy. "
            "Use interactable=true to filter to only interactive components (buttons, "
            "fields, tables, etc.)."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "interactable": {
                    "type": "boolean",
                    "description": "If true, return only interactive components",
                    "default": False,
                },
                "types": {
                    "type": "string",
                    "description": (
                        "Comma-separated component types to filter, "
                        "e.g. 'JButton,JTextField'"
                    ),
                },
                "depth": {
                    "type": "integer",
                    "description": "Max depth to traverse",
                },
            },
        },
    },
    {
        "name": "get_component_state",
        "description": (
            "Get detailed state of a specific component by name or ID. For JTable, "
            "returns full row data. For JTree, returns node hierarchy. For JComboBox, "
            "returns all items and selection."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Component name or numeric ID",
                },
                "rows": {
                    "type": "string",
                    "description": "Row range for tables, e.g. '0-9'",
                },
            },
            "required": ["target"],
        },
    },
    {
        "name": "click",
        "description": "Click a component (button, checkbox, menu item, etc.)",
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Component name or ID to click",
                },
            },
            "required": ["target"],
        },
    },
    {
        "name": "type_text",
        "description": "Type text into a text field. Replaces existing content.",
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Text field name or ID",
                },
                "text": {
                    "type": "string",
                    "description": "Text to type",
                },
            },
            "required": ["target", "text"],
        },
    },
    {
        "name": "select_combo_item",
        "description": "Select an item in a combo box by value or index.",
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Combo box name or ID",
                },
                "value": {
                    "type": "string",
                    "description": "Item value to select",
                },
                "index": {
                    "type": "integer",
                    "description": "Item index to select (0-based)",
                },
            },
            "required": ["target"],
        },
    },
    {
        "name": "select_table_row",
        "description": "Select a row in a table.",
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Table name or ID",
                },
                "row": {
                    "type": "integer",
                    "description": "Row index to select (0-based)",
                },
            },
            "required": ["target", "row"],
        },
    },
    {
        "name": "select_tree_node",
        "description": "Select a node in a tree by path.",
        "input_schema": {
            "type": "object",
            "properties": {
                "target": {
                    "type": "string",
                    "description": "Tree name or ID",
                },
                "path": {
                    "type": "string",
                    "description": "Node path, e.g. 'Portfolio > Equities > AAPL'",
                },
            },
            "required": ["target", "path"],
        },
    },
    {
        "name": "click_menu",
        "description": "Click a menu item by path.",
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {
                    "type": "string",
                    "description": (
                        "Menu path, e.g. 'File > Exit' or 'View > Dark Mode'"
                    ),
                },
            },
            "required": ["path"],
        },
    },
    {
        "name": "take_screenshot",
        "description": (
            "Capture a screenshot of the application window or a specific component. "
            "Use this to visually verify the current state of the UI."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "component": {
                    "type": "string",
                    "description": (
                        "Optional component name/ID to capture. "
                        "If omitted, captures entire window."
                    ),
                },
            },
        },
    },
    {
        "name": "check_contrast",
        "description": (
            "Check all text-bearing components for WCAG contrast ratio compliance. "
            "Returns a list of components with insufficient contrast between foreground "
            "and background colors."
        ),
        "input_schema": {
            "type": "object",
            "properties": {},
        },
    },
]
