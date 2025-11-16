package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TimeLogDTO {
    private Long id;
    private EventType eventType;
    private LocalDateTime timestamp;
    private Long workWeekId;

}
