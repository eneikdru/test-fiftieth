package com.eneik.epidemiology.contract;

public record RuntimeContractDto(String id, String name, boolean isActive) {
    public static RuntimeContractDto fromEntity(RuntimeContractConfiguration entity) {
        return new RuntimeContractDto(entity.getId(), entity.getName(), entity.isActive());
    }
}
