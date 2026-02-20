# Known Issues and Build Notes

## Build Issues Encountered

### 1. Java 8 Compatibility (Critical)

The project must compile and run on Java 8. The following Java 9+ features were initially used and had to be rewritten:

| Feature | Error | Fix |
|---------|-------|-----|
| Switch expressions (`case X ->`) | "switch expressions are not supported in -source 8" | Changed to `case X: ... break;` |
| Pattern matching instanceof (`x instanceof Foo f`) | "pattern matching in instanceof is not supported in -source 8" | Changed to `if (x instanceof Foo) { Foo f = (Foo) x; }` |
| Records (`record CaptureResult(...)`) | "records are not supported in -source 8" | Changed to regular class with constructor, fields, and getters |
| `Map.of()`, `Set.of()` | "cannot find symbol: method of" | Changed to `new LinkedHashMap<>()` + `put()`, `new HashSet<>(Arrays.asList(...))` |
| `InputStream.readAllBytes()` | "cannot find symbol: method readAllBytes()" | Added manual `readFully()` helper with `ByteArrayOutputStream` |
| `URLDecoder.decode(String, Charset)` | "incompatible types: Charset cannot be converted to String" | Changed to `URLDecoder.decode(value, "UTF-8")` |

### 2. Ternary Autoboxing NullPointerException (Critical Runtime Bug)

**Symptom**: `GET /tree?interactable=true` returns `{"error":"java.lang.NullPointerException: null"}` with no useful information in the error message.

**Root cause**: In `ComponentTreeWalker.walkComponent()`, the line:
```java
Integer effectiveParentId = shouldInclude ? id : parentId;
```
where `id` is `int` and `parentId` is `Integer` (null for root components).

Java's ternary operator type promotion rules say: when one branch is `int` and the other is `Integer`, promote both to `int`. This means `parentId` (which is null) gets unboxed to `int`, causing the NPE.

**Fix**: Wrap the primitive side with `Integer.valueOf()`:
```java
Integer effectiveParentId = shouldInclude ? Integer.valueOf(id) : parentId;
```

**Lesson**: Never mix primitive `int` and nullable `Integer` in a ternary expression. This is a well-known Java pitfall but extremely hard to diagnose from the stack trace alone.

### 3. Gradle Wrapper Requirement

Gradle was not installed globally on the build system. The project requires a Gradle Wrapper (`gradlew` / `gradlew.bat`) bundled in the repository. The wrapper downloads Gradle 8.5 automatically on first run.

Always use `./gradlew` (Unix) or `gradlew.bat` (Windows) instead of `gradle`.

## Known Functional Issues

### 4. JMenuBar Duplication in Full Tree

When requesting the full component tree with `interactable=false`, the JMenuBar and its child menus/items appear twice in the output:
- Once via the standard `Container.getComponentCount()` / `getComponent(i)` traversal
- Once via the explicit `JFrame.getJMenuBar()` check

This is because `getJMenuBar()` returns the menu bar, but the menu bar is also accessible through the container's component list. The duplication does not affect the `interactable=true` mode (the default) because the skipped parent containers naturally prevent double-counting in most cases.

**Workaround**: Use `interactable=true` (the default) which naturally avoids this issue. If you need the full tree, be aware that menu components may appear twice.

### 5. Dark Mode Contrast Bug (Intentional)

The demo app intentionally introduces a contrast issue in dark mode: status panel labels (orders count, filled count, etc.) use `Color(60,60,60)` foreground on `Color(50,50,50)` background. This results in a contrast ratio of approximately 1.18:1, well below the WCAG AA requirement of 4.5:1.

This is by design — it validates that the contrast checker correctly identifies accessibility issues.

### 6. Window Focus on Screenshot

`ScreenshotCapture` uses `java.awt.Robot.createScreenCapture()` which captures pixels from the screen. If another window overlaps the Swing application, those pixels will appear in the screenshot. This is a fundamental limitation of the Robot-based approach.

### 7. Single-Client Assumption

The HTTP server does not serialize requests. Multiple concurrent clients could cause issues if they both trigger actions that modify the same components. The 100ms delay after each action is a best-effort approach for single-client scenarios.

## Python Orchestrator Notes

### 8. Context Window Exhaustion

Without the compaction logic, the orchestrator will fail after approximately 10-15 tool calls as the message history exceeds Claude's context window. The compaction threshold is set at 80K tokens. If you see degraded performance or errors on long tasks, check the token estimation logs.

### 9. Screenshot Token Cost

Each screenshot in the conversation history costs approximately 1,600 tokens. The orchestrator keeps at most 2 screenshots in history. Taking frequent screenshots is the fastest way to exhaust the context budget.
