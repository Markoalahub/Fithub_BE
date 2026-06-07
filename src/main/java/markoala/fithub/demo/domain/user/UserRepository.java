package markoala.fithub.demo.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.aiPipelineGenerationRemainingCount = coalesce(u.aiPipelineGenerationRemainingCount, :defaultLimit) - 1
            where u.id = :userId
              and coalesce(u.aiPipelineGenerationRemainingCount, :defaultLimit) > 0
            """)
    int decreaseAiPipelineGenerationRemainingCount(
            @Param("userId") Long userId,
            @Param("defaultLimit") int defaultLimit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.aiPipelineGenerationRemainingCount =
                case
                    when coalesce(u.aiPipelineGenerationRemainingCount, :defaultLimit) >= :defaultLimit
                    then :defaultLimit
                    else coalesce(u.aiPipelineGenerationRemainingCount, :defaultLimit) + 1
                end
            where u.id = :userId
            """)
    int restoreAiPipelineGenerationRemainingCount(
            @Param("userId") Long userId,
            @Param("defaultLimit") int defaultLimit
    );
}
