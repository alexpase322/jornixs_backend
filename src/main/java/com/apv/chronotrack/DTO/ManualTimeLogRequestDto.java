package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ManualTimeLogRequestDto {

    @NotNull
    private Long workerId;

    @NotNull
    private EventType eventType;

    @NotNull
    private LocalDateTime timestamp;

    // Este campo es opcional. Si se envía, se edita un registro existente.
    // Si es nulo, se crea un registro nuevo.
    private Long timeLogIdToEdit;
}