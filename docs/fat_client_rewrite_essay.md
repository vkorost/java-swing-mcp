# The Fat Client Problem: Why Rewriting Legacy Desktop Applications Breaks Every Team That Tries

## The server is the easy part

When organizations decide to rewrite a legacy system, the first instinct is to panic about the server side. Decades of business logic, stored procedures, message queues, integration points. It looks terrifying.

It isn't.

Server-side code, however old and tangled, is fundamentally tractable. Functions take inputs and produce outputs. Data flows through defined paths. You can trace execution, read logs, instrument endpoints, and with today's AI-assisted code analysis, you can reverse-engineer the behavior of even the most convoluted backend in a fraction of the time it would have taken five years ago. The server side is a solved problem. Not easy, but solved.

The hard part is the fat client.

## What makes fat clients different

A fat client is a desktop application that runs as a native process on the user's machine. Java Swing, WPF, Qt, GTK, Win32/MFC, Delphi. These applications share a set of characteristics that make them fundamentally harder to understand, document, and rewrite than server-side systems.

**State lives in memory, not in a database.** A server application's state is visible. It's in rows, columns, caches, queues. You can query it. A fat client's state is scattered across thousands of objects in a running JVM or CLR process. Which tab is active. Which tree nodes are expanded. Which table columns are sorted and in what direction. Which fields are enabled based on the combination of three other field values. What the user selected six clicks ago that still affects what they see now. None of this is logged. None of this is queryable. It exists only at runtime.

**Behavior is event-driven and non-linear.** Server-side request handling is broadly sequential: request comes in, processing happens, response goes out. Fat client behavior is a web of event handlers responding to mouse clicks, key presses, focus changes, timer events, resize events, data arrival events, and custom events fired by other components. The interaction between these handlers is where the real application behavior lives, and it is nearly impossible to reconstruct from reading code alone.

**UI behavior is emergent.** Nobody designed some of the most important behaviors in any long-running fat client. They emerged. A developer added a listener to update a status bar. Another developer added a listener on the same event to refresh a table. A third developer added a timer to poll for data changes. Ten years later, the interaction between these three independent decisions creates a behavior that users depend on, that appears nowhere in any spec, and that nobody currently on the team fully understands. It just works. Until you try to rewrite it and it doesn't.

**Users can't tell you what they do.** Ask a trader to describe their workflow, and they will give you a high-level narrative that omits 80% of what they actually do. They won't mention that they always check a specific column before clicking submit. They won't mention the right-click context menu they use to copy values. They won't mention that they paste from Excel in a specific format. They won't mention the keyboard shortcut they discovered by accident seven years ago. These micro-behaviors are invisible to the users themselves, and they are the first things that break in a rewrite.

**The application is its own documentation.** In most legacy fat client codebases, the running application is the only reliable source of truth. The wiki is stale. The requirements documents describe a version from four years ago. The Jira tickets describe what was requested, not what was built. The code tells you what happens mechanically but not why. Only the running application, observed in real time, tells you what it actually does and how users actually use it.

## Why rewrites fail

Legacy desktop application rewrites have a dismal track record. The pattern is predictable.

**Phase 1: Optimism.** The team inventories the major features, estimates the effort, and begins building. Modern frameworks will make everything faster. The new version will be cleaner, more maintainable, more extensible.

**Phase 2: The discovery gap.** As development progresses, the team discovers behavior after behavior that was not in the spec because nobody knew it existed. Each discovery requires investigation, discussion, and a decision: replicate it or not? The timeline extends.

**Phase 3: User revolt.** The first pilot users encounter the new version and immediately identify things that are missing or different. Features they use daily. Workflows they depend on. Small behaviors that "just worked" in the old version. The feedback is overwhelmingly negative, not because the new version is bad, but because it is different in ways that the users experience as broken.

**Phase 4: The long tail.** The team enters an extended phase of chasing parity with the old application. Every week surfaces new gaps. The old application continues to evolve because the business can't wait. The new version is perpetually 90% complete. Morale erodes. The project either drags on for years or gets canceled.

The root cause is always the same: the team did not fully understand what the old application did before they started building the new one.

## The observation problem

Understanding a running fat client application is fundamentally an observation problem. You need to see what the application does, how users interact with it, what state it holds, and how that state changes in response to user actions and external events.

For web applications, this problem is solved. Chrome DevTools Protocol gives you programmatic access to the DOM, the network layer, the JavaScript runtime, and the rendering engine. You can inspect any element, read any property, execute any action, record any interaction, and do it all programmatically from outside the browser. This is why AI-assisted web development works so well: the agent can see and interact with the application through structured APIs.

