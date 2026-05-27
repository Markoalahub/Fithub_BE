package markoala.fithub.demo.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
    Optional<User> findByEmail(String email);
    Optional<User> findBySocialLoginId(String socialLoginId);
    boolean existsByEmail(String email);
}
