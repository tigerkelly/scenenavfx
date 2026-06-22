package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called when the application loses OS focus.
 *
 * <p>Unlike {@link OnLeave}, which fires on scene transitions, {@code @OnPause}
 * fires when the JavaFX window loses focus (e.g. the user switches to another
 * application, the display blanks, or the OS suspends). Only the currently active
 * scene's controller receives this event.
 *
 * <p>Call {@link com.tigerkelly.scenenavfx.SceneNavigator#bindFocusTo(javafx.stage.Stage)}
 * to activate pause/resume tracking for a stage.
 *
 * <p>The annotated method must be {@code public void} with no parameters.
 *
 * <pre>{@code
 * @OnPause
 * public void handlePause() {
 *     videoPlayer.pause();
 *     saveState();
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnPause {
}
