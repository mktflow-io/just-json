package io.mktflow.json.internal;

import java.util.List;
import java.util.Map;

public sealed interface JsonValue {

    record JsonString(String value) implements JsonValue {}

    record JsonNumber(String value) implements JsonValue {}

    record JsonBoolean(boolean value) implements JsonValue {}

    record JsonNull() implements JsonValue {}

    record JsonArray(List<JsonValue> elements) implements JsonValue {}

    record JsonObject(Map<String, JsonValue> members) implements JsonValue {}
}
