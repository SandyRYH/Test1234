package id.co.bsi.hello_spring.service;

import id.co.bsi.hello_spring.dto.response.DashboardResponse;
import id.co.bsi.hello_spring.model.User;
import id.co.bsi.hello_spring.repository.UserRepository;
import id.co.bsi.hello_spring.util.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtility securityUtility;

    public DashboardResponse getDashboardByToken() {
        DashboardResponse response = new DashboardResponse();
        String userId = securityUtility.getCurrentUserId();
        if (userId == null) {
            response.setStatus("fail");
            response.setMessage("Unauthorized access");
            return response;
        }

        var userOpt = userRepository.findByAccountnum(userId);
        if (userOpt.isEmpty()) {
            response.setStatus("fail");
            response.setMessage("User not found");
            return response;
        }

        User user = userOpt.get();

        DashboardResponse.DashboardData data = new DashboardResponse.DashboardData();
        data.setAccountnum(user.getAccountnum());
        data.setFullname(user.getFullName());
        data.setBalance(user.getBalance());

        response.setStatus("success");
        response.setMessage("Dashboard successfully displayed");
        response.setData(data);
        return response;
    }

    public List<String> getAllUsersFormatted() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> user.getAccountnum() + " - " + user.getFullName())
                .collect(Collectors.toList());
    }
}
