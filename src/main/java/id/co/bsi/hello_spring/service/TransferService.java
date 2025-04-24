
package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.dto.request.TransferRequest;
import id.co.bsi.hello_spring.dto.response.TransferResponse;
import id.co.bsi.hello_spring.model.TransactionModel;
import id.co.bsi.hello_spring.model.User;
import id.co.bsi.hello_spring.model.UserPin;
import id.co.bsi.hello_spring.repository.UserRepository;
import id.co.bsi.hello_spring.repository.UserPinRepository;
import id.co.bsi.hello_spring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Service
public class TransferService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserPinRepository userPinRepository;

    public TransferResponse transfer(TransferRequest request) {
        TransferResponse res = new TransferResponse();

        Optional<User> fromUserOpt = userRepository.findByAccountnum(request.getFromAccountnum());
        Optional<User> toUserOpt = userRepository.findByAccountnum(request.getToAccountnum());

        if (!fromUserOpt.isPresent() || !toUserOpt.isPresent()) {
            res.setStatus("error");
            res.setMessage("User not found");
            return res;
        }

        User fromUser = fromUserOpt.get();
        User toUser = toUserOpt.get();

        // Validasi PIN
        Optional<UserPin> pinOpt = userPinRepository.findByAccountnum(fromUser.getAccountnum());
        if (!pinOpt.isPresent()) {
            res.setStatus("error");
            res.setMessage("PIN not registered");
            return res;
        }

        UserPin userPin = pinOpt.get();
        String hashedPin = hashPin(request.getPin());

        if (!userPin.getPinHash().equals(hashedPin)) {
            res.setStatus("error");
            res.setMessage("Invalid PIN");
            return res;
        }

        // Validasi: tidak boleh transfer ke diri sendiri
        if (fromUser.getAccountnum().equals(toUser.getAccountnum())) {
            res.setStatus("error");
            res.setMessage("Cannot transfer to the same account");
            return res;
        }

        // Validasi: transfer tidak boleh 0 atau negatif
        if (request.getAmount() <= 0) {
            res.setStatus("error");
            res.setMessage("Transfer amount must be greater than zero");
            return res;
        }

        // Validasi: saldo harus cukup
        if (fromUser.getBalance() < request.getAmount()) {
            res.setStatus("error");
            res.setMessage("Insufficient balance");
            return res;
        }

        // Proses transfer: update saldo
        fromUser.setBalance(fromUser.getBalance() - request.getAmount());
        toUser.setBalance(toUser.getBalance() + request.getAmount());

        userRepository.save(fromUser);
        userRepository.save(toUser);

        TransactionModel debitTxn = new TransactionModel();
        debitTxn.setAccountnum(fromUser.getAccountnum());
        debitTxn.setAmount(request.getAmount());
        debitTxn.setType("expense");
        debitTxn.setFromTo("Transfer to " + toUser.getFullName());
        debitTxn.setDescription("Transfer to account " + toUser.getAccountnum());
        debitTxn.setDateTime(java.time.LocalDateTime.now().toString());
        transactionRepository.save(debitTxn);

        TransactionModel creditTxn = new TransactionModel();
        creditTxn.setAccountnum(toUser.getAccountnum());
        creditTxn.setAmount(request.getAmount());
        creditTxn.setType("income");
        creditTxn.setFromTo("Received from " + fromUser.getFullName());
        creditTxn.setDescription("Transfer from account " + fromUser.getAccountnum());
        creditTxn.setDateTime(java.time.LocalDateTime.now().toString());
        transactionRepository.save(creditTxn);

        res.setStatus("success");
        res.setMessage("Transfer completed");
        res.setFromName(fromUser.getFullName());
        res.setFromAccountnum(fromUser.getAccountnum());
        res.setToName(toUser.getFullName());
        res.setToAccountnum(toUser.getAccountnum());
        res.setAmount(request.getAmount());

        return res;
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }
}


