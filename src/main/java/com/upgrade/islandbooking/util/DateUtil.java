package com.upgrade.islandbooking.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DateUtil {

    public static LocalDate convertToLocalDate(Date d) {
        return LocalDate.ofInstant(
                d.toInstant(), ZoneId.systemDefault());
    }

    public static List<LocalDate> getDatesBetween(LocalDate from, LocalDate to) {
        return from.datesUntil(to).collect(Collectors.toList());
    }

    public static List<LocalDate> removeDateRange(List<LocalDate> baseDates, LocalDate from, LocalDate to) {
        List<LocalDate> datesToRemove = getDatesBetween(from, to);
        baseDates.removeAll(datesToRemove);
        return baseDates;
    }
}
