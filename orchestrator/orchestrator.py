"""Main agent loop connecting Claude to the SwingMCP server."""

from __future__ import annotations

import json
import sys

import anthropic

from swing_client import SwingClient
from tool_definitions import TOOLS

client = anthropic.Anthropic()
swing = SwingClient("http://localhost:9222")

SYSTEM_PROMPT = """You are an AI agent testing a Java Swing trading application. You have tools to:
- Inspect the UI component tree (get_component_tree)
- Read detailed component state including table data, tree nodes, combo items (get_component_state)
- Interact with the app: click buttons, type text, select items (click, type_text, select_combo_item, select_table_row, select_tree_node, click_menu)
- Take screenshots to visually verify the UI state (take_screenshot)
- Check for visual contrast/accessibility issues (check_contrast)

IMPORTANT GUIDELINES:
- Always start by getting the component tree to understand the app layout.
- Use component NAMES (not IDs) when possible for readability.
- After performing actions, verify the result by checking component state or taking a screenshot.
- When reporting findings, be specific about component names, values, and states.
- Screenshots show you what the USER sees. Component state shows you the DATA. Use both.

CONTEXT EFFICIENCY — YOU HAVE A LIMITED CONTEXT WINDOW:
- Do NOT call get_component_tree repeatedly. Call it once at the start, then work from memory. Only re-fetch if the UI structure may have changed significantly (e.g., new dialog opened).
- Do NOT take screenshots after every single action. Take them at key verification points: after completing a workflow, before/after a visual change, when debugging an unexpected result.
- When reading table data, request only the rows you need. Use the rows parameter (e.g., rows='0-9') instead of fetching all rows.
- Prefer get_component_state for a specific component over get_component_tree for the whole app when you only need to check one thing.
- Combine related checks: if you need to verify multiple fields after an action, get their states in consecutive calls rather than taking a screenshot for each.
- When reporting results, be concise. State what you found and what it means, not a play-by-play of every tool call.
"""


def handle_tool_call(tool_name: str, tool_input: dict) -> dict | list | object:
    """Execute a tool call against the Swing app and return the result.

    Args:
        tool_name: The tool to invoke.
        tool_input: The tool input parameters.

    Returns:
        The tool result (dict, list, or special image dict).
    """
    if tool_name == "get_component_tree":
        return swing.get_tree(**tool_input)
    elif tool_name == "get_component_state":
        target = tool_input["target"]
        rows = tool_input.get("rows")
        return swing.get_component(target, rows=rows)
    elif tool_name == "click":
        return swing.action("click", tool_input["target"])
    elif tool_name == "type_text":
        return swing.action("type", tool_input["target"], text=tool_input["text"])
    elif tool_name == "select_combo_item":
        kwargs: dict[str, object] = {}
        if "value" in tool_input:
            kwargs["value"] = tool_input["value"]
        if "index" in tool_input:
            kwargs["index"] = tool_input["index"]
        return swing.action("select_combo", tool_input["target"], **kwargs)
    elif tool_name == "select_table_row":
        return swing.action("select_row", tool_input["target"], row=tool_input["row"])
    elif tool_name == "select_tree_node":
        return swing.action(
            "select_tree", tool_input["target"], path=tool_input["path"]
        )
    elif tool_name == "click_menu":
        return swing.action("menu", path=tool_input["path"])
    elif tool_name == "take_screenshot":
        b64, w, h = swing.screenshot(tool_input.get("component"))
        return {"type": "image", "base64": b64, "width": w, "height": h}
    elif tool_name == "check_contrast":
        return swing.contrast_check()
    else:
        return {"error": f"Unknown tool: {tool_name}"}


def estimate_tokens(messages: list[dict]) -> int:
    """Rough token estimate for the messages list.

    Args:
        messages: The conversation messages.

    Returns:
        Estimated token count.
    """
    total = 0
    for msg in messages:
        content = msg.get("content", "")
        if isinstance(content, str):
            total += len(content) // 4
        elif isinstance(content, list):
            for block in content:
                if isinstance(block, dict):
                    if block.get("type") == "image" or (
                        block.get("source", {}).get("type") == "base64"
                    ):
                        total += 1600
                    elif "text" in block:
                        total += len(block["text"]) // 4
                    elif "content" in block:
                        c = block["content"]
                        if isinstance(c, str):
                            total += len(c) // 4
                        elif isinstance(c, list):
                            for sub in c:
                                if isinstance(sub, dict):
                                    if sub.get("type") == "image" or sub.get(
                                        "source", {}
                                    ).get("type") == "base64":
                                        total += 1600
                                    elif "text" in sub:
                                        total += len(sub["text"]) // 4
                elif isinstance(block, str):
                    total += len(block) // 4
    return total


def summarize_json_result(data: object) -> str | None:
    """Create a compact summary of a large JSON tool result.

    Args:
        data: Parsed JSON data.

    Returns:
        Summary string, or None if no summarization is applicable.
    """
    if isinstance(data, list) and len(data) > 5:
        # Component tree
        if all(
            isinstance(item, dict) and "type" in item and "bounds" in item
            for item in data[:3]
        ):
            names = [item.get("name") for item in data if item.get("name")]
            name_str = ", ".join(names[:15])
            if len(names) > 15:
                name_str += "..."
            return (
                f"[Component tree: {len(data)} components. Named: {name_str}]"
            )

    if isinstance(data, dict):
        # Table data
        if "data" in data and isinstance(data["data"], list) and "columns" in data:
            return (
                f"[Table '{data.get('name', '?')}': {len(data['data'])} rows, "
                f"columns: {data.get('columns')}]"
            )

        # Tree data
        if "nodes" in data and isinstance(data["nodes"], list):
            return (
                f"[Tree '{data.get('name', '?')}': {len(data['nodes'])} nodes, "
                f"selected: {data.get('selectedPath')}]"
            )

        # Contrast check
        if "issues" in data and "totalChecked" in data:
            return (
                f"[Contrast check: {data['totalIssues']} issues out of "
                f"{data['totalChecked']} components checked]"
            )

    return None


