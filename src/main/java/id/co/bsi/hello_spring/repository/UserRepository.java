
package id.co.bsi.hello_spring.repository;

import id.co.bsi.hello_spring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAccountnum(String accountnum);
    Optional<User> findByPhone(String phone);

}