For desktop applications, the tooling landscape is different. Accessibility APIs exist: Java Access Bridge, Microsoft UI Automation, AT-SPI on Linux. These APIs can walk component trees and read some state. But they were designed for screen readers and assistive technology, not for comprehensive application understanding. The serialization formats are optimized for real-time streaming to assistive devices, not for analysis. The transport layers require framework-specific bindings, not HTTP calls. The level of detail varies wildly by component type and implementation quality.

More importantly, none of these tools were designed for AI agent consumption. An LLM cannot consume a COM-based UI Automation tree or a Java Accessibility context directly. There is no standard way to serialize a desktop application's complete state into a format that an AI agent can reason about.

This is the gap.

## Observing the application from inside

The approach that closes this gap is conceptually simple: embed a lightweight HTTP server inside the running application so it can describe itself.

This is what the java-swing-mcp project does for Java Swing applications. One line of code adds an HTTP server to any Swing application. That server exposes the entire live component tree as structured JSON. Every component, its type, its state, its properties, its position, its parent, its children, all serialized as a flat array with parent ID references, optimized for LLM context windows.

The key insight is that the application has access to everything. It is running inside the same JVM process. It can walk the AWT component hierarchy on the Event Dispatch Thread. It can read the text in every field, the selection state of every table, the expanded paths of every tree, the items in every combo box. It can extract table data with column headers. It can identify which components are enabled, visible, focused. It does not need external instrumentation, bytecode manipulation, or JVM agent flags. It simply looks at its own component graph and reports what it sees.

This transforms a black box into a transparent system. Any HTTP client, whether an AI agent, a test harness, a monitoring tool, or a human with curl, can now ask the application: what are you showing? what state are you in? what can the user do right now?

## What this enables for rewrite teams

### Requirements discovery through observation

Instead of relying on stale documentation and incomplete user interviews, the rewrite team can observe the actual application behavior. The component tree reveals every UI element: fields, buttons, menus, tables, trees, tabs, dialogs, context menus. The state extraction shows how these elements are configured: what values are loaded, what options are available, what validations are in place.

An AI agent can methodically explore the application, screen by screen, workflow by workflow, and produce structured documentation of what exists. Not what the wiki says exists. Not what the Jira tickets describe. What actually exists, right now, in the running application.

This is not a replacement for user interviews and business analysis. It is a foundation for them. When a business analyst sits down with users to discuss workflows, they can start from a complete, accurate inventory of the current application's capabilities rather than a partial, stale one.

### Workflow recording and analysis

The user action recorder captures every mouse click, key press, combo selection, tree navigation, and menu action as timestamped markdown files. Deploy the instrumented application to a group of pilot users, let them work for a week, and you have a detailed record of actual user behavior.

This data answers questions that user interviews cannot. Which features are used daily? Which are used weekly? Which are never used? What is the typical sequence of actions for entering an order? How do power users differ from casual users? Where do users hesitate, backtrack, or make errors? What keyboard shortcuts do they use?

For a rewrite team, this is invaluable. It tells you what to build first, what to replicate exactly, and what to deprioritize or drop.

### QA without manual test scripts

Manual QA of a legacy fat client is painful. Testers follow scripts: click here, type this, verify that. The scripts are slow to write, slow to execute, and slow to update when the application changes.

With the application exposing its state over HTTP, test verification becomes programmatic. An AI agent or test script can submit an order through the UI using semantic actions (click the "BUY" button, type "AAPL" in the symbol field, click "Submit") and then verify the result by reading the blotter table's actual data. No screenshot comparison. No pixel coordinates. Direct state verification through the same JSON API.

This means QA teams can describe test cases in natural language ("submit a market order for 100 shares of AAPL and verify it appears in the blotter with status 'Pending'"), and an AI agent can execute them against the live application and report results with exact component-level detail about what matched and what didn't.

For regression testing after code changes, the agent can compare structured component trees and state snapshots before and after. It knows exactly which component changed, what property changed, and by how much. No fuzzy image diffs. Precise, attributable differences.

### Accessibility compliance as a rewrite deliverable

Many legacy fat clients were built before accessibility standards were widely adopted. A rewrite is the natural opportunity to address this debt. But first you need to know the scope of the problem.

The WCAG 2.1 contrast audit walks every text-bearing component in the running application, extracts foreground and background colors, computes the luminance contrast ratio, and flags anything below AA or AAA thresholds. This produces an actionable report of every accessibility violation in the current application, organized by component, with exact colors and ratios.

