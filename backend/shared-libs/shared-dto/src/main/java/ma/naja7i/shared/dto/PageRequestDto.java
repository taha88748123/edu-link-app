package ma.naja7i.shared.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequestDto(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        String sort
) {
    public PageRequestDto {
        if (size == 0) size = 20;
    }
}
