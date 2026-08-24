package com.eneik.epidemiology.contract;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "runtime_contract_configurations")
public class RuntimeContractConfiguration {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, name = "is_active")
    private boolean isActive;

    protected RuntimeContractConfiguration() {}

    public RuntimeContractConfiguration(String id, String name, boolean isActive) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
}
