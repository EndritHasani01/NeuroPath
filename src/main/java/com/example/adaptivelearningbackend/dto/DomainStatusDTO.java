package com.example.adaptivelearningbackend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DomainStatusDTO extends DomainDTO{
    private boolean inProgress;
}
