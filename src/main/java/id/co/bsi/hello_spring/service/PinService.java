
package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.model.UserPin;
import id.co.bsi.hello_spring.repository.UserPinRepository;
import id.co.bsi.hello_spring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Service
public class PinService {

    @Autowired
    private UserPinRepository userPinRepository;

    @Autowired
    private UserRepository userRepository;

    public String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash PIN", e);
        }
    }

    public String registerPin(String accountnum, String pin) {
        Optional<UserPin> existing = userPinRepository.findByAccountnum(accountnum);
        if (existing.isPresent()) {
            return "PIN already registered.";
        }
        UserPin userPin = new UserPin();
        userPin.setAccountnum(accountnum);
        userPin.setPinHash(hashPin(pin));
        userPinRepository.save(userPin);
        return "PIN registered successfully.";
    }

    public boolean checkAccountExists(String accountnum) {
        return userRepository.findByAccountnum(accountnum).isPresent();
    }
}

