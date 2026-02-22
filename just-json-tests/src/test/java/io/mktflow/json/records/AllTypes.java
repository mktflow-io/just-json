package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;
import java.math.BigDecimal;
import java.math.BigInteger;

@JsonRecord
public record AllTypes(
        int primitiveInt,
        long primitiveLong,
        double primitiveDouble,
        float primitiveFloat,
        boolean primitiveBoolean,
        Integer boxedInt,
        Long boxedLong,
        Double boxedDouble,
        Float boxedFloat,
        Boolean boxedBoolean,
        String text,
        BigDecimal bigDecimal,
        BigInteger bigInteger,
        Status status
) {}
