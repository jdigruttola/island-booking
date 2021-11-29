package com.upgrade.islandbooking.service;

import com.upgrade.islandbooking.domain.entity.Booking;
import com.upgrade.islandbooking.domain.entity.Person;
import com.upgrade.islandbooking.domain.repository.BookingRepository;
import com.upgrade.islandbooking.domain.repository.PersonRepository;
import com.upgrade.islandbooking.exception.Error;
import com.upgrade.islandbooking.exception.ServiceException;
import com.upgrade.islandbooking.util.DateUtil;
import com.upgrade.islandbooking.vo.BookingReserveVo;
import com.upgrade.islandbooking.vo.BookingUpdateVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);

    private static final String LOCK_KEY = "resource_lock";

    @Value("${rule.dateRange.max}")
    private Integer maxDateRangeSize;

    @Value("${locking.timeout}")
    private Long lockingTimeout;

    final private BookingRepository bookingRepository;
    final private PersonRepository personRepository;
    final private RedissonClient redissonClient;

    public BookingService(BookingRepository bookingRepository, PersonRepository personRepository, RedissonClient redissonClient) {
        this.bookingRepository = bookingRepository;
        this.personRepository = personRepository;
        this.redissonClient = redissonClient;
    }

    private Booking getExistingBooking(String id) {
        LOGGER.debug("Checking if the booking with ID " + id + " exists...");
        Optional<Booking> booking = bookingRepository.findById(id);
        if(booking == null || booking.isEmpty()) {
            LOGGER.error("Booking with ID " + id + " does not exist");
            throw new ServiceException(Error.BOOKING_NOT_FOUND, new Object[]{id});
        }
        return booking.get();
    }

    private void validateDateRangeForUpdate(Booking booking, BookingUpdateVo vo) {
        if(vo.getFrom() == null) {
            vo.setFrom(booking.getFromDate());
        }

        if(vo.getTo() == null) {
            vo.setTo(booking.getToDate());
        }

        if(!vo.isValidDateFrom()) {
            LOGGER.error("Date from must be greater than today " + vo.getFrom().toString());
            throw new ServiceException(Error.BOOKING_NOTVALID_DATE_FROM, new Object[]{vo.getFrom()});
        }

        if(!vo.isValidDateRange()) {
            LOGGER.error("Date from must be less than date to: from "
                    + vo.getFrom().toString() + " - to " + vo.getTo().toString());
            throw new ServiceException(Error.BOOKING_NOTVALID_DATE_RANGE, new Object[]{vo.getFrom(), vo.getTo()});
        }

        if(!vo.isLessThanMaxRange()) {
            LOGGER.error("The max range date is 3 days: from "
                    + vo.getFrom().toString() + " - to " + vo.getTo().toString());
            throw new ServiceException(Error.BOOKING_MAX_DATE_RANGE, new Object[]{vo.getFrom(), vo.getTo()});
        }

        Long count = bookingRepository.countByIdIsNotAndFromDateBetweenAndToDateGreaterThan(booking.getId(),
                vo.getFrom().minusDays(maxDateRangeSize),
                vo.getTo().minusDays(1),
                vo.getFrom());

        //Validates if the date rage is not available
        LOGGER.debug("Checking if date range is not being used for other booking(s)...");
        if(count != null && count.longValue() > 0) {
            LOGGER.error("Date range is being used partially or totally for other booking(s): from "
                    + vo.getFrom().toString() + " - to " + vo.getTo().toString());
            throw new ServiceException(Error.DATE_RANGE_IS_BEING_USED, new Object[]{vo.getFrom(), vo.getTo()});
        }
    }

    private void validateDateRangeForReserve(LocalDate from, LocalDate to) {
        Long count = bookingRepository.countByFromDateBetweenAndToDateGreaterThan(
                from.minusDays(maxDateRangeSize),
                to.minusDays(1),
                from);

        //Validates if the date rage is not available
        LOGGER.debug("Checking if date range is not being used for other booking(s)...");
        if(count != null && count.longValue() > 0) {
            LOGGER.error("Date range is being used partially or totally for other booking(s): from "
                    + from.toString() + " - to " + to.toString());
            throw new ServiceException(Error.DATE_RANGE_IS_BEING_USED, new Object[]{from, to});
        }
    }

    private void validatePerson(String email) {
        //Validates if the person already exists, that means the person has another existing booking
        LOGGER.debug("Checking if the person already exists...");
        Optional<Person> person = personRepository.findById(email);
        if(person != null && !person.isEmpty()) {
            LOGGER.error("There is already a person with email " + email);
            throw new ServiceException(Error.PERSON_HAS_BOOKING, new Object[]{email});
        }

    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String reserve(BookingReserveVo vo) {
        LOGGER.debug("Creating a new booking for email " + vo.getPersonEmail()
            + "and date range " + vo.getFrom() + " - " + vo.getTo());

        //Manages resource locking to avoid issues regarding concurrency in case of having multiple app server instances
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            lock.lock(lockingTimeout, TimeUnit.MILLISECONDS);

            //Date range validations
            validateDateRangeForReserve(vo.getFrom(), vo.getTo());

            //Person validations
            validatePerson(vo.getPersonEmail());

            //Creates/Updates person
            Person person = personRepository.save(
                    new Person(vo.getPersonEmail(), vo.getPersonFirstName(), vo.getPersonLastName()));

            //Creates booking
            Booking booking = new Booking(person, vo.getFrom(), vo.getTo());
            booking = bookingRepository.save(booking);

            LOGGER.info("Booking with ID " + booking.getId() + " was successfully created");
            return booking.getId();
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean update(String id, BookingUpdateVo vo) {
        LOGGER.debug("Updating booking with ID " + id);

        //Manages resource locking to avoid issues regarding concurrency in case of having multiple app server instances
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            lock.lock(lockingTimeout, TimeUnit.MILLISECONDS);

            //Gets and checks existing booking
            Booking booking = getExistingBooking(id);

            //Date range validations
            validateDateRangeForUpdate(booking, vo);

            boolean instanceChanged = false;

            Person person = booking.getPerson();

            if (vo.getPersonFirstName() != null) {
                person.setFirstName(vo.getPersonFirstName());
                instanceChanged = true;
            }

            if (vo.getPersonLastName() != null) {
                person.setLastName(vo.getPersonLastName());
                instanceChanged = true;
            }

            if (instanceChanged) {
                personRepository.save(person);
            }

            //Resets the flag
            instanceChanged = false;

            if (vo.getFrom() != null) {
                booking.setFromDate(vo.getFrom());
                instanceChanged = true;
            }

            if (vo.getTo() != null) {
                booking.setToDate(vo.getTo());
                instanceChanged = true;
            }

            if (instanceChanged) {
                bookingRepository.save(booking);
            }

            LOGGER.info("Booking with ID " + id + " was successfully updated");
            return true;
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(String id) {
        LOGGER.debug("Deleting booking with ID " + id);

        //Gets and checks existing booking
        Booking booking = getExistingBooking(id);

        //Removes the person linked to that booking
        personRepository.delete(booking.getPerson());

        //Removes the existing booking
        bookingRepository.delete(booking);
    }

    public List<LocalDate> getAvailableDays(LocalDate from, LocalDate to) {
        LOGGER.debug("Getting available dates for range " + from.toString() + " - " + to.toString());

        List<LocalDate> possibleDates = DateUtil.getDatesBetween(from, to);
        List<Booking> bookings = bookingRepository.findByFromDateBetween(from.minusDays(maxDateRangeSize), to);

        for(Booking b : bookings) {
            possibleDates = DateUtil.removeDateRange(possibleDates, b.getFromDate(), b.getToDate());
        }

        return possibleDates;
    }

    public void setMaxDateRangeSize(Integer maxDateRangeSize) {
        this.maxDateRangeSize = maxDateRangeSize;
    }

    public void setLockingTimeout(Long lockingTimeout) {
        this.lockingTimeout = lockingTimeout;
    }
}