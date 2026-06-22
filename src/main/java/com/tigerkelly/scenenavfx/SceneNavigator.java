package com.tigerkelly.scenenavfx;

import com.tigerkelly.scenenavfx.annotations.OnPause;
import com.tigerkelly.scenenavfx.annotations.OnResume;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A drop-in JavaFX scene navigation library for embedded / framebuffer applications.
 *
 * <h2>Quickstart</h2>
 * <pre>{@code
 * SceneNavigator nav = SceneNavigator.builder()
 *     .holder(myStackPane)
 *     .home("main")
 *     .register("main",     MainController.class,     "application/Main.fxml")
 *     .register("settings", SettingsController.class, "application/Settings.fxml")
 *     .register("detail",   DetailController.class,   "application/Detail.fxml")
 *     .navigationListener((from, to) -> System.out.println(from + " -> " + to))
 *     .errorHandler(e -> showErrorDialog(e.getMessage()))
 *     .idleTimeout(300_000, "screensaver")   // 5 min idle -> screensaver scene
 *     .build();
 *
 * nav.goTo("main");
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <ul>
 *   <li>{@link #goTo(String)} — push a scene</li>
 *   <li>{@link #goTo(String, Object)} — push a scene with a data payload</li>
 *   <li>{@link #goBack()} — pop one scene</li>
 *   <li>{@link #goBack(int)} — pop N scenes</li>
 *   <li>{@link #goHome()} — pop all scenes back to home</li>
 *   <li>{@link #replace(String)} — swap current scene without stacking</li>
 *   <li>{@link #clearHistory()} — reset stack to home without lifecycle hooks</li>
 * </ul>
 *
 * <h2>Controller lifecycle</h2>
 * Controllers may implement {@link SceneLifecycle} and/or use annotations from
 * {@link com.tigerkelly.scenenavfx.annotations}. Hooks fire in this order per transition:
 * <ol>
 *   <li>{@link NavigationGuard#canLeave(String)} on the departing controller (if implemented)</li>
 *   <li>{@link SceneLifecycle#onLeave(String)} / {@code @OnLeave} on the departing controller</li>
 *   <li>{@code @OnData} on the arriving controller (if data was passed)</li>
 *   <li>{@link SceneLifecycle#onEnter(String)} / {@code @OnEnter} on the arriving controller</li>
 *   <li>{@link NavigationListener#onNavigated(String, String)}</li>
 * </ol>
 *
 * <h2>Scene-local store</h2>
 * Each scene has a key/value store accessible via {@link #storeSet(String, Object)},
 * {@link #storeGet(String)}, and {@link #storeClear()}. The store is cleared automatically
 * when the scene is left.
 *
 * <h2>Thread safety</h2>
 * All public methods must be called on the JavaFX Application Thread.
 */
public final class SceneNavigator {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final StackPane holder;
    private final String homeName;
    private final Map<String, SceneRecord> registry;
    private final NavigationListener navigationListener;
    private final java.util.function.Consumer<SceneNavigatorException> errorHandler;
    private final long idleTimeoutMs;
    private final String idleScene;

    /** Navigation history. Top of deque is the current scene. */
    private final Deque<String> stack = new ArrayDeque<>();

    private Timer idleTimer;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private SceneNavigator(Builder builder) {
        this.holder             = builder.holder;
        this.homeName           = builder.homeName;
        this.registry           = Collections.unmodifiableMap(builder.registry);
        this.navigationListener = builder.navigationListener;
        this.errorHandler       = builder.errorHandler;
        this.idleTimeoutMs      = builder.idleTimeoutMs;
        this.idleScene          = builder.idleScene;

        if (this.holder == null)
            throw new SceneNavigatorException("SceneNavigator requires a StackPane holder.");
        if (this.homeName == null || !this.registry.containsKey(this.homeName))
            throw new SceneNavigatorException(
                "Home scene '" + this.homeName + "' has not been registered.");
        if (this.idleTimeoutMs > 0 && (this.idleScene == null
                || !this.registry.containsKey(this.idleScene)))
            throw new SceneNavigatorException(
                "Idle scene '" + this.idleScene + "' has not been registered.");
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @return a new {@code Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Navigation API
    // -------------------------------------------------------------------------

    /**
     * Pushes the named scene onto the stack and makes it active.
     * If the named scene is already on top of the stack this call is a no-op.
     *
     * @param name the logical name the scene was registered under
     * @throws SceneNavigatorException if the name is unknown or FXML fails to load
     */
    public void goTo(String name) {
        goToInternal(name, null);
    }

    /**
     * Pushes the named scene onto the stack, passing a data payload to the
     * destination controller via its {@code @OnData}-annotated method.
     * The data method fires before {@code onEnter()} so the controller has
     * the payload available when the enter hook runs.
     *
     * @param name the logical name the scene was registered under
     * @param data the payload to deliver; ignored if {@code null}
     * @throws SceneNavigatorException if the name is unknown or FXML fails to load
     */
    public void goTo(String name, Object data) {
        goToInternal(name, data);
    }

    /**
     * Replaces the current scene without pushing onto the stack.
     * Going back from the new scene will skip the replaced scene entirely.
     * If the named scene is already on top this call is a no-op.
     *
     * @param name the scene to replace the current one with
     * @throws SceneNavigatorException if the name is unknown or FXML fails to load
     */
    public void replace(String name) {
        if (!registry.containsKey(name))
            throw new SceneNavigatorException("Scene not registered: '" + name + "'");

        String current = stack.isEmpty() ? null : stack.peek();
        if (name.equals(current)) return;

        if (!guardCanLeave(current, name)) return;

        if (current != null) {
            fireLeaveAndClearStore(current, name);
            stack.pop();
        }

        SceneRecord record = ensureLoaded(name);
        stack.push(name);
        record.lastEnterTime = Instant.now().toEpochMilli();
        show(record.node);
        LifecycleDispatcher.fireEnter(record.controller, current);
        fireListener(current, name);
        resetIdleTimer();
    }

    /**
     * Pops the current scene and returns to the previous one.
     * If the stack has only one entry this call is a no-op.
     *
     * @throws SceneNavigatorException if the stack is empty
     */
    public void goBack() {
        goBack(1);
    }

    /**
     * Pops {@code n} scenes off the stack.
     * Stops early (without error) if the bottom of the stack is reached before
     * {@code n} pops complete.
     *
     * @param n number of scenes to pop; must be &gt; 0
     * @throws SceneNavigatorException if the stack is empty or {@code n} &lt; 1
     */
    public void goBack(int n) {
        if (n < 1)
            throw new SceneNavigatorException("goBack(n): n must be >= 1, got " + n);
        if (stack.isEmpty())
            throw new SceneNavigatorException("Cannot goBack(): navigation stack is empty.");
        if (stack.size() == 1)
            return;

        String leavingScene = stack.peek();
        String destination  = peekBeneath(n);

        if (!guardCanLeave(leavingScene, destination)) return;

        fireLeaveAndClearStore(leavingScene, destination);

        int popped = 0;
        while (popped < n && stack.size() > 1) {
            stack.pop();
            popped++;
        }

        String previous = stack.peek();
        SceneRecord record = registry.get(previous);
        record.lastEnterTime = Instant.now().toEpochMilli();
        show(record.node);
        LifecycleDispatcher.fireEnter(record.controller, leavingScene);
        fireListener(leavingScene, previous);
        resetIdleTimer();
    }

    /**
     * Pops all scenes off the stack until the home scene is on top.
     * Fires {@code onLeave} only on the current (topmost) scene.
     * If the home scene is already active this call is a no-op.
     */
    public void goHome() {
        if (stack.isEmpty() || stack.peek().equals(homeName)) return;

        String current = stack.peek();
        if (!guardCanLeave(current, homeName)) return;

        fireLeaveAndClearStore(current, homeName);

        while (!stack.isEmpty() && !stack.peek().equals(homeName)) {
            stack.pop();
        }

        if (stack.isEmpty()) {
            ensureLoaded(homeName);
            stack.push(homeName);
        }

        SceneRecord home = registry.get(homeName);
        home.lastEnterTime = Instant.now().toEpochMilli();
        show(home.node);
        LifecycleDispatcher.fireEnter(home.controller, current);
        fireListener(current, homeName);
        resetIdleTimer();
    }

    /**
     * Resets the navigation stack to just the home scene without firing any
     * lifecycle hooks on the scenes being cleared. Use when you need a hard
     * reset (e.g. after a session timeout) and don't want controllers involved.
     */
    public void clearHistory() {
        stack.clear();
        SceneRecord home = ensureLoaded(homeName);
        stack.push(homeName);
        home.lastEnterTime = Instant.now().toEpochMilli();
        show(home.node);
        resetIdleTimer();
    }

    // -------------------------------------------------------------------------
    // Scene-local store
    // -------------------------------------------------------------------------

    /**
     * Stores a value in the current scene's local key/value store.
     * The store is cleared automatically when the scene is left.
     *
     * @param key   the key
     * @param value the value; {@code null} removes the key
     * @throws SceneNavigatorException if no scene is currently active
     */
    public void storeSet(String key, Object value) {
        SceneRecord r = currentRecord();
        if (value == null) r.store.remove(key);
        else r.store.put(key, value);
    }

    /**
     * Retrieves a value from the current scene's local store.
     *
     * @param key the key
     * @return the value, or {@code null} if absent
     * @throws SceneNavigatorException if no scene is currently active
     */
    public Object storeGet(String key) {
        return currentRecord().store.get(key);
    }

    /**
     * Clears all entries from the current scene's local store.
     *
     * @throws SceneNavigatorException if no scene is currently active
     */
    public void storeClear() {
        currentRecord().store.clear();
    }

    // -------------------------------------------------------------------------
    // OS focus (pause / resume)
    // -------------------------------------------------------------------------

    /**
     * Binds OS focus events on the given {@link Stage} to {@code @OnPause} and
     * {@code @OnResume} annotations on the currently active controller.
     *
     * <p>Call this once after building the navigator and showing the primary stage.
     * Only one stage can be bound at a time; calling this again replaces the previous binding.
     *
     * @param stage the primary application stage
     */
    public void bindFocusTo(Stage stage) {
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            Object ctrl = currentController();
            if (ctrl == null) return;
            if (!isFocused) LifecycleDispatcher.firePause(ctrl);
            else            LifecycleDispatcher.fireResume(ctrl);
        });
    }

    // -------------------------------------------------------------------------
    // Fullscreen / framebuffer helper
    // -------------------------------------------------------------------------

    /**
     * Configures the given {@link Stage} for fullscreen framebuffer deployment
     * (no title bar, no decorations, always on top) and wires it to the navigator's
     * {@link StackPane} holder via a new {@link javafx.scene.Scene}.
     *
     * <p>Typical use for Raspberry Pi / monocle framebuffer targets:
     * <pre>{@code
     * nav.bindFullscreen(primaryStage);
     * primaryStage.show();
     * nav.goTo("main");
     * }</pre>
     *
     * @param stage the primary application stage to configure
     */
    public void bindFullscreen(Stage stage) {
        stage.setScene(new javafx.scene.Scene(holder));
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setAlwaysOnTop(true);
        javafx.stage.StageStyle style = javafx.stage.StageStyle.UNDECORATED;
        stage.initStyle(style);
    }

    // -------------------------------------------------------------------------
    // Query API
    // -------------------------------------------------------------------------

    /**
     * Returns the logical name of the currently active scene,
     * or {@code null} if no scene has been navigated to yet.
     *
     * @return the current scene name, or {@code null}
     */
    public String currentScene() {
        return stack.isEmpty() ? null : stack.peek();
    }

    /**
     * Returns the logical name of the scene directly below the current one on
     * the stack, or {@code null} if the stack has fewer than two entries.
     *
     * @return the previous scene name, or {@code null}
     */
    public String previousScene() {
        if (stack.size() < 2) return null;
        String top = stack.pop();
        String prev = stack.peek();
        stack.push(top);
        return prev;
    }

    /**
     * Returns the number of scenes currently on the navigation stack.
     *
     * @return stack depth; 0 if no navigation has occurred
     */
    public int stackDepth() {
        return stack.size();
    }

    /**
     * Returns the controller instance for the currently active scene,
     * or {@code null} if no scene is active or it has not been loaded yet.
     *
     * @return the current scene's controller, or {@code null}
     */
    public Object currentController() {
        if (stack.isEmpty()) return null;
        SceneRecord r = registry.get(stack.peek());
        return (r != null) ? r.controller : null;
    }

    /**
     * Returns the controller instance for the named scene,
     * or {@code null} if the scene has not been loaded yet.
     *
     * @param name the registered scene name
     * @return the scene's controller instance, or {@code null} if not yet loaded
     * @throws SceneNavigatorException if the name is not registered
     */
    public Object controllerFor(String name) {
        SceneRecord r = registry.get(name);
        if (r == null)
            throw new SceneNavigatorException("Scene not registered: '" + name + "'");
        return r.controller;
    }

    /**
     * Returns milliseconds elapsed since the current scene was last entered,
     * or {@code -1} if no scene is active.
     *
     * @return elapsed milliseconds, or {@code -1}
     */
    public long currentSceneElapsedMs() {
        if (stack.isEmpty()) return -1;
        SceneRecord r = registry.get(stack.peek());
        if (r == null || r.lastEnterTime < 0) return -1;
        return Instant.now().toEpochMilli() - r.lastEnterTime;
    }

    /**
     * Returns an unmodifiable snapshot of the navigation history, oldest first.
     * The last element is the current scene.
     *
     * @return unmodifiable list of scene names, oldest to newest
     */
    public List<String> history() {
        List<String> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns {@code true} if the named scene has been loaded (FXML parsed,
     * controller instantiated) at least once.
     *
     * @param name the registered scene name
     * @return {@code true} if the scene has been loaded at least once
     */
    public boolean isLoaded(String name) {
        SceneRecord r = registry.get(name);
        return r != null && r.loaded;
    }

    /**
     * Returns the logical home scene name this navigator was built with.
     *
     * @return the home scene name
     */
    public String homeName() {
        return homeName;
    }

    /**
     * Cancels the idle timer and releases any timer resources.
     * Call this from {@code Application.stop()} if an idle timeout was configured.
     */
    public void shutdown() {
        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void goToInternal(String name, Object data) {
        if (!registry.containsKey(name))
            throw new SceneNavigatorException("Scene not registered: '" + name + "'");

        String current = stack.isEmpty() ? null : stack.peek();
        if (name.equals(current)) return;

        if (!guardCanLeave(current, name)) return;

        if (current != null) fireLeaveAndClearStore(current, name);

        SceneRecord record = ensureLoaded(name);
        stack.push(name);
        record.lastEnterTime = Instant.now().toEpochMilli();
        show(record.node);

        if (data != null) LifecycleDispatcher.fireData(record.controller, data);
        LifecycleDispatcher.fireEnter(record.controller, current);
        fireListener(current, name);
        resetIdleTimer();
    }

    private boolean guardCanLeave(String sceneName, String destination) {
        if (sceneName == null) return true;
        SceneRecord r = registry.get(sceneName);
        if (r == null || !(r.controller instanceof NavigationGuard)) return true;
        return ((NavigationGuard) r.controller).canLeave(destination);
    }

    private void fireLeaveAndClearStore(String sceneName, String destination) {
        SceneRecord r = registry.get(sceneName);
        if (r == null) return;
        LifecycleDispatcher.fireLeave(r.controller, destination);
        r.store.clear();
        if (r.noCache) {
            r.loaded     = false;
            r.node       = null;
            r.controller = null;
            r.loader     = null;
        }
    }

    private SceneRecord ensureLoaded(String name) {
        SceneRecord record = registry.get(name);

        if (!record.loaded) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    record.controllerClass.getResource(record.fxmlPath));
                record.loader     = loader;
                record.node       = (Node) loader.load();
                record.controller = loader.getController();
                record.loaded     = true;

                LifecycleDispatcher.fireFirstLoad(record.controller);

            } catch (IOException e) {
                SceneNavigatorException ex = new SceneNavigatorException(
                    "Failed to load FXML '" + record.fxmlPath
                    + "' for scene '" + name + "'.", e);
                if (errorHandler != null) { errorHandler.accept(ex); return record; }
                throw ex;
            }
        }

        return record;
    }

    private void show(Node node) {
        if (node != null) holder.getChildren().setAll(node);
    }

    private void fireListener(String from, String to) {
        if (navigationListener != null) navigationListener.onNavigated(from, to);
    }

    private SceneRecord currentRecord() {
        if (stack.isEmpty())
            throw new SceneNavigatorException("No active scene.");
        return registry.get(stack.peek());
    }

    /** Returns the scene name that would be on top after popping n times. */
    private String peekBeneath(int n) {
        List<String> items = new ArrayList<>(stack);
        int idx = Math.min(n, items.size() - 1);
        return items.get(idx);
    }

    private void resetIdleTimer() {
        if (idleTimeoutMs <= 0 || idleScene == null) return;

        if (idleTimer != null) idleTimer.cancel();
        idleTimer = new Timer("scenenavfx-idle", true);
        idleTimer.schedule(new TimerTask() {
            @Override public void run() {
                javafx.application.Platform.runLater(() -> goTo(idleScene));
            }
        }, idleTimeoutMs);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder for {@link SceneNavigator}.
     *
     * <pre>{@code
     * SceneNavigator nav = SceneNavigator.builder()
     *     .holder(stackPane)
     *     .home("main")
     *     .register("main",     MainController.class, "application/Main.fxml")
     *     .register("settings", SettingsController.class, "application/Settings.fxml")
     *     .navigationListener((from, to) -> log(from + " -> " + to))
     *     .errorHandler(e -> showDialog(e.getMessage()))
     *     .idleTimeout(300_000, "screensaver")
     *     .build();
     * }</pre>
     */
    public static final class Builder {

        private StackPane holder;
        private String homeName;
        private final Map<String, SceneRecord> registry = new HashMap<>();
        private NavigationListener navigationListener;
        private java.util.function.Consumer<SceneNavigatorException> errorHandler;
        private long idleTimeoutMs = 0;
        private String idleScene;

        private Builder() {}

        /**
         * Sets the {@link StackPane} that will host the scene nodes.
         * This pane's children are replaced on every navigation call.
         *
         * @param holder the {@code StackPane} to use as the scene container
         * @return this builder
         */
        public Builder holder(StackPane holder) {
            this.holder = holder;
            return this;
        }

        /**
         * Sets the logical name of the home scene.
         * {@link SceneNavigator#goHome()} navigates to this scene.
         *
         * @param name the logical name of the home scene
         * @return this builder
         */
        public Builder home(String name) {
            this.homeName = name;
            return this;
        }

        /**
         * Registers a cached scene (FXML loaded once and reused).
         *
         * @param name            logical name used in navigation calls
         * @param controllerClass class whose package anchors the FXML resource lookup
         * @param fxmlPath        path to the FXML file
         * @return this builder
         * @throws SceneNavigatorException if a scene with the same name is already registered
         */
        public Builder register(String name, Class<?> controllerClass, String fxmlPath) {
            return register(name, controllerClass, fxmlPath, false);
        }

        /**
         * Registers a scene with explicit control over FXML caching.
         * When {@code noCache} is {@code true} the FXML is reloaded and the controller
         * re-instantiated on every {@code goTo()} call. Useful during development or for
         * scenes that must always start completely fresh.
         *
         * @param name            logical name used in navigation calls
         * @param controllerClass class whose package anchors the FXML resource lookup
         * @param fxmlPath        path to the FXML file
         * @param noCache         {@code true} to reload on every visit
         * @return this builder
         * @throws SceneNavigatorException if a scene with the same name is already registered
         */
        public Builder register(String name, Class<?> controllerClass,
                                String fxmlPath, boolean noCache) {
            if (registry.containsKey(name))
                throw new SceneNavigatorException(
                    "A scene named '" + name + "' is already registered.");
            registry.put(name, new SceneRecord(name, controllerClass, fxmlPath, noCache));
            return this;
        }

        /**
         * Registers a global navigation listener that is called after every
         * completed scene transition.
         *
         * @param listener the listener; {@code null} to clear
         * @return this builder
         */
        public Builder navigationListener(NavigationListener listener) {
            this.navigationListener = listener;
            return this;
        }

        /**
         * Registers a global error handler for FXML load failures.
         * When set, load errors are passed to the handler instead of being thrown,
         * allowing the application to display a custom error scene.
         *
         * @param handler a {@code Consumer<SceneNavigatorException>}; {@code null} to clear
         * @return this builder
         */
        public Builder errorHandler(java.util.function.Consumer<SceneNavigatorException> handler) {
            this.errorHandler = handler;
            return this;
        }

        /**
         * Configures an idle timeout. After {@code timeoutMs} milliseconds of no
         * navigation activity, the navigator automatically navigates to {@code idleScene}.
         * The timer resets on every navigation call.
         *
         * <p>Call {@link SceneNavigator#shutdown()} from {@code Application.stop()} to
         * cancel the timer cleanly.
         *
         * @param timeoutMs milliseconds of inactivity before navigating to the idle scene
         * @param idleScene the registered scene name to navigate to on timeout
         * @return this builder
         */
        public Builder idleTimeout(long timeoutMs, String idleScene) {
            this.idleTimeoutMs = timeoutMs;
            this.idleScene     = idleScene;
            return this;
        }

        /**
         * Builds and returns the configured {@link SceneNavigator}.
         *
         * @return a configured {@code SceneNavigator}
         * @throws SceneNavigatorException if required configuration is missing
         */
        public SceneNavigator build() {
            return new SceneNavigator(this);
        }
    }
}
