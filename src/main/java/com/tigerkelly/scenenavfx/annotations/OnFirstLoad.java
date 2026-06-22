package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called exactly once, the first time a scene is loaded.
 *
 * Unlike {@link OnEnter}, which fires on every visit, this annotation fires only
 * on the initial FXML load — after JavaFX's own {@code initialize()} has run but
 * before the scene becomes visible for the first time.
 *
 * Use this for one-time setup that is too late to do in {@code initialize()}
 * (e.g., wiring scene-level listeners that need the node to be in a live scene graph)
 * but should not repeat on every return to the scene.
 *
 * The annotated method must be public, return void, and take no parameters.
 *
 * <pre>{@code
 * @OnFirstLoad
 * public void setupOnce() {
 *     myChart.setAnimated(false);
 *     myTable.setItems(dataModel.getRows());
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnFirstLoad {
}
