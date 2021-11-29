package com.upgrade.islandbooking.exception;

public enum Error {
    BOOKING_NOT_FOUND("booking.notFound"),
    DATE_RANGE_IS_BEING_USED("booking.dateRangeIsBeingUsed"),
    PERSON_HAS_BOOKING("booking.personHasBooking"),
    BOOKING_NOTVALID_DATE_RANGE("booking.validDateRange"),
    BOOKING_NOTVALID_DATE_FROM("booking.validDateFrom"),
    BOOKING_MAX_DATE_RANGE("booking.maxDateRange");

    private final String key;

    Error(String key) {
        this.key = key;
    }

    /**
     * Its parent (BusinessException) is the only one which has access to this method
     * @return
     */
    public String getKey() {
        return key;
    }
}
