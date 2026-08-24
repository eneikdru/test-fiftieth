package com.eneik.epidemiology.contract;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class RuntimeContractService {

    private final RuntimeContractRepository repository;

    public RuntimeContractService(RuntimeContractRepository repository) {
        this.repository = repository;
    }

    public Optional<RuntimeContractDto> getContract(String id) {
        return repository.findById(id).map(RuntimeContractDto::fromEntity);
    }

    public RuntimeContractDto createOrUpdateContract(String id, String name, boolean isActive) {
        RuntimeContractConfiguration entity = new RuntimeContractConfiguration(id, name, isActive);
        return RuntimeContractDto.fromEntity(repository.save(entity));
    }
}
