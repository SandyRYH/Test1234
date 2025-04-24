
package id.co.bsi.hello_spring.controller;

import id.co.bsi.hello_spring.dto.request.TransferRequest;
import id.co.bsi.hello_spring.dto.response.TransferResponse;
import id.co.bsi.hello_spring.service.TransferService;
import id.co.bsi.hello_spring.util.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Autowired
    private SecurityUtility securityUtility;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest transferRequest) {
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            TransferResponse unauthorizedResponse = new TransferResponse();
            unauthorizedResponse.setStatus("error");
            unauthorizedResponse.setMessage("Unauthorized access");
            return new ResponseEntity<>(unauthorizedResponse, HttpStatus.UNAUTHORIZED);
        }

        transferRequest.setFromAccountnum(userId); // Inject accountnum from JWT

        TransferResponse transferResponse = transferService.transfer(transferRequest);
        if ("success".equals(transferResponse.getStatus())) {
            return ResponseEntity.ok(transferResponse);
        } else {
            return new ResponseEntity<>(transferResponse, HttpStatus.BAD_REQUEST);
        }
    }
}