def summarize_tool_results(msg: dict) -> dict:
    """Replace large tool result content with compact summaries.

    Args:
        msg: A message dict containing tool results.

    Returns:
        A new message dict with compacted content.
    """
    new_content: list[dict] = []
    for block in msg["content"]:
        if not isinstance(block, dict) or block.get("type") != "tool_result":
            new_content.append(block)
            continue

        result_content = block.get("content", "")

        # Handle image content — remove images, keep text note
        if isinstance(result_content, list):
            has_image = any(
                c.get("type") == "image"
                for c in result_content
                if isinstance(c, dict)
            )
            if has_image:
                text_parts = [
                    c.get("text", "")
                    for c in result_content
                    if isinstance(c, dict) and c.get("type") == "text"
                ]
                new_content.append(
                    {
                        **block,
                        "content": (
                            "[Screenshot removed to save context. "
                            f"Original note: {' '.join(text_parts)}]"
                        ),
                    }
                )
                continue

        # Handle JSON string content
        if isinstance(result_content, str):
            try:
                data = json.loads(result_content)
                summary = summarize_json_result(data)
                if summary:
                    new_content.append({**block, "content": summary})
                    continue
            except (json.JSONDecodeError, TypeError):
                pass

        # Default: keep as-is if small enough, truncate if large
        if isinstance(result_content, str) and len(result_content) > 2000:
            new_content.append(
                {**block, "content": result_content[:500] + "\n...[truncated]..."}
            )
        else:
            new_content.append(block)

    return {**msg, "content": new_content}


def compact_messages(
    messages: list[dict], keep_recent_turns: int = 5
) -> list[dict]:
    """Replace old, large tool results with compact summaries.

    Keeps the last ``keep_recent_turns`` worth of tool_result messages intact.

    Args:
        messages: The full conversation messages list.
        keep_recent_turns: Number of recent tool-result messages to preserve.

    Returns:
        A new messages list with compacted older entries.
    """
    tool_result_indices = [
        i
        for i, m in enumerate(messages)
        if m.get("role") == "user"
        and isinstance(m.get("content"), list)
        and any(
            c.get("type") == "tool_result"
            for c in m["content"]
            if isinstance(c, dict)
        )
    ]

    indices_to_compact = (
        tool_result_indices[:-keep_recent_turns]
        if len(tool_result_indices) > keep_recent_turns
        else []
    )

    compacted: list[dict] = []
    for i, msg in enumerate(messages):
        if i in indices_to_compact:
            compacted.append(summarize_tool_results(msg))
        else:
            compacted.append(msg)

    return compacted


def run_agent(
    task: str, max_turns: int = 30, token_budget: int = 80_000
) -> object:
    """Run the Claude agent loop for a given task.

    Args:
        task: The task description for Claude.
        max_turns: Maximum number of agent turns.
        token_budget: Token budget before compaction triggers.

    Returns:
        The final API response, or None if max turns reached.
    """
    messages: list[dict] = [{"role": "user", "content": task}]

    for turn in range(max_turns):
        # Check context budget
        est = estimate_tokens(messages)
        tool_count = 0
        print(f"[Turn {turn + 1}] Estimated tokens: {est:,}")

        if est > token_budget:
            print(f"[Turn {turn + 1}] COMPACTING...")
            messages = compact_messages(messages, keep_recent_turns=5)
            est = estimate_tokens(messages)
            print(f"[Turn {turn + 1}] After compaction: {est:,}")

        response = client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=4096,
            system=SYSTEM_PROMPT,
            tools=TOOLS,
            messages=messages,
        )

        # Process response
        if response.stop_reason == "end_turn":
            for block in response.content:
                if hasattr(block, "text"):
                    print(f"\n=== AGENT RESULT ===\n{block.text}")
            return response

        # Handle tool use
        tool_results: list[dict] = []
        for block in response.content:
            if block.type == "tool_use":
                tool_count += 1
                print(f"  [Tool] {block.name}({json.dumps(block.input, default=str)})")
                result = handle_tool_call(block.name, block.input)

                # Format result based on type
                if isinstance(result, dict) and result.get("type") == "image":
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": [
                                {
                                    "type": "image",
                                    "source": {
                                        "type": "base64",
                                        "media_type": "image/png",
                                        "data": result["base64"],
                                    },
                                },
                                {
                                    "type": "text",
                                    "text": (
                                        f"Screenshot captured: "
                                        f"{result['width']}x{result['height']} pixels"
                                    ),
                                },
                            ],
                        }
                    )
                else:
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": json.dumps(result, default=str),
                        }
                    )

        print(f"  Tool calls this turn: {tool_count}")

        messages.append({"role": "assistant", "content": response.content})
        messages.append({"role": "user", "content": tool_results})

    print("Max turns reached")
    return None


if __name__ == "__main__":
    task = (
        sys.argv[1]
        if len(sys.argv) > 1
        else (
            "Explore this trading application. First get the component tree, "
            "then take a screenshot. "
            "Submit a test order for 100 shares of AAPL on the BUY side using ACCT1. "
            "Verify the order appears in the table. "
            "Then check for any contrast/accessibility issues. "
            "Report everything you find."
        )
    )
    run_agent(task)
