package markoala.fithub.demo.issue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByRepositoryId(Long repositoryId);
    Optional<Issue> findByRepositoryIdAndGithubIssueNumber(Long repositoryId, Integer githubIssueNumber);
    List<Issue> findByPipelineStepId(Integer pipelineStepId);
}
