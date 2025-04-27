package id.co.bsi.hello_spring.dto.response;

import lombok.Data;

@Data
public class DashboardResponse {
    private String status;
    private String message;
    private DashboardData data;

    @Data
    public static class DashboardData {
        private String accountnum;
        private String fullname;
        private int balance;
        private String email;
        private String phone;
    }
}
