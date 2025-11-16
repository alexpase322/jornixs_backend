package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkerCorrectionRequestDto {

    @NotNull(message = "El tipo de evento no puede ser nulo.")
    private EventType eventType;

    @NotNull(message = "La fecha y hora no pueden ser nulas.")
    private LocalDateTime timestamp;

    // Este campo es opcional. Si se envía, se edita un registro existente.
    // Si es nulo, se crea un registro nuevo.
    private Long timeLogIdToEdit;
}