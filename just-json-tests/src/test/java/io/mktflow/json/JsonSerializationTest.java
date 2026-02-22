package io.mktflow.json;

import io.mktflow.json.records.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSerializationTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void serializePerson() {
        var person = new Person("Alice", 30, List.of("dev", "java"));
        String json = Json.toJson(person);
        assertTrue(json.contains("\"name\":\"Alice\""));
        assertTrue(json.contains("\"age\":30"));
        assertTrue(json.contains("\"tags\":[\"dev\",\"java\"]"));
    }

    @Test
    void serializeOrder() {
        var order = new Order("ORD-1", new BigDecimal("99.99"), 5, Status.ACTIVE);
        String json = Json.toJson(order);
        assertTrue(json.contains("\"id\":\"ORD-1\""));
        assertTrue(json.contains("\"price\":99.99"));
        assertTrue(json.contains("\"quantity\":5"));
        assertTrue(json.contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void serializeNested() {
        var address = new Address("123 Main St", "Paris", "75001");
        var nested = new Nested("home", address, List.of(address));
        String json = Json.toJson(nested);
        assertTrue(json.contains("\"label\":\"home\""));
        assertTrue(json.contains("\"street\":\"123 Main St\""));
        assertTrue(json.contains("\"city\":\"Paris\""));
    }

    @Test
    void serializeAllPrimitiveTypes() {
        var all = new AllTypes(1, 2L, 3.14, 1.5f, true,
                Integer.valueOf(10), Long.valueOf(20L), Double.valueOf(3.14),
                Float.valueOf(1.5f), Boolean.TRUE, "hello",
                new BigDecimal("123.456"), new BigInteger("999"), Status.PENDING);
        String json = Json.toJson(all);
        assertTrue(json.contains("\"primitiveInt\":1"));
        assertTrue(json.contains("\"primitiveLong\":2"));
        assertTrue(json.contains("\"primitiveBoolean\":true"));
        assertTrue(json.contains("\"text\":\"hello\""));
        assertTrue(json.contains("\"bigDecimal\":123.456"));
        assertTrue(json.contains("\"bigInteger\":999"));
        assertTrue(json.contains("\"status\":\"PENDING\""));
    }

    @Test
    void serializeWithMap() {
        var wm = new WithMap("scores", Map.of("math", 95, "english", 88));
        String json = Json.toJson(wm);
        assertTrue(json.contains("\"name\":\"scores\""));
        assertTrue(json.contains("\"math\":95"));
        assertTrue(json.contains("\"english\":88"));
    }

    @Test
    void serializeWithNullFields() {
        var person = new Person(null, 25, null);
        String json = Json.toJson(person);
        assertTrue(json.contains("\"name\":null"));
        assertTrue(json.contains("\"age\":25"));
        assertTrue(json.contains("\"tags\":null"));
    }

    @Test
    void serializeNullObject() {
        assertEquals("null", Json.toJson(null));
    }

    @Test
    void serializeEmpty() {
        var empty = new Empty();
        assertEquals("{}", Json.toJson(empty));
    }

    @Test
    void serializeEmptyList() {
        var person = new Person("Bob", 20, List.of());
        String json = Json.toJson(person);
        assertTrue(json.contains("\"tags\":[]"));
    }

    @Test
    void serializeWithNullNestedRecord() {
        var nested = new Nested("test", null, null);
        String json = Json.toJson(nested);
        assertTrue(json.contains("\"address\":null"));
        assertTrue(json.contains("\"otherAddresses\":null"));
    }
}
