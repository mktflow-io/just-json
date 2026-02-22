package io.mktflow.json;

import io.mktflow.json.records.JsonAdapterRegistry;

final class TestInit {

    private static volatile boolean initialized = false;

    static void ensureInitialized() {
        if (!initialized) {
            JsonAdapterRegistry.initialize();
            initialized = true;
        }
    }

    private TestInit() {}
}