This gives the rewrite team a concrete remediation list. It also provides a baseline: here is the accessibility state of the old application. The new application must do at least this well, and preferably better.

### Business analyst enablement

Business analysts in organizations with large legacy fat clients face a particular challenge. They need to document current behavior to specify the new behavior, but the current behavior is locked inside an application they can only interact with as end users. They can see what the application shows, but they cannot query it systematically.

With the HTTP API exposed, a BA can use simple tools to explore the application programmatically. What are all the columns in this table? What values appear in this dropdown? What menu items are available? What happens to the ticket fields when I change the order type? These questions, previously answered by manually clicking through the application or by asking a developer to read the source code, become HTTP requests that return structured data.

An AI assistant connected to the API can serve as a persistent exploration partner for BAs. "Walk me through every field on the order entry screen and tell me which ones are required." "Show me all the menu items and their keyboard shortcuts." "What changes in the UI when I switch from Equities to Futures?" The assistant reads the live application state and provides accurate, current answers.

## The hundred-thousand-line codebase

The value of this approach scales with the size and age of the application.

A 10,000-line Swing application is manageable. A developer can read it, understand it, and rewrite it. The code is the documentation.

A 100,000-line Swing application is a different beast. Multiple developers have contributed over years. There are dead code paths that nobody dares remove. There are utility classes copied from Stack Overflow in 2008. There are workarounds for bugs in Java 6 that are still there in Java 17. There are event handlers that reference global state through static fields. There are custom renderers, custom editors, custom layout managers, custom look-and-feel overrides. The code is not the documentation. The code is an archaeological site.

At this scale, reading the source code to understand behavior is like reading the blueprint of a building to understand what happens inside it. The blueprint tells you where the walls are. It does not tell you that room 304 is always locked on Tuesdays, that the elevator skips the third floor after 6 PM, or that the thermostat in the south wing has been set to manual since 2019. These are runtime behaviors that emerge from the interaction of structural decisions and operational practices over time.

The embedded HTTP server lets you observe the building while people are in it. You see what rooms are used, how people move through them, what they carry, where they stop. You see the application as it is, not as it was designed to be.

## The portable pattern

The java-swing-mcp implementation targets Java Swing, but the pattern is technology-agnostic. Any desktop application framework where you have source code access can support this approach:

**Java Swing.** Walk the AWT Component hierarchy. Serialize with Gson. Serve with the JDK's built-in HttpServer. This is what java-swing-mcp implements.

**WPF (.NET).** Walk the VisualTree and LogicalTree. Serialize with System.Text.Json. Serve with HttpListener or Kestrel embedded. WPF's dependency property system means you can extract data bindings and styles in addition to visual state.

**Qt (C++/Python).** Walk the QObject tree. Serialize with QJsonDocument or nlohmann/json. Serve with QHttpServer (Qt 6.4+) or embedded microhttpd. Qt's meta-object system provides rich property information.

**GTK.** Walk the widget tree. Serialize with json-glib. Serve with libsoup. GTK's GObject property system supports introspection.

**Win32/MFC.** Walk the HWND tree with EnumChildWindows. Serialize with a JSON library. Serve with WinHTTP or an embedded HTTP library. Less rich component metadata, but window text, class names, and styles are available.

The implementation details differ. The principle is the same: the application opens a port, looks at its own UI graph, and describes what it sees. Any tool that speaks HTTP can then ask questions and take actions.

## What this is not

This approach does not replace a proper rewrite methodology. It does not generate the new application for you. It does not automatically translate Swing code into React components or WPF XAML into Flutter widgets.

It also does not solve the backend understanding problem, though as discussed, that problem is substantially easier and is well-served by existing tools.

What it does is close the observation gap. It gives rewrite teams, QA engineers, business analysts, and AI agents structured, programmatic access to the one source of truth that matters most and is hardest to access: the running application itself.

The teams that fail at legacy rewrites fail because they build against incomplete understanding. They don't know what they don't know, and they discover the gaps in production when users report that something is missing or different.

Structured observation of the live application does not guarantee a successful rewrite. But it removes the most common reason for failure: building blind.

## Getting started

The java-swing-mcp project is open source under the MIT license.

The library is a single dependency added to your Swing application's build. One line of code starts the server. Named components (via `setName()`) become addressable by name in every API call. Components without names are still accessible by auto-assigned numeric IDs.

The project includes a demo trading application, a Python orchestrator that connects Claude to the HTTP API, and complete API documentation for every endpoint.

Repository: https://github.com/vkorost/java-swing-mcp

Demo video showing a Claude agent navigating a Swing trading application through the server: https://youtu.be/09Cq8mLPSfw
