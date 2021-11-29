package com.upgrade.islandbooking.service;

import com.upgrade.islandbooking.vo.BookingUpdateVo;

public class BookingServiceUpdateThread extends Thread{

    final private BookingUpdateVo vo;
    final private BookingService service;
    final private String bookingId;
    private boolean update;

    public BookingServiceUpdateThread(BookingUpdateVo vo, BookingService service, String bookingId) {
        this.vo = vo;
        this.service = service;
        this.bookingId = bookingId;
        this.update = false;
    }

    @Override
    public void run() {
        update = service.update(bookingId, vo);
    }

    public boolean isServiceStillRunning() {
        return !update;
    }
}
