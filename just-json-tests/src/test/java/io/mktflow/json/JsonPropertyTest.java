package io.mktflow.json;

import io.mktflow.json.internal.JsonValue;
import io.mktflow.json.records.Priority;
import io.mktflow.json.records.SnakeCaseRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JsonPropertyTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void deserializeWithJsonPropertyFieldRenaming() {
        String json = """
                {"name":"Alice","first_name":"Alice","last_name":"Smith","cost_min":10.5,"is_active":true,"priority":"high_priority"}""";
        SnakeCaseRecord r = Json.fromJson(json, SnakeCaseRecord.class);
        assertEquals("Alice", r.name());
        assertEquals("Alice", r.firstName());
        assertEquals("Smith", r.lastName());
        assertEquals(new BigDecimal("10.5"), r.costMin());
        assertTrue(r.isActive());
        assertEquals(Priority.HIGH, r.priority());
    }

    @Test
    void serializeWithJsonPropertyFieldRenaming() {
        SnakeCaseRecord r = new SnakeCaseRecord("Alice", "Alice", "Smith", new BigDecimal("10.5"), true, Priority.HIGH);
        String json = Json.toJson(r);
        assertTrue(json.contains("\"first_name\""));
        assertTrue(json.contains("\"last_name\""));
        assertTrue(json.contains("\"cost_min\""));
        assertTrue(json.contains("\"is_active\""));
        assertFalse(json.contains("\"firstName\""));
        assertFalse(json.contains("\"lastName\""));
        assertFalse(json.contains("\"costMin\""));
        assertFalse(json.contains("\"isActive\""));
    }

    @Test
    void serializeEnumWithJsonProperty() {
        SnakeCaseRecord r = new SnakeCaseRecord("test", "A", "B", BigDecimal.ONE, false, Priority.LOW);
        String json = Json.toJson(r);
        assertTrue(json.contains("\"low_priority\""));
        assertFalse(json.contains("\"LOW\""));
    }

    @Test
    void deserializeEnumWithJsonProperty() {
        String json = """
                {"name":"test","first_name":"A","last_name":"B","cost_min":1,"is_active":false,"priority":"medium_priority"}""";
        SnakeCaseRecord r = Json.fromJson(json, SnakeCaseRecord.class);
        assertEquals(Priority.MEDIUM, r.priority());
    }

    @Test
    void roundTripWithJsonProperty() {
        SnakeCaseRecord original = new SnakeCaseRecord("Bob", "Bob", "Jones", new BigDecimal("99.99"), true, Priority.HIGH);
        String json = Json.toJson(original);
        SnakeCaseRecord deserialized = Json.fromJson(json, SnakeCaseRecord.class);
        assertEquals(original, deserialized);
    }

    @Test
    void deserializeEnumWithNullValue() {
        String json = """
                {"name":"test","first_name":"A","last_name":"B","cost_min":1,"is_active":false,"priority":null}""";
        SnakeCaseRecord r = Json.fromJson(json, SnakeCaseRecord.class);
        assertNull(r.priority());
    }

    @Test
    void deserializeEnumWithUnknownValueThrows() {
        String json = """
                {"name":"test","first_name":"A","last_name":"B","cost_min":1,"is_active":false,"priority":"unknown_value"}""";
        assertThrows(JsonException.class, () -> Json.fromJson(json, SnakeCaseRecord.class));
    }

    @Test
    void jsonParseLowLevel() {
        String json = """
                {"type":"snapshot","channel":"book","data":[1,2,3]}""";
        JsonValue value = Json.parse(json);
        assertInstanceOf(JsonValue.JsonObject.class, value);
        JsonValue.JsonObject obj = (JsonValue.JsonObject) value;
        assertEquals("snapshot", ((JsonValue.JsonString) obj.members().get("type")).value());
        assertEquals("book", ((JsonValue.JsonString) obj.members().get("channel")).value());
        assertInstanceOf(JsonValue.JsonArray.class, obj.members().get("data"));
    }

    @Test
    void jsonParseNullInputThrows() {
        assertThrows(JsonException.class, () -> Json.parse(null));
    }

    @Test
    void jsonParseEmptyInputThrows() {
        assertThrows(JsonException.class, () -> Json.parse("  "));
    }
}
