package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called just before the scene is replaced by another.
 *
 * This is the annotation equivalent of {@link com.tigerkelly.scenenavfx.SceneLifecycle#onLeave(String)}.
 * It is called in addition to {@code onLeave()} if the controller also implements
 * {@code SceneLifecycle} — not instead of it.
 *
 * The annotated method must be public, return void, and take no parameters.
 *
 * <pre>{@code
 * @OnLeave
 * public void handleLeave() {
 *     saveCurrentInput();
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnLeave {
}
