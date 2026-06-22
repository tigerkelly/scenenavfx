package com.tigerkelly.scenenavfx;

/**
 * Observer interface for global navigation events.
 *
 * <p>Register a listener via {@link SceneNavigator.Builder#navigationListener(NavigationListener)}
 * to be notified on every scene transition. Useful for logging, analytics, or cross-cutting
 * concerns that shouldn't live inside individual controllers.
 *
 * <p>The listener fires after lifecycle hooks complete and the new scene is visible.
 *
 * <pre>{@code
 * SceneNavigator nav = SceneNavigator.builder()
 *     .navigationListener((from, to) ->
 *         System.out.println("Navigated: " + from + " -> " + to))
 *     ...
 *     .build();
 * }</pre>
 */
@FunctionalInterface
public interface NavigationListener {

    /**
     * Called after every completed scene transition.
     *
     * @param from the scene that was just left, or {@code null} on the first navigation
     * @param to   the scene that is now active
     */
    void onNavigated(String from, String to);
}
