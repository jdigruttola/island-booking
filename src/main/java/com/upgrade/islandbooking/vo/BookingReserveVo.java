package com.upgrade.islandbooking.vo;

import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode
public class BookingReserveVo {

    @NotEmpty(message = "The person email cannot be empty")
    @Email(message = "'${validatedValue}' is not a valid email")
    private String personEmail;

    @NotEmpty(message = "The person first name cannot be empty")
    @Size(min = 1, max = 45,
            message = "The person first name '${validatedValue}' must be between {min} and {max} characters long")
    private String personFirstName;

    @NotEmpty(message = "The person last name cannot be empty")
    @Size(min = 1, max = 45,
            message = "The person last name '${validatedValue}' must be between {min} and {max} characters long")
    private String personLastName;

    @NotNull(message = "The booking date from cannot be empty")
    private LocalDate from;

    @NotNull(message = "The booking date to cannot be empty")
    private LocalDate to;

    @AssertTrue(message = "Date from must be less than date to")
    public boolean isValidDateRange() {
        return to != null && from != null && to.isAfter(from);
    }

    @AssertTrue(message = "Date from must be greater than today")
    public boolean isValidDateFrom() {
        return from != null && from.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "The max range date is 3 days")
    public boolean isLessThanMaxRange() {
        return from != null
                && to != null
                && !from.isBefore(to.minusDays(3));
    }
}
