package markoala.fithub.demo.domain.issue.repository;

import markoala.fithub.demo.domain.issue.entity.IssueSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueSyncRepository extends JpaRepository<IssueSync, Long> {
    Optional<IssueSync> findByIssueId(Long issueId);
    List<IssueSync> findByRepositoryId(Long repositoryId);
    List<IssueSync> findByStatus(String status);
    Optional<IssueSync> findByRepositoryIdAndGithubIssueNumber(Long repositoryId, Integer githubIssueNumber);
}
