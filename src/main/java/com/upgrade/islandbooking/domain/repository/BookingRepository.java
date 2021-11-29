package com.upgrade.islandbooking.domain.repository;

import com.upgrade.islandbooking.domain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByFromDateBetween(LocalDate fromDate, LocalDate toDate);

    Long countByFromDateBetweenAndToDateGreaterThan(LocalDate fromFrom, LocalDate fromTo, LocalDate to);

    Long countByIdIsNotAndFromDateBetweenAndToDateGreaterThan(String id, LocalDate fromFrom,
                                                                      LocalDate fromTo, LocalDate to);
}
