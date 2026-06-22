package com.tigerkelly.scenenavfx;

/**
 * Optional interface for controllers that need to block navigation away from their scene.
 *
 * <p>If a controller implements this interface, {@link #canLeave(String)} is called
 * before any navigation that would remove the scene from the top of the stack.
 * Returning {@code false} cancels the navigation entirely — no lifecycle hooks fire
 * and the scene remains active.
 *
 * <p>Typical use: unsaved-changes dialogs, form validation, confirmation prompts.
 *
 * <pre>{@code
 * public class EditController implements NavigationGuard {
 *
 *     private boolean dirty = false;
 *
 *     @Override
 *     public boolean canLeave(String destinationScene) {
 *         if (!dirty) return true;
 *         ButtonType result = myAlert.showAndWait().orElse(ButtonType.CANCEL);
 *         return result == ButtonType.OK;
 *     }
 * }
 * }</pre>
 */
public interface NavigationGuard {

    /**
     * Called before the scene is about to be left.
     * Return {@code true} to allow navigation, {@code false} to block it.
     *
     * @param destinationScene the logical name of the scene being navigated to,
     *                         or {@code null} if navigating back to an unknown previous scene
     * @return {@code true} if navigation should proceed, {@code false} to cancel
     */
    boolean canLeave(String destinationScene);
}
