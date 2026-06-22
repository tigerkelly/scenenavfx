package com.tigerkelly.scenenavfx;

/**
 * Thrown when the {@link SceneNavigator} cannot complete a navigation operation.
 *
 * Common causes:
 * <ul>
 *   <li>A scene name was requested that was never registered.</li>
 *   <li>The FXML file for a scene could not be located or parsed.</li>
 *   <li>A lifecycle annotation was placed on a method with an incompatible signature.</li>
 *   <li>{@code goBack()} or {@code goHome()} was called on an empty or single-entry stack.</li>
 * </ul>
 */
public class SceneNavigatorException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public SceneNavigatorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SceneNavigatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
