package com.tigerkelly.scenenavfx.example;

// ============================================================
// EXAMPLE: How to integrate SceneNavFX into your JavaFX app
// ============================================================

import com.tigerkelly.scenenavfx.NavigationGuard;
import com.tigerkelly.scenenavfx.SceneLifecycle;
import com.tigerkelly.scenenavfx.SceneNavigator;
import com.tigerkelly.scenenavfx.SceneNavigatorException;
import com.tigerkelly.scenenavfx.annotations.OnData;
import com.tigerkelly.scenenavfx.annotations.OnEnter;
import com.tigerkelly.scenenavfx.annotations.OnFirstLoad;
import com.tigerkelly.scenenavfx.annotations.OnLeave;
import com.tigerkelly.scenenavfx.annotations.OnPause;
import com.tigerkelly.scenenavfx.annotations.OnResume;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

// ============================================================
// 1. Application entry point
// ============================================================

class MyApp extends Application {

    public static SceneNavigator nav;

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader rootLoader = new FXMLLoader(getClass().getResource("Root.fxml"));
        StackPane root = rootLoader.load();

        nav = SceneNavigator.builder()
            .holder(root)
            .home("main")
            .register("main",        MainController.class,        "Main.fxml")
            .register("settings",    SettingsController.class,    "Settings.fxml")
            .register("detail",      DetailController.class,      "Detail.fxml")
            .register("edit",        EditController.class,        "Edit.fxml")
            .register("media",       MediaController.class,       "Media.fxml")
            .register("screensaver", ScreensaverController.class, "Screensaver.fxml",
                      true /* noCache: reload fresh every visit */)
            .navigationListener((from, to) ->
                System.out.println("Navigated: " + from + " -> " + to))
            .errorHandler(e ->
                System.err.println("Nav error: " + e.getMessage()))
            .idleTimeout(300_000, "screensaver")   // 5 min idle
            .build();

        // Bind OS focus events for @OnPause / @OnResume
        nav.bindFocusTo(primaryStage);

        // Fullscreen framebuffer setup (Raspberry Pi / monocle)
        // nav.bindFullscreen(primaryStage);
        // primaryStage.show();

        // Standard desktop setup
        primaryStage.setScene(new javafx.scene.Scene(root, 720, 576));
        primaryStage.show();

        nav.goTo("main");
    }

    @Override
    public void stop() {
        nav.shutdown();  // cancel idle timer
    }
}

// ============================================================
// 2. SceneLifecycle interface — context-aware hooks
// ============================================================

class MainController implements SceneLifecycle {

    @FXML private Label lblStatus;
    @FXML private Button btnSettings;
    @FXML private Button btnDetail;

    @FXML
    public void initialize() {
        btnSettings.setOnAction(e -> MyApp.nav.goTo("settings"));
        // Pass a data payload to the detail scene
        btnDetail.setOnAction(e -> MyApp.nav.goTo("detail", new MyRecord("Item 1", "Details...")));
    }

    @Override
    public void onEnter(String fromScene) {
        // fromScene tells you where navigation came from
        if ("settings".equals(fromScene)) {
            lblStatus.setText("Back from settings.");
        } else {
            lblStatus.setText("Welcome!");
        }
    }

    @Override
    public void onLeave(String toScene) {
        System.out.println("Main leaving for: " + toScene
            + " after " + MyApp.nav.currentSceneElapsedMs() + " ms");
    }
}

// ============================================================
// 3. Annotation-only lifecycle
// ============================================================

class SettingsController {

    @FXML private Label lblLoaded;
    @FXML private Button btnBack;
    @FXML private Button btnHome;

    @FXML
    public void initialize() {
        btnBack.setOnAction(e -> MyApp.nav.goBack());
        btnHome.setOnAction(e -> MyApp.nav.goHome());
    }

    @OnFirstLoad
    public void firstLoadSetup() {
        // Runs once, after initialize(), before the scene is first shown.
        lblLoaded.setText("Loaded once.");
    }

    @OnEnter
    public void handleEnter() {
        System.out.println("Settings entered.");
    }

    @OnLeave
    public void handleLeave() {
        System.out.println("Settings left.");
    }

