package com.tigerkelly.scenenavfx;

import com.tigerkelly.scenenavfx.annotations.OnData;
import com.tigerkelly.scenenavfx.annotations.OnEnter;
import com.tigerkelly.scenenavfx.annotations.OnFirstLoad;
import com.tigerkelly.scenenavfx.annotations.OnLeave;
import com.tigerkelly.scenenavfx.annotations.OnPause;
import com.tigerkelly.scenenavfx.annotations.OnResume;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Handles dispatching lifecycle events to a scene controller.
 *
 * <p>For enter/leave, call order is:
 * <ol>
 *   <li>Interface method (if controller implements {@link SceneLifecycle})</li>
 *   <li>Annotated method(s)</li>
 * </ol>
 *
 * <p>Context-aware variants pass the peer scene name to {@link SceneLifecycle}
 * when the interface declares the two-argument form; the no-arg interface form is
 * called otherwise for backward compatibility.
 *
 * <p>Annotated methods must be {@code public void} with no parameters (except
 * {@code @OnData} methods, which take exactly one parameter of any type).
 */
final class LifecycleDispatcher {

    private LifecycleDispatcher() {}

    // -------------------------------------------------------------------------
    // Enter / Leave  (context-aware: peer name passed to interface)
    // -------------------------------------------------------------------------

    static void fireEnter(Object controller, String fromScene) {
        if (controller instanceof SceneLifecycle) {
            ((SceneLifecycle) controller).onEnter(fromScene);
        }
        invokeAnnotatedNoArg(controller, OnEnter.class);
    }

    static void fireLeave(Object controller, String toScene) {
        if (controller instanceof SceneLifecycle) {
            ((SceneLifecycle) controller).onLeave(toScene);
        }
        invokeAnnotatedNoArg(controller, OnLeave.class);
    }

    // -------------------------------------------------------------------------
    // One-time load
    // -------------------------------------------------------------------------

    static void fireFirstLoad(Object controller) {
        invokeAnnotatedNoArg(controller, OnFirstLoad.class);
    }

    // -------------------------------------------------------------------------
    // Data payload
    // -------------------------------------------------------------------------

    static void fireData(Object controller, Object data) {
        if (controller == null || data == null) return;

        for (Method m : controller.getClass().getMethods()) {
            if (!m.isAnnotationPresent(OnData.class)) continue;

            if (m.getReturnType() != void.class || m.getParameterCount() != 1) {
                throw new SceneNavigatorException(String.format(
                    "@OnData method '%s' in '%s' must be public void with exactly one parameter.",
                    m.getName(), controller.getClass().getSimpleName()));
            }

            try {
                m.invoke(controller, data);
            } catch (Exception e) {
                throw new SceneNavigatorException(String.format(
                    "Error invoking @OnData method '%s' in '%s'.",
                    m.getName(), controller.getClass().getSimpleName()), e);
            }
            return; // only invoke the first @OnData method found
        }
    }

    // -------------------------------------------------------------------------
    // Pause / Resume  (OS focus events)
    // -------------------------------------------------------------------------

    static void firePause(Object controller) {
        invokeAnnotatedNoArg(controller, OnPause.class);
    }

    static void fireResume(Object controller) {
        invokeAnnotatedNoArg(controller, OnResume.class);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static void invokeAnnotatedNoArg(Object controller,
                                             Class<? extends Annotation> annotationType) {
        if (controller == null) return;

        for (Method m : controller.getClass().getMethods()) {
            if (!m.isAnnotationPresent(annotationType)) continue;

            if (m.getReturnType() != void.class || m.getParameterCount() != 0) {
                throw new SceneNavigatorException(String.format(
                    "@%s method '%s' in '%s' must be public void with no parameters.",
                    annotationType.getSimpleName(),
                    m.getName(),
                    controller.getClass().getSimpleName()));
            }

            try {
                m.invoke(controller);
            } catch (Exception e) {
                throw new SceneNavigatorException(String.format(
                    "Error invoking @%s method '%s' in '%s'.",
                    annotationType.getSimpleName(),
                    m.getName(),
                    controller.getClass().getSimpleName()), e);
            }
        }
    }
}
