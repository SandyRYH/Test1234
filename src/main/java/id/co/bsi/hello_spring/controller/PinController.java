package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.service.PinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pin")
public class PinController {

    @Autowired
    private PinService pinService;

    @PostMapping
    public ResponseEntity<?> registerPin(@RequestBody Map<String, String> payload) {
        String accountnum = payload.get("accountnum");
        String pin = payload.get("pin");

        // Validasi input tidak boleh null atau kosong
        if (accountnum == null || pin == null || pin.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "accountnum and pin must be provided."));
        }

        // Validasi PIN harus 6 digit angka
        if (!pin.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "PIN must be exactly 6 digits."));
        }

        // Validasi accountnum ada di DB
        boolean accountExists = pinService.checkAccountExists(accountnum);
        if (!accountExists) {
            return ResponseEntity.status(404).body(Map.of("message", "Account number not found."));
        }

        String result = pinService.registerPin(accountnum, pin);

        if ("PIN already registered.".equals(result)) {
            return ResponseEntity.status(409).body(Map.of("message", result));
        }

        return ResponseEntity.ok(Map.of("message", result));
    }
}
