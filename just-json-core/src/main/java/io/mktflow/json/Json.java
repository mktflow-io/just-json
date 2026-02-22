package io.mktflow.json;

import io.mktflow.json.internal.JsonParser;
import io.mktflow.json.internal.JsonValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Json {

    private static final Map<Class<?>, Function<Object, String>> SERIALIZERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Function<JsonValue, ?>> DESERIALIZERS = new ConcurrentHashMap<>();

    private Json() {}

    public static <T> void registerAdapter(Class<T> type,
                                           Function<T, String> serializer,
                                           Function<JsonValue, T> deserializer) {
        @SuppressWarnings("unchecked")
        Function<Object, String> rawSerializer = (Function<Object, String>) (Function<?, ?>) serializer;
        SERIALIZERS.put(type, rawSerializer);
        DESERIALIZERS.put(type, deserializer);
    }

    public static <T> String toJson(T obj) {
        if (obj == null) {
            return "null";
        }
        Function<Object, String> serializer = SERIALIZERS.get(obj.getClass());
        if (serializer == null) {
            throw new JsonException("No JSON adapter registered for " + obj.getClass().getName()
                    + ". Annotate the record with @JsonRecord and ensure the annotation processor ran.");
        }
        return serializer.apply(obj);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new JsonException("Input JSON string is null or empty");
        }
        @SuppressWarnings("unchecked")
        Function<JsonValue, T> deserializer = (Function<JsonValue, T>) DESERIALIZERS.get(type);
        if (deserializer == null) {
            throw new JsonException("No JSON adapter registered for " + type.getName()
                    + ". Annotate the record with @JsonRecord and ensure the annotation processor ran.");
        }
        JsonValue value = JsonParser.parse(json);
        return deserializer.apply(value);
    }

    public static JsonValue parse(String json) {
        return JsonParser.parse(json);
    }
}
