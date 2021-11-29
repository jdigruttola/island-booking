package com.upgrade.islandbooking.service;

import com.upgrade.islandbooking.domain.entity.Booking;
import com.upgrade.islandbooking.domain.entity.Person;
import com.upgrade.islandbooking.domain.repository.BookingRepository;
import com.upgrade.islandbooking.domain.repository.PersonRepository;
import com.upgrade.islandbooking.vo.BookingReserveVo;
import com.upgrade.islandbooking.vo.BookingUpdateVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import redis.embedded.RedisServer;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingServiceTest.class);

    private static final String FIRST_NAME = "Javier";
    private static final String LAST_NAME = "Digruttola";
    private static final String EMAIL = "javi.digru@foo.com";
    private static final LocalDate FROM = LocalDate.now().plusDays(1);
    private static final LocalDate TO = FROM.plusDays(2);
    private static final String BOOKING_ID = "1";
    private static final String BOOKING_ID2 = "2";

    private RedisServer redisServer;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingRepository bookingRepository2;

    @Mock
    private PersonRepository personRepository;

    private RedissonClient redissonClient;

    private BookingService service;

    private BookingService service2;

     @BeforeEach
     public void setUp() {
         //Initializes the redis server for testing
         final RedisProperties redisProperties = new RedisProperties();
         redisProperties.setPort(6379);
         redisProperties.setHost("localhost");
         this.redisServer = new RedisServer(redisProperties.getPort());
         this.redisServer.start();

         //Initializes the redis client
         redissonClient = Redisson.create();

         //Initializes the service
         this.service = new BookingService(bookingRepository, personRepository, redissonClient);
         this.service.setLockingTimeout(10000L);
         this.service.setMaxDateRangeSize(3);

         this.service2 = new BookingService(bookingRepository2, personRepository, redissonClient);
         this.service2.setLockingTimeout(10000L);
         this.service2.setMaxDateRangeSize(3);

         //Mocking...
         final Person person = new Person(EMAIL, FIRST_NAME, LAST_NAME);
         final Booking updatedBooking = new Booking(person, FROM, TO);
         updatedBooking.setId(BOOKING_ID);
         final Booking updatedBooking2 = new Booking(person, FROM, TO);
         updatedBooking2.setId(BOOKING_ID2);

         final Answer<Booking> answerWithDelay = new Answer<>() {
             @Override
             public Booking answer(InvocationOnMock invocationOnMock) throws Throwable {
                 Thread.sleep(1000L);
                 return updatedBooking;
             }
         };

         Mockito.lenient().when(personRepository.save(any(Person.class))).thenReturn(person);
         Mockito.lenient().when(personRepository.findById(any(String.class))).thenReturn(null);
         Mockito.lenient().when(bookingRepository.save(any(Booking.class))).then(answerWithDelay);
         Mockito.lenient().when(bookingRepository.findByFromDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
         Mockito.lenient().when(bookingRepository.countByFromDateBetweenAndToDateGreaterThan(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
         Mockito.lenient().when(bookingRepository2.save(any(Booking.class))).thenReturn(updatedBooking2);
         Mockito.lenient().when(bookingRepository2.findByFromDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
         Mockito.lenient().when(bookingRepository2.countByFromDateBetweenAndToDateGreaterThan(any(LocalDate.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
         Mockito.lenient().when(bookingRepository.findById(any(String.class))).thenReturn(Optional.of(updatedBooking));
         Mockito.lenient().when(bookingRepository2.findById(any(String.class))).thenReturn(Optional.of(updatedBooking2));
     }

     @AfterEach
     public void afterTest() {
         this.redisServer.stop();
     }

    @DisplayName("Test concurrency for reserve")
    @Test
    public void reserve_concurrency() {
        //Given
        final BookingReserveVo vo = new BookingReserveVo();
        vo.setPersonFirstName(FIRST_NAME);
        vo.setPersonLastName(LAST_NAME);
        vo.setPersonEmail(EMAIL);
        vo.setFrom(FROM);
        vo.setTo(TO);

        boolean result = true;
        int attemps = 0;

        while(attemps < 10) {
            attemps++;
            if(!result) {
                break;
            }

            LOGGER.debug("Starting threads...");
            final BookingServiceReserveThread t1 = new BookingServiceReserveThread(vo, service);
            t1.start();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final BookingServiceReserveThread t2 = new BookingServiceReserveThread(vo, service2);
            t2.start();
            LOGGER.debug("Finishing threads...");


            while (t1.isAlive()) {
                if (t1.isServiceStillRunning()
                        && !t2.isServiceStillRunning()
                        && redissonClient.getLock("resource_lock").isHeldByThread(t2.getId())) {
                    LOGGER.error("T1 is alive and the lock is for T2");
                    result = false;
                    break;
                }
            }
        }

        assertTrue(result);
    }

    @DisplayName("Test concurrency for update")
    @Test
    public void update_concurrency() {
        //Given
        final BookingUpdateVo vo = new BookingUpdateVo();
        vo.setPersonFirstName(FIRST_NAME);
        vo.setPersonLastName(LAST_NAME);
        vo.setFrom(FROM);
        vo.setTo(TO);

        boolean result = true;
        int attempts = 0;

        while(attempts < 10) {
            attempts++;
            if(!result) {
                break;
            }

            LOGGER.info("Starting threads...");
            final BookingServiceUpdateThread t1 = new BookingServiceUpdateThread(vo, service, BOOKING_ID);
            t1.start();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final BookingServiceUpdateThread t2 = new BookingServiceUpdateThread(vo, service2, BOOKING_ID2);
            t2.start();
            LOGGER.info("Finishing threads...");


            while (t1.isAlive()) {
                if (t1.isServiceStillRunning()
                        && !t2.isServiceStillRunning()
                        && redissonClient.getLock("resource_lock").isHeldByThread(t2.getId())) {
                    LOGGER.error("T1 is alive and the lock is for T2");
                    result = false;
                    break;
                }
            }
        }

        assertTrue(result);
    }
}
