package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;
import java.math.BigDecimal;

@JsonRecord
public record Order(String id, BigDecimal price, int quantity, Status status) {}
