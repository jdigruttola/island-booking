package com.upgrade.islandbooking.service;

import com.upgrade.islandbooking.vo.BookingReserveVo;

public class BookingServiceReserveThread extends Thread{

    final private BookingReserveVo vo;
    final private BookingService service;
    private String bookingId;

    public BookingServiceReserveThread(BookingReserveVo vo, BookingService service) {
        this.vo = vo;
        this.service = service;
        this.bookingId = null;
    }

    @Override
    public void run() {
        bookingId = service.reserve(vo);
    }

    public boolean isServiceStillRunning() {
        return bookingId == null;
    }
}
