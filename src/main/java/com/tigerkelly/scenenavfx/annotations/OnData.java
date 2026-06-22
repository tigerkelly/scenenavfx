package com.tigerkelly.scenenavfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to receive a data payload passed via
 * {@link com.tigerkelly.scenenavfx.SceneNavigator#goTo(String, Object)}.
 *
 * <p>The annotated method must be {@code public}, return {@code void}, and accept
 * exactly one parameter of any type. The navigator passes the data object directly;
 * a {@link ClassCastException} will propagate if the type does not match.
 *
 * <p>{@code @OnData} fires after {@code @OnFirstLoad} (if applicable) and before
 * {@code onEnter()} / {@code @OnEnter}, so data is available when the enter hooks run.
 *
 * <p>If {@code goTo(name)} is called without data, {@code @OnData} is not invoked.
 *
 * <pre>{@code
 * @OnData
 * public void receive(MyRecord item) {
 *     lblTitle.setText(item.title());
 *     lblDetail.setText(item.detail());
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnData {
}
