package markoala.fithub.demo.issue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<GithubRepository, Long> {
    List<GithubRepository> findByProjectId(Long projectId);
    Optional<GithubRepository> findByRepoUrl(String repoUrl);
}
