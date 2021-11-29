package com.upgrade.islandbooking.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @OneToOne
    private Person person;

    @Column(nullable = false, unique = true)
    private LocalDate fromDate;

    @Column(nullable = false, unique = true)
    private LocalDate toDate;

    public Booking(Person person, LocalDate fromDate, LocalDate toDate) {
        this.person = person;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }
}
