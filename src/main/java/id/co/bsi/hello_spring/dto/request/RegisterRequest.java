package id.co.bsi.hello_spring.dto.request;

import lombok.Data;

import java.math.BigInteger;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String confirmationPassword;
    private BigInteger phone;
}