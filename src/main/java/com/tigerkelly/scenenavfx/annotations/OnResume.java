package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called when the application regains OS focus.
 *
 * <p>The counterpart to {@link OnPause}. Fires when the JavaFX window regains focus
 * after having lost it. Only the currently active scene's controller receives this event.
 *
 * <p>Call {@link com.tigerkelly.scenenavfx.SceneNavigator#bindFocusTo(javafx.stage.Stage)}
 * to activate pause/resume tracking for a stage.
 *
 * <p>The annotated method must be {@code public void} with no parameters.
 *
 * <pre>{@code
 * @OnResume
 * public void handleResume() {
 *     videoPlayer.play();
 *     refreshData();
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnResume {
}
