package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;
import java.util.List;

@JsonRecord
public record Person(String name, int age, List<String> tags) {}
