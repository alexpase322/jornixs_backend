package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkerCorrectionRequestDto {

    @NotNull(message = "Event type cannot be null.")
    private EventType eventType;

    @NotNull(message = "Date and time cannot be null.")
    private LocalDateTime timestamp;

    // Optional. If provided, edits an existing record. If null, creates a new one.
    private Long timeLogIdToEdit;
}
