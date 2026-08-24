package com.eneik.epidemiology.contract;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime-contracts")
public class RuntimeContractController {

    private final RuntimeContractService service;

    public RuntimeContractController(RuntimeContractService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuntimeContractDto> getContract(@PathVariable String id) {
        return service.getContract(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RuntimeContractDto> createContract(@RequestBody RuntimeContractDto request) {
        return ResponseEntity.ok(service.createOrUpdateContract(request.id(), request.name(), request.isActive()));
    }
}
