package io.mktflow.json.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class JsonWriter {

    private JsonWriter() {}

    public static void writeString(String value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u");
                        sb.append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    public static void writeInt(int value, StringBuilder sb) {
        sb.append(value);
    }

    public static void writeLong(long value, StringBuilder sb) {
        sb.append(value);
    }

    public static void writeDouble(double value, StringBuilder sb) {
        sb.append(value);
    }

    public static void writeFloat(float value, StringBuilder sb) {
        sb.append(value);
    }

    public static void writeBoolean(boolean value, StringBuilder sb) {
        sb.append(value);
    }

    public static void writeInteger(Integer value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.intValue());
        }
    }

    public static void writeLongBoxed(Long value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.longValue());
        }
    }

    public static void writeDoubleBoxed(Double value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.doubleValue());
        }
    }

    public static void writeFloatBoxed(Float value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.floatValue());
        }
    }

    public static void writeBooleanBoxed(Boolean value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.booleanValue());
        }
    }

    public static void writeBigDecimal(BigDecimal value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.toPlainString());
        }
    }

    public static void writeBigInteger(BigInteger value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.toString());
        }
    }

    public static <E extends Enum<E>> void writeEnum(E value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else {
            writeString(value.name(), sb);
        }
    }

    public static <T> void writeList(List<T> list, StringBuilder sb, BiConsumer<T, StringBuilder> elementWriter) {
        if (list == null) {
            sb.append("null");
            return;
        }
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            elementWriter.accept(list.get(i), sb);
        }
        sb.append(']');
    }

    public static <V> void writeMap(Map<String, V> map, StringBuilder sb, BiConsumer<V, StringBuilder> valueWriter) {
        if (map == null) {
            sb.append("null");
            return;
        }
        sb.append('{');
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(entry.getKey(), sb);
            sb.append(':');
            valueWriter.accept(entry.getValue(), sb);
        }
        sb.append('}');
    }

    public static void writeNull(StringBuilder sb) {
        sb.append("null");
    }

    public static void writeJsonValue(Object value, StringBuilder sb, BiConsumer<Object, StringBuilder> objectWriter) {
        if (value == null) {
            sb.append("null");
        } else {
            objectWriter.accept(value, sb);
        }
    }
}
