package com.upgrade.islandbooking.controller;

import com.upgrade.islandbooking.vo.BookingReserveVo;
import com.upgrade.islandbooking.vo.BookingResponseVo;
import com.upgrade.islandbooking.vo.BookingUpdateVo;
import com.upgrade.islandbooking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingController.class);

    final private BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public BookingResponseVo reserve(@Valid @RequestBody BookingReserveVo request) {
        LOGGER.debug("Request data: " + request.toString());
        String bookingId = service.reserve(request);
        return new BookingResponseVo(bookingId);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable String id, @Valid @RequestBody BookingUpdateVo request) {
        LOGGER.debug("Updating booking with ID " + id + " Request data: " + request.toString());
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        LOGGER.debug("Deleting booking with ID  " + id);
        service.delete(id);
    }

    @GetMapping
    public List<LocalDate> getAvailableDates(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        LOGGER.debug("Getting available date from: " + from + " to " + to);
        return service.getAvailableDays(from, to);
    }
}
