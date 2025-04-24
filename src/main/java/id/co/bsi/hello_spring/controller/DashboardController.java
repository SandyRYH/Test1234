package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.response.DashboardResponse;
import id.co.bsi.hello_spring.service.DashboardService;
import id.co.bsi.hello_spring.util.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SecurityUtility securityUtility;

    @GetMapping("/me")
    public ResponseEntity<DashboardResponse> getMyDashboard() {
        DashboardResponse response = dashboardService.getDashboardByToken();
        if ("fail".equals(response.getStatus())) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<String>> getAllUsersFormatted() {
        List<String> users = dashboardService.getAllUsersFormatted();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{accountnum}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable String accountnum) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null || !userId.equals(accountnum)) {
            DashboardResponse response = new DashboardResponse();
            response.setStatus("fail");
            response.setMessage(userId == null ? "Unauthorized access" : "Account number mismatch");
            return ResponseEntity.status(userId == null ? 401 : 403).body(response);
        }

        DashboardResponse response = dashboardService.getDashboardByToken();
        return ResponseEntity.ok(response);
    }
}
