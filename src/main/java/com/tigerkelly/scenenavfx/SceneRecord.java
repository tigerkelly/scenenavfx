package com.tigerkelly.scenenavfx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal record holding all state for a single registered scene.
 * Not part of the public API.
 */
final class SceneRecord {

    /** The logical name this scene was registered under. */
    final String name;

    /** Path to the FXML resource, as passed to the builder. */
    final String fxmlPath;

    /** The controller class, used for FXML location resolution. */
    final Class<?> controllerClass;

    /** When true, the FXML is reloaded on every goTo() instead of being cached. */
    final boolean noCache;

    /** The FXMLLoader used to load this scene. Null until first load. */
    FXMLLoader loader;

    /** The root node of the loaded FXML. Null until first load. */
    Node node;

    /** The controller instance. Null until first load. */
    Object controller;

    /** Epoch millis of the last time this scene became active. -1 if never. */
    long lastEnterTime = -1;

    /** Whether this scene has been loaded at least once. */
    boolean loaded = false;

    /** Scene-local key/value store, cleared on each leave. */
    final Map<String, Object> store = new HashMap<>();

    SceneRecord(String name, Class<?> controllerClass, String fxmlPath, boolean noCache) {
        this.name = name;
        this.controllerClass = controllerClass;
        this.fxmlPath = fxmlPath;
        this.noCache = noCache;
    }
}
