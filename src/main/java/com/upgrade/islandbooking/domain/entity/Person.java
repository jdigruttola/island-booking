package com.upgrade.islandbooking.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
@Entity
@Table(name = "person")
public class Person {
    @Id
    private String email;
    private String firstName;
    private String lastName;
}
