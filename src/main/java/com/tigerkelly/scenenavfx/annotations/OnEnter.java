package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called each time the scene becomes active.
 *
 * This is the annotation equivalent of {@link com.tigerkelly.scenenavfx.SceneLifecycle#onEnter()}.
 * It is called in addition to {@code onEnter()} if the controller also implements
 * {@code SceneLifecycle} — not instead of it.
 *
 * The annotated method must be public, return void, and take no parameters.
 *
 * <pre>{@code
 * @OnEnter
 * public void handleEnter() {
 *     label.setText("Welcome back!");
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnEnter {
}
