package io.mktflow.json;

import io.mktflow.json.records.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonDeserializationTest {

    @BeforeAll
    static void init() {
        TestInit.ensureInitialized();
    }

    @Test
    void deserializePerson() {
        String json = """
                {"name":"Alice","age":30,"tags":["dev","java"]}""";
        Person p = Json.fromJson(json, Person.class);
        assertEquals("Alice", p.name());
        assertEquals(30, p.age());
        assertEquals(List.of("dev", "java"), p.tags());
    }

    @Test
    void deserializeOrder() {
        String json = """
                {"id":"ORD-1","price":99.99,"quantity":5,"status":"ACTIVE"}""";
        Order o = Json.fromJson(json, Order.class);
        assertEquals("ORD-1", o.id());
        assertEquals(new BigDecimal("99.99"), o.price());
        assertEquals(5, o.quantity());
        assertEquals(Status.ACTIVE, o.status());
    }

    @Test
    void deserializeNested() {
        String json = """
                {"label":"home","address":{"street":"123 Main","city":"Paris","zipCode":"75001"},"otherAddresses":[]}""";
        Nested n = Json.fromJson(json, Nested.class);
        assertEquals("home", n.label());
        assertNotNull(n.address());
        assertEquals("123 Main", n.address().street());
        assertEquals("Paris", n.address().city());
        assertEquals("75001", n.address().zipCode());
        assertEquals(List.of(), n.otherAddresses());
    }

    @Test
    void deserializeAllTypes() {
        String json = """
                {"primitiveInt":1,"primitiveLong":2,"primitiveDouble":3.14,"primitiveFloat":1.5,\
                "primitiveBoolean":true,"boxedInt":10,"boxedLong":20,"boxedDouble":3.14,"boxedFloat":1.5,\
                "boxedBoolean":true,"text":"hello","bigDecimal":123.456,"bigInteger":999,"status":"PENDING"}""";
        AllTypes a = Json.fromJson(json, AllTypes.class);
        assertEquals(1, a.primitiveInt());
        assertEquals(2L, a.primitiveLong());
        assertEquals(3.14, a.primitiveDouble(), 0.001);
        assertEquals(1.5f, a.primitiveFloat(), 0.001);
        assertTrue(a.primitiveBoolean());
        assertEquals(10, a.boxedInt());
        assertEquals(20L, a.boxedLong());
        assertEquals(3.14, a.boxedDouble(), 0.001);
        assertEquals(1.5f, a.boxedFloat(), 0.001);
        assertTrue(a.boxedBoolean());
        assertEquals("hello", a.text());
        assertEquals(new BigDecimal("123.456"), a.bigDecimal());
        assertEquals(new BigInteger("999"), a.bigInteger());
        assertEquals(Status.PENDING, a.status());
    }

    @Test
    void deserializeWithMap() {
        String json = """
                {"name":"scores","scores":{"math":95,"english":88}}""";
        WithMap wm = Json.fromJson(json, WithMap.class);
        assertEquals("scores", wm.name());
        assertEquals(95, wm.scores().get("math"));
        assertEquals(88, wm.scores().get("english"));
    }

    @Test
    void deserializeWithNullFields() {
        String json = """
                {"name":null,"age":25,"tags":null}""";
        Person p = Json.fromJson(json, Person.class);
        assertNull(p.name());
        assertEquals(25, p.age());
        assertNull(p.tags());
    }

    @Test
    void deserializeEmpty() {
        String json = "{}";
        Empty e = Json.fromJson(json, Empty.class);
        assertNotNull(e);
    }

    @Test
    void deserializeWithWhitespace() {
        String json = """
                {
                  "name" : "Alice" ,
                  "age" : 30 ,
                  "tags" : [ "a" , "b" ]
                }""";
        Person p = Json.fromJson(json, Person.class);
        assertEquals("Alice", p.name());
        assertEquals(30, p.age());
        assertEquals(List.of("a", "b"), p.tags());
    }

    @Test
    void deserializeWithNullNestedRecord() {
        String json = """
                {"label":"test","address":null,"otherAddresses":null}""";
        Nested n = Json.fromJson(json, Nested.class);
        assertEquals("test", n.label());
        assertNull(n.address());
        assertNull(n.otherAddresses());
    }
}
