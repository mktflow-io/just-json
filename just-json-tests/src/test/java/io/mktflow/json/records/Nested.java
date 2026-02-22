package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;
import java.util.List;

@JsonRecord
public record Nested(String label, Address address, List<Address> otherAddresses) {}
