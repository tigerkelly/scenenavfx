package com.tigerkelly.scenenavfx;

/**
 * Required lifecycle interface for scene controllers.
 *
 * <p>Controllers registered with {@link SceneNavigator} must implement this interface.
 * Both methods receive the name of the peer scene so controllers can adjust behaviour
 * based on where navigation is coming from or going to.
 *
 * <ul>
 *   <li>{@link #onEnter(String)} — called each time this scene becomes the active view.
 *       {@code fromScene} is the scene that was just left, or {@code null} on first entry.</li>
 *   <li>{@link #onLeave(String)} — called just before this scene is replaced.
 *       {@code toScene} is the scene about to become active, or {@code null} when
 *       the destination is not yet known (e.g. {@code goBack()}).</li>
 * </ul>
 *
 * <p>Optional lifecycle hooks are available via annotations in
 * {@link com.tigerkelly.scenenavfx.annotations} for finer-grained control.
 * Annotations fire after the interface methods.
 *
 * <pre>{@code
 * public class MyController implements SceneLifecycle {
 *
 *     @Override
 *     public void onEnter(String fromScene) {
 *         if ("login".equals(fromScene)) {
 *             lblWelcome.setText("Welcome!");
 *         }
 *     }
 *
 *     @Override
 *     public void onLeave(String toScene) {
 *         System.out.println("Leaving for: " + toScene);
 *     }
 * }
 * }</pre>
 */
public interface SceneLifecycle {

    /**
     * Called each time this scene becomes the visible scene.
     *
     * @param fromScene the logical name of the scene that was just left,
     *                  or {@code null} if this is the first navigation
     */
    void onEnter(String fromScene);

    /**
     * Called just before this scene is replaced by another.
     *
     * @param toScene the logical name of the scene about to become active,
     *                or {@code null} if the destination is not yet determined
     */
    void onLeave(String toScene);
}