    @OnPause
    public void handlePause() {
        // App lost OS focus while settings is active
        System.out.println("Settings paused (app lost focus).");
    }

    @OnResume
    public void handleResume() {
        System.out.println("Settings resumed (app regained focus).");
    }
}

// ============================================================
// 4. @OnData — receiving a payload from goTo(name, data)
// ============================================================

record MyRecord(String title, String detail) {}

class DetailController {

    @FXML private Label lblTitle;
    @FXML private Label lblDetail;
    @FXML private Button btnBack;

    @FXML
    public void initialize() {
        btnBack.setOnAction(e -> MyApp.nav.goBack());
    }

    // Called before onEnter when goTo("detail", payload) is used.
    @OnData
    public void receive(MyRecord item) {
        lblTitle.setText(item.title());
        lblDetail.setText(item.detail());
    }

    @OnEnter
    public void handleEnter() {
        // Data is already set by the time this fires.
        System.out.println("Detail entered with title: " + lblTitle.getText());
    }
}

// ============================================================
// 5. NavigationGuard — blocking navigation
// ============================================================

class EditController implements NavigationGuard {

    @FXML private Button btnSave;
    @FXML private Button btnBack;
    private boolean dirty = false;

    @FXML
    public void initialize() {
        btnBack.setOnAction(e -> MyApp.nav.goBack());
        btnSave.setOnAction(e -> { save(); dirty = false; });
    }

    @Override
    public boolean canLeave(String destinationScene) {
        if (!dirty) return true;
        // Show confirmation dialog; return true only if user confirms
        var alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION,
            "Discard unsaved changes?");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void save() { /* persist data */ }
}

// ============================================================
// 6. @OnPause / @OnResume — OS focus events
// ============================================================

class MediaController {

    @FXML private Button btnBack;

    @FXML
    public void initialize() {
        btnBack.setOnAction(e -> MyApp.nav.goBack());
    }

    @OnPause
    public void pausePlayback() {
        System.out.println("App lost focus — pausing media.");
    }

    @OnResume
    public void resumePlayback() {
        System.out.println("App regained focus — resuming media.");
    }
}

// ============================================================
// 7. noCache scene — reloaded fresh every visit
// ============================================================

class ScreensaverController {

    // Registered with noCache=true, so this controller is re-instantiated
    // and @OnFirstLoad fires again on every visit.

    @OnFirstLoad
    public void init() {
        System.out.println("Screensaver loaded fresh.");
    }

    @OnEnter
    public void start() {
        System.out.println("Screensaver started.");
    }

    @OnLeave
    public void stop() {
        System.out.println("Screensaver stopped.");
    }
}

// ============================================================
// 8. Full navigation API reference
// ============================================================

class NavigationExamples {

    void examples(SceneNavigator nav) {

        // -- Navigation --
        nav.goTo("settings");             // push
        nav.goTo("detail", new MyRecord("x", "y")); // push with data payload
        nav.replace("settings");          // swap without stacking
        nav.goBack();                     // pop one
        nav.goBack(2);                    // pop two
        nav.goHome();                     // pop to home
        nav.clearHistory();               // hard reset to home, no hooks

        // -- Query --
        String cur      = nav.currentScene();          // "settings"
        String prev     = nav.previousScene();         // scene below current
        int depth       = nav.stackDepth();            // 2
        long elapsed    = nav.currentSceneElapsedMs(); // ms on current scene
        var history     = nav.history();               // ["main", "settings"]
        Object ctrl     = nav.currentController();     // current controller
        Object sc       = nav.controllerFor("settings"); // any controller
        boolean loaded  = nav.isLoaded("detail");      // false until first goTo
        String home     = nav.homeName();              // "main"

        // -- Scene-local store --
        nav.storeSet("selectedId", 42);
        Object id = nav.storeGet("selectedId");       // 42
        nav.storeClear();                              // cleared; also auto-cleared on leave

        // -- Lifecycle --
        nav.bindFocusTo(new Stage());    // wire @OnPause / @OnResume
        nav.bindFullscreen(new Stage()); // framebuffer fullscreen setup
        nav.shutdown();                  // cancel idle timer on app exit
    }
}
