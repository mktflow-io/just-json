package io.mktflow.json.internal;

import io.mktflow.json.JsonException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class JsonMapper {

    private JsonMapper() {}

    public static String toString(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonString s) {
            return s.value();
        }
        throw new JsonException("Expected JSON string but got " + value.getClass().getSimpleName());
    }

    public static int toInt(JsonValue value) {
        if (value instanceof JsonValue.JsonNumber n) {
            return Integer.parseInt(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static long toLong(JsonValue value) {
        if (value instanceof JsonValue.JsonNumber n) {
            return Long.parseLong(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static double toDouble(JsonValue value) {
        if (value instanceof JsonValue.JsonNumber n) {
            return Double.parseDouble(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static float toFloat(JsonValue value) {
        if (value instanceof JsonValue.JsonNumber n) {
            return Float.parseFloat(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static boolean toBoolean(JsonValue value) {
        if (value instanceof JsonValue.JsonBoolean b) {
            return b.value();
        }
        throw new JsonException("Expected JSON boolean but got " + value.getClass().getSimpleName());
    }

    public static Integer toIntegerBoxed(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return Integer.parseInt(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static Long toLongBoxed(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return Long.parseLong(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static Double toDoubleBoxed(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return Double.parseDouble(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static Float toFloatBoxed(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return Float.parseFloat(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static Boolean toBooleanBoxed(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonBoolean b) {
            return b.value();
        }
        throw new JsonException("Expected JSON boolean but got " + value.getClass().getSimpleName());
    }

    public static BigDecimal toBigDecimal(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return new BigDecimal(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static BigInteger toBigInteger(JsonValue value) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonNumber n) {
            return new BigInteger(n.value());
        }
        throw new JsonException("Expected JSON number but got " + value.getClass().getSimpleName());
    }

    public static <E extends Enum<E>> E toEnum(JsonValue value, Class<E> enumClass) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonString s) {
            return Enum.valueOf(enumClass, s.value());
        }
        throw new JsonException("Expected JSON string for enum but got " + value.getClass().getSimpleName());
    }

    public static <T> List<T> toList(JsonValue value, Function<JsonValue, T> elementMapper) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonArray arr) {
            var result = new ArrayList<T>(arr.elements().size());
            for (var element : arr.elements()) {
                result.add(elementMapper.apply(element));
            }
            return result;
        }
        throw new JsonException("Expected JSON array but got " + value.getClass().getSimpleName());
    }

    public static <V> Map<String, V> toMap(JsonValue value, Function<JsonValue, V> valueMapper) {
        if (value instanceof JsonValue.JsonNull) {
            return null;
        }
        if (value instanceof JsonValue.JsonObject obj) {
            var result = new LinkedHashMap<String, V>(obj.members().size());
            for (var entry : obj.members().entrySet()) {
                result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
            }
            return result;
        }
        throw new JsonException("Expected JSON object but got " + value.getClass().getSimpleName());
    }

    public static boolean isNull(JsonValue value) {
        return value == null || value instanceof JsonValue.JsonNull;
    }
}
