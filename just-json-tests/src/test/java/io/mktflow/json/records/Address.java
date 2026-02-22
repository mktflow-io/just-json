package io.mktflow.json.records;

import io.mktflow.json.JsonRecord;

@JsonRecord
public record Address(String street, String city, String zipCode) {}
