# SceneNavFX

A drop-in JavaFX scene navigation library for embedded / framebuffer applications
(Raspberry Pi, kiosk displays, touch-only UIs). Zero dependencies beyond JavaFX itself.

---

## Installation

### Maven
```xml
<dependency>
    <groupId>com.tigerkelly</groupId>
    <artifactId>scenenavfx</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Manual (non-Maven projects)
Build the jar with `mvn package`, then add `scenenavfx-1.0.0.jar` to your project's
classpath / modulepath.

---

## Quickstart

```java
SceneNavigator nav = SceneNavigator.builder()
    .holder(myStackPane)
    .home("main")
    .register("main",        MainController.class,        "application/Main.fxml")
    .register("settings",    SettingsController.class,    "application/Settings.fxml")
    .register("screensaver", ScreensaverController.class, "application/Screensaver.fxml",
              true /* noCache: reload fresh every visit */)
    .navigationListener((from, to) -> System.out.println(from + " -> " + to))
    .errorHandler(e -> showErrorDialog(e.getMessage()))
    .idleTimeout(300_000, "screensaver")   // navigate after 5 min idle
    .build();

nav.bindFocusTo(primaryStage);   // wire @OnPause / @OnResume
nav.goTo("main");
```

---

## Navigation

| Method | Description |
|--------|-------------|
| `goTo("name")` | Push a scene onto the stack |
| `goTo("name", data)` | Push a scene and deliver a data payload via `@OnData` |
| `replace("name")` | Swap the current scene without pushing (back skips it) |
| `goBack()` | Pop one scene |
| `goBack(n)` | Pop n scenes |
| `goHome()` | Pop everything back to the home scene |
| `clearHistory()` | Hard reset to home with no lifecycle hooks |

---

## Lifecycle hooks

Controllers can opt into lifecycle events via the `SceneLifecycle` interface,
annotations, or both. Interface methods fire before annotations.

### Option A — Interface (context-aware)
```java
public class MyController implements SceneLifecycle {

    @Override
    public void onEnter(String fromScene) {
        // fromScene: the scene just left, or null on first entry
        if ("login".equals(fromScene)) lblWelcome.setText("Welcome!");
    }

    @Override
    public void onLeave(String toScene) {
        // toScene: the scene about to become active
        System.out.println("Leaving for: " + toScene);
    }
}
```

### Option B — Annotations
```java
public class MyController {
    @OnFirstLoad public void setupOnce()   { /* runs once after FXML load         */ }
    @OnEnter     public void handleEnter() { /* runs on every entry               */ }
    @OnLeave     public void handleLeave() { /* runs on every leave               */ }
    @OnPause     public void handlePause() { /* app lost OS focus                 */ }
    @OnResume    public void handleResume(){ /* app regained OS focus             */ }
}
```

Annotated methods must be `public void` with no parameters (except `@OnData` — see below).

### Option C — Mix both
Interface methods fire first, then annotated methods.

---

## Passing data between scenes

```java
// Sender
nav.goTo("detail", new MyRecord("Item 1", "Some details"));

// Receiver — fires before onEnter()
@OnData
public void receive(MyRecord item) {
    lblTitle.setText(item.title());
}
```

---

## Blocking navigation (unsaved changes)

```java
public class EditController implements NavigationGuard {

    private boolean dirty = false;

    @Override
    public boolean canLeave(String destinationScene) {
        if (!dirty) return true;
        var alert = new Alert(AlertType.CONFIRMATION, "Discard unsaved changes?");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
```

Returning `false` from `canLeave()` cancels the navigation entirely.

---

## OS focus events (@OnPause / @OnResume)

```java
// In Application.start(), after building the navigator:
nav.bindFocusTo(primaryStage);

// In any controller:
@OnPause  public void pause()  { videoPlayer.pause(); }
@OnResume public void resume() { videoPlayer.play();  }
```

---

## Scene-local store

A key/value store scoped to the current scene, automatically cleared on leave.

```java
nav.storeSet("selectedId", 42);
Object id = nav.storeGet("selectedId");
nav.storeClear();
```

---

## Idle timeout

```java
SceneNavigator nav = SceneNavigator.builder()
    ...
    .idleTimeout(5 * 60 * 1000, "screensaver")   // 5 minutes → screensaver
    .build();

// Cancel the timer on shutdown
@Override
public void stop() { nav.shutdown(); }
```

The timer resets on every navigation call.

---

## Framebuffer / fullscreen (Raspberry Pi)

```java
nav.bindFullscreen(primaryStage);  // undecorated, maximized, always on top
primaryStage.show();
nav.goTo("main");
```

---

## Query API

```java
nav.currentScene()            // logical name of active scene
nav.previousScene()           // scene directly below current on the stack
nav.stackDepth()              // number of scenes on the stack
nav.currentController()       // controller instance of active scene
nav.controllerFor("main")     // controller instance for any registered scene
nav.currentSceneElapsedMs()   // ms since active scene was entered
nav.history()                 // unmodifiable list, oldest→newest
nav.isLoaded("detail")        // true if FXML has been parsed at least once
nav.homeName()                // the home scene name set in the builder
```

---

## Hook firing order per transition

1. `NavigationGuard.canLeave(to)` on the departing controller — may cancel navigation
2. `SceneLifecycle.onLeave(to)` / `@OnLeave` on the departing controller
3. Scene-local store cleared
4. `@OnFirstLoad` on the arriving controller (first visit only)
5. `@OnData` on the arriving controller (if data was passed)
6. `SceneLifecycle.onEnter(from)` / `@OnEnter` on the arriving controller
7. `NavigationListener.onNavigated(from, to)`
8. Idle timer reset

---

## Behaviour notes

| Situation | Behaviour |
|-----------|-----------|
| `goTo()` when scene is already on top | no-op, no lifecycle calls |
| `goBack()` when only one scene on stack | no-op |
| `goBack(n)` when n exceeds stack depth | stops at bottom, no error |
| `goHome()` when home is already active | no-op |
| `clearHistory()` | hard reset, no hooks fired |
| FXML loaded more than once (default) | never — cached after first load |
| `register(..., noCache: true)` | FXML and controller reloaded on every visit |
| `NavigationGuard.canLeave()` returns false | navigation cancelled, no hooks fire |
| `@OnData` with no matching method | silently ignored |

---

## Navigation stack example

```
goTo("main")       stack: [main]
goTo("settings")   stack: [settings, main]
goTo("detail")     stack: [detail, settings, main]
goBack(2)          stack: [main]
replace("login")   stack: [login]          ← main not re-pushed
goHome()           stack: [main]
```

---

## Requirements

- Java 17+
- JavaFX 17+ (provided by your application — not bundled)
