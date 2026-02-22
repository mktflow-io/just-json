package io.mktflow.json.records;

import io.mktflow.json.JsonProperty;

public enum Priority {

    @JsonProperty("low_priority")
    LOW("low_priority"),

    @JsonProperty("medium_priority")
    MEDIUM("medium_priority"),

    @JsonProperty("high_priority")
    HIGH("high_priority");

    private final String value;

    Priority(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
