package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.response.TransactionPageResponse;
import id.co.bsi.hello_spring.model.TransactionModel;
import id.co.bsi.hello_spring.service.TransactionService;
import id.co.bsi.hello_spring.util.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SecurityUtility securityUtility;

    @GetMapping("/me")
    public ResponseEntity<TransactionPageResponse> getMyTransactions(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd
    ) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            TransactionPageResponse failRes = new TransactionPageResponse();
            failRes.setStatus("fail");
            failRes.setMessage("Unauthorized access");
            failRes.setData(null);
            return ResponseEntity.status(401).body(failRes);
        }

        Page<TransactionModel> resultPage = transactionService.getFilteredTransactionsCombined(
                userId, search, page, size, sortBy, direction, dateStart, dateEnd
        );

        TransactionPageResponse.DataContent content = new TransactionPageResponse.DataContent();
        content.setTransactions(resultPage.getContent());
        content.setTotalPages(resultPage.getTotalPages());
        content.setCurrentPage(resultPage.getNumber());
        content.setTotalElements(resultPage.getTotalElements());

        TransactionPageResponse res = new TransactionPageResponse();
        res.setStatus("success");
        res.setMessage("List transaksi");
        res.setData(content);

        return ResponseEntity.ok(res);
    }


    @GetMapping("/summary/monthly_chart")
    public ResponseEntity<?> getMonthlyChart() {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "fail",
                    "message", "Unauthorized access",
                    "data", null
            ));
        }

        Map<String, Object> chartData = transactionService.getMonthlyChartSummary(userId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Monthly chart data",
                "data", chartData
        ));
    }


    @PostMapping
    public ResponseEntity<?> postTransaction(@RequestBody TransactionModel transaction) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null || !userId.equals(transaction.getAccountnum())) {
            return ResponseEntity.status(401).body("Unauthorized or account mismatch");
        }

        // Validasi dan format dateTime
        try {
            if (transaction.getDateTime() == null || transaction.getDateTime().trim().isEmpty()) {
                // Auto sekarang jika kosong
                transaction.setDateTime(java.time.LocalDateTime.now().toString());
            } else {
                // Parse dateTime dari request
                LocalDateTime inputDateTime;

                if (transaction.getDateTime().length() == 10) { // format YYYY-MM-DD
                    inputDateTime = LocalDate.parse(transaction.getDateTime()).atStartOfDay();
                } else {
                    inputDateTime = LocalDateTime.parse(transaction.getDateTime()); // ISO 8601
                }

                // Validasi tidak boleh melebihi sekarang
                if (inputDateTime.isAfter(LocalDateTime.now())) {
                    return ResponseEntity.badRequest().body("dateTime cannot be in the future.");
                }

                // Set format yang pasti panjang (pakai toString ISO full)
                transaction.setDateTime(inputDateTime.toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid dateTime format.");
        }

        TransactionModel saved = transactionService.saveTransaction(transaction);
        return ResponseEntity.ok(saved);
    }



    @GetMapping("/summary")
    public ResponseEntity<?> getTransactionSummary() {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "fail",
                    "message", "Unauthorized access",
                    "data", null
            ));
        }

        Map<String, Object> summary = transactionService.getTransactionSummary(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Transaction summary",
                "data", summary
        ));
    }


    @GetMapping("/summary/this_month")
    public ResponseEntity<?> getSummaryThisMonth() {
        return getSummaryByMonthRange(0);
    }

    @GetMapping("/summary/last_month")
    public ResponseEntity<?> getSummaryLastMonth() {
        return getSummaryByMonthRange(1);
    }

    @GetMapping("/summary/three_month_ago")
    public ResponseEntity<?> getSummaryThreeMonthsAgo() {
        return getSummaryByMonthRange(3);
    }

    private ResponseEntity<?> getSummaryByMonthRange(int monthsAgo) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "fail",
                    "message", "Unauthorized access",
                    "data", null
            ));
        }

        Map<String, Object> summary = transactionService.getTransactionSummaryByMonth(userId, monthsAgo);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Transaction summary for " + (monthsAgo == 0 ? "this month" : (monthsAgo == 1 ? "last month" : "three months ago")),
                "data", summary
        ));
    }

    @GetMapping("/summary/donut/this_month")
    public ResponseEntity<?> getDonutThisMonth() {
        return getDonutSummaryByMonthRange(0);
    }

    @GetMapping("/summary/donut/last_month")
    public ResponseEntity<?> getDonutLastMonth() {
        return getDonutSummaryByMonthRange(1);
    }

    @GetMapping("/summary/donut/three_month_ago")
    public ResponseEntity<?> getDonutThreeMonthAgo() {
        return getDonutSummaryByMonthRange(3);
    }

    private ResponseEntity<?> getDonutSummaryByMonthRange(int monthsAgo) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "fail",
                    "message", "Unauthorized access",
                    "data", null
            ));
        }

        Map<String, Object> donutData = transactionService.getDonutChartSummary(userId, monthsAgo);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Donut chart summary",
                "data", donutData
        ));
    }



}
