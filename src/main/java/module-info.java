/**
 * SceneNavFX — drop-in JavaFX scene navigation for embedded and framebuffer applications.
 *
 * <p>Public packages:
 * <ul>
 *   <li>{@code com.tigerkelly.scenenavfx} — core API ({@code SceneNavigator}, {@code SceneLifecycle},
 *       {@code NavigationGuard}, {@code NavigationListener})</li>
 *   <li>{@code com.tigerkelly.scenenavfx.annotations} — optional lifecycle annotations</li>
 * </ul>
 */
module com.tigerkelly.scenenavfx {

    requires javafx.fxml;
    requires javafx.controls;

    opens com.tigerkelly.scenenavfx to javafx.fxml;

    exports com.tigerkelly.scenenavfx;
    exports com.tigerkelly.scenenavfx.annotations;
}
