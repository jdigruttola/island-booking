package com.upgrade.islandbooking.vo;

import lombok.*;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode
public class BookingUpdateVo {

    @Size(max = 45,
            message = "The person first name '${validatedValue}' must be less than {max} characters")
    private String personFirstName;

    @Size(max = 45,
            message = "The person last name '${validatedValue}' must be less than {max} characters")
    private String personLastName;

    private LocalDate from;

    private LocalDate to;

    @AssertTrue(message = "Date from must be less than date to")
    public boolean isValidDateRange() {
        return to == null || from == null || to.isAfter(from);
    }

    @AssertTrue(message = "Date from must be greater than today")
    public boolean isValidDateFrom() {
        return from == null || from.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "The max range date is 3 days")
    public boolean isLessThanMaxRange() {
        return from == null
                || to == null
                || !from.isBefore(to.minusDays(3));
    }
}
