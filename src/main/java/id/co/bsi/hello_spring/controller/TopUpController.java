package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.request.TopUpRequest;
import id.co.bsi.hello_spring.dto.response.TopUpResponse;
import id.co.bsi.hello_spring.model.TransactionModel;
import id.co.bsi.hello_spring.model.User;
import id.co.bsi.hello_spring.repository.UserPinRepository;
import id.co.bsi.hello_spring.repository.UserRepository;
import id.co.bsi.hello_spring.service.TransactionService;
import id.co.bsi.hello_spring.util.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class TopUpController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPinRepository userPinRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SecurityUtility securityUtility;

    @PostMapping("/api/topup")
    public ResponseEntity<TopUpResponse> topup (@RequestBody TopUpRequest topUpRequest) {
        TopUpResponse response = new TopUpResponse();

        String userId = securityUtility.getCurrentUserId();
        if (userId == null || !userId.equals(topUpRequest.getAccountnum())) {
            response.setStatus("fail");
            response.setMessage("Unauthorized access.");
            return ResponseEntity.status(401).body(response);
        }

        Optional<User> userOpt = userRepository.findByAccountnum(userId);
        if (userOpt.isEmpty()) {
            response.setStatus("fail");
            response.setMessage("Account not found.");
            return ResponseEntity.status(404).body(response);
        }

        if (topUpRequest.getMethod() == null || topUpRequest.getMethod().trim().isEmpty()) {
            response.setStatus("fail");
            response.setMessage("Topup method must be provided.");
            return ResponseEntity.status(400).body(response);
        }

        User user = userOpt.get();

        if (topUpRequest.getCardNumber() == null || !topUpRequest.getCardNumber().matches("\\d{16}")) {
            response.setStatus("fail");
            response.setMessage("Invalid card number. Must be 16 digits.");
            return ResponseEntity.status(400).body(response);
        }
        if (topUpRequest.getCvv() == null || !topUpRequest.getCvv().matches("\\d{3}")) {
            response.setStatus("fail");
            response.setMessage("Invalid CVV. Must be 3 digits.");
            return ResponseEntity.status(400).body(response);
        }
        if (topUpRequest.getExpirationDate() == null || !topUpRequest.getExpirationDate().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            response.setStatus("fail");
            response.setMessage("Invalid expiration date format. Must be MM/YY.");
            return ResponseEntity.status(400).body(response);
        }

        String[] expParts = topUpRequest.getExpirationDate().split("/");
        int expMonth = Integer.parseInt(expParts[0]);
        int expYear = Integer.parseInt("20" + expParts[1]);

        java.time.YearMonth expDate = java.time.YearMonth.of(expYear, expMonth);
        java.time.YearMonth now = java.time.YearMonth.now();
        if (expDate.isBefore(now)) {
            response.setStatus("fail");
            response.setMessage("Card has expired.");
            return ResponseEntity.status(400).body(response);
        }

        // Validasi PIN
//        Optional<UserPin> userPinOpt = userPinRepository.findByAccountnum(user.getAccountnum());
//        if (userPinOpt.isEmpty()) {
//            response.setStatus("fail");
//            response.setMessage("PIN not registered.");
//            return ResponseEntity.status(401).body(response);
//        }
//
//        try {
//            String hashedPin = Base64.getEncoder().encodeToString(
//                    MessageDigest.getInstance("SHA-256").digest(topUpRequest.getPin().getBytes())
//            );
//            if (!userPinOpt.get().getPinHash().equals(hashedPin)) {
//                response.setStatus("fail");
//                response.setMessage("Invalid PIN");
//                return ResponseEntity.status(401).body(response);
//            }
//        } catch (Exception e) {
//            response.setStatus("fail");
//            response.setMessage("PIN validation error.");
//            return ResponseEntity.status(500).body(response);
//        }

        user.setBalance(user.getBalance() + topUpRequest.getAmount());
        userRepository.save(user);
        
        TransactionModel txn = new TransactionModel();
        txn.setAccountnum(user.getAccountnum());
        txn.setAmount(topUpRequest.getAmount());
        txn.setType("income");
        txn.setFromTo("TopUp via " + topUpRequest.getMethod());
        txn.setDescription("Top up successful");
        txn.setDateTime(java.time.LocalDateTime.now().toString());

        transactionService.saveTransaction(txn);

        response.setStatus("success");
        response.setMessage("Top Up Success");

        TopUpResponse.OptionTopUp data = new TopUpResponse.OptionTopUp();
        data.setAccountnum(user.getAccountnum());
        data.setBalance(user.getBalance());
        response.setData(data);

        return ResponseEntity.ok(response);
    }

}
