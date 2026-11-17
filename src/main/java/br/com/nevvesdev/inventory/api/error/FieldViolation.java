package br.com.nevvesdev.inventory.api.error;

public record FieldViolation(String field, String message) {
}