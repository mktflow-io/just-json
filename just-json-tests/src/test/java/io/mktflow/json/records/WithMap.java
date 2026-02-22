package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;
import java.util.Map;

@JsonRecord
public record WithMap(String name, Map<String, Integer> scores) {}
