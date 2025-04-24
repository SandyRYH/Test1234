package id.co.bsi.hello_spring.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    private String accountnum; // ini jadi PRIMARY KEY

    private String fullName;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    @Column(unique = true)
    private String phone;

    private int balance = 1000000;
}
