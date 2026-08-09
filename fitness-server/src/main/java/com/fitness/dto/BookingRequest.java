package com.fitness.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long coachId;
    @NotNull private Long venueId;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
}
