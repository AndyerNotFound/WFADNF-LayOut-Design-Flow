# LayOut Flow

LayOut Flow is a native Android tool for designing cross-platform interaction flows and front-end screen structure. It presents a free-form canvas of Material Design 3 component previews, connects components with typed interaction edges, and exports the result as a full diagram or as structured XML.

The app is written in Kotlin with Jetpack Compose and Material 3. It targets Android 16 (API 36), supports Android 8.0 and later (API 26+), and is compiled exclusively by GitHub Actions.

## Capabilities

- **Drag to build.** Long-press a component in the bottom shelf and drop it on the canvas. Tapping a tile or catalog entry inserts the component at the viewport centre.
- **Drag to remove.** While a node is being dragged, a delete target appears at the top of the canvas. Release the node over it to remove the node and every connection attached to it.
- **Connect components.** Drag from the round port on either side of a node to another node. The port on the right creates a forward edge, the port on the left creates a reverse edge.
- **Describe interactions on the edge.** Every edge stores a trigger, an action, a view transition, a duration, a delay, an easing curve, an optional condition, a custom label, and free-form key/value properties.
- **Precision editing.** The inspector accepts numeric values for position, size, corner radius, elevation and opacity. Text, accent colour and arbitrary `key=value` properties are editable as well.
- **Flexible canvas.** One-finger pan, pinch zoom, zoom buttons, fit-to-content, grid snapping and an undo/redo history are available.
- **Autosave and projects.** The current project is saved to app storage automatically. Projects can be exported as JSON and imported again through the system file picker.
- **Export.** Produce a PNG or SVG of the complete flow, an XML interaction-flow document, or a JSON project file.

## Component coverage

The built-in catalog contains 110 templates:

| Platform | Examples |
| --- | --- |
| Android / Material 3 | Activity or Compose screen, TopAppBar, NavigationBar, NavigationRail, filled/tonal/outlined/text buttons, FAB, text fields, search, switch, checkbox, radio, slider, chip, card, list, dialog, bottom sheet, snackbar, tabs, menu, progress, date/time picker |
| Web | Page, header, navigation, button, link, input, textarea, select, checkbox, radio, range, card, modal, dropdown, toast, image, video, table, form, tabs, sidebar, footer, canvas |
| Flutter | Scaffold, AppBar, FloatingActionButton, ElevatedButton, TextField, Switch, Checkbox, Card, ListView, BottomNavigationBar, Drawer, AlertDialog, SnackBar, TabBar, Image, PageView |
| iOS | View, NavigationStack, TabView, Button, TextField, Toggle, Picker, List, GroupBox, Sheet, Alert, Image, TabBar |
| Desktop | Window, menu bar, toolbar, button, text field, list, dialog, tray menu, drawing canvas |
| Logic | Start, condition, state, API request, timer, note, group, end |
| Custom | A generic node for any component, platform or interaction not covered by a template |

All templates use Material Design 3 styling by default. A node can switch its platform and accent colour without losing its structure.

The interaction catalog covers 23 triggers (tap, long press, double tap, four swipe directions, drag, pinch, scroll, focus, hover, key, back, load, timer, value change, notification, voice, shake, deep link, custom), 16 actions, 14 transitions (fade, fade through, four slides, shared axis X/Y/Z, container transform, scale, reveal, none, custom) and 6 easing curves.

## Export formats

### PNG and SVG

Both render the full flow with automatic bounds, a title block, node cards, connection curves, arrowheads and interaction labels. PNG is rendered at 2x with a size cap; SVG keeps the diagram in vector form and uses the same coordinate system.

### Interaction-flow XML

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout-flow version="1.0" generator="LayOut Flow" exported-at="...">
  <meta name="Login flow" nodes="2" connections="1" grid="8" snap="true" />
  <nodes>
    <node id="n1" spec="android.button" name="Sign in" platform="android" kind="button"
          x="120" y="320" width="140" height="48" text="Sign in" accent="#6750A4"
          corner="24" elevation="0" opacity="1">
      <property key="analytics" value="sign_in_click" />
    </node>
    <node id="n2" spec="android.screen" name="Home" platform="android" kind="screen"
          x="430" y="80" width="280" height="500" text="Today" accent=""
          corner="24" elevation="2" opacity="1" />
  </nodes>
  <connections>
    <connection id="e1" from="n1" to="n2" trigger="click" action="navigate"
                transition="sharedAxisX" duration="300" delay="0" easing="emphasized"
                condition="" label="点击 · 跳转 · 共享轴X · 300ms"
                from-name="Sign in" to-name="Home" />
  </connections>
</layout-flow>
```

The XML is a design description, not executable platform code. It is intended for documentation, review, hand-off and downstream code generation.

## Using the editor

1. Long-press a component in the bottom shelf and drag it onto the canvas. The full catalog is available through **全部元件** and supports search and platform filters.
2. Drag a node to move it. Release near the top delete target to remove it. Selected nodes expose a resize handle at the bottom-right corner.
3. Drag from a node port to another node to create a connection. Tap the edge or its label to open the edge inspector.
4. Use the inspector to set the trigger, action, transition, timing, condition, label and properties. Numeric fields accept decimals.
5. Pinch or use the zoom controls to navigate. **适** fits all nodes into the viewport.
6. Export through the top-right menu. The app also autosaves locally after each edit.

## Build and release

The repository contains a single `app` module and a GitHub Actions workflow at `.github/workflows/android.yml`.

- A push to `main` runs unit tests, assembles a signed release APK and uploads it as a workflow artifact.
- A tag such as `v0.1.0` creates a GitHub release with the APK and `SHA256SUMS.txt`.
- A manual workflow dispatch with a version input creates the same release without requiring a local tag.
- When `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` are configured, the release is signed with that keystore. Otherwise the debug key is used and the workflow emits a warning.

No local Gradle or Android SDK installation is required to produce a build.

## Project layout

```text
app/src/main/java/com/andyer/layoutflow/
├── MainActivity.kt
├── data/ProjectStore.kt              autosave and project parsing
├── model/
│   ├── Catalog.kt                    components, triggers, actions, transitions
│   ├── Demo.kt                       first-run example flow
│   ├── FlowExporter.kt               XML, SVG and JSON export
│   ├── Geometry.kt                   edge curves and arrowheads
│   └── Model.kt                      project, node and edge schema
└── ui/
    ├── CanvasView.kt                 canvas, connections, hit testing
    ├── DiagramRenderer.kt            PNG renderer
    ├── EditorScreen.kt               scaffold, menus, export and file pickers
    ├── EditorViewModel.kt            state, undo/redo, autosave
    ├── Inspector.kt                  numeric and interaction editors
    ├── NodeCard.kt                   Material 3 previews and gestures
    ├── Palette.kt                    component shelf and full catalog
    └── Theme.kt                      Material 3 colour schemes
```

## Limitations

- The XML documents a flow; it is not an Android layout resource and does not generate runnable code.
- Component previews are representative Material 3 renderings, not pixel-identical platform widgets.
- Projects are stored as a single autosave document. Multiple named projects can be managed by exporting and importing JSON files.

## License

MIT. See `LICENSE`.
