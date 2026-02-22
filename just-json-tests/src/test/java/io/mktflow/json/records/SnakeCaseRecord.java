package io.mktflow.json.records;

import io.mktflow.json.JsonProperty;
import io.mktflow.json.JsonRecord;

import java.math.BigDecimal;

@JsonRecord
public record SnakeCaseRecord(
        String name,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("cost_min") BigDecimal costMin,
        @JsonProperty("is_active") boolean isActive,
        Priority priority
) {}
