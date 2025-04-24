
package id.co.bsi.hello_spring.dto.request;

import lombok.Data;

@Data
public class TopUpRequest {
    private int amount;
    private String from;
    private String notes;
    private String accountnum;
//    private String pin;
    private String method;

    // Tambahan validasi kartu
    private String cardNumber;
    private String cvv;
    private String expirationDate;
}

