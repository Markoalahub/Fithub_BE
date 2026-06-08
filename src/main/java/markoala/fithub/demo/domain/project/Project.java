package markoala.fithub.demo.domain.project;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "creator_nickname")
    private String creatorNickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    

    public Project(Long id, String name, String description, Long creatorId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, description, creatorId, null, createdAt, updatedAt);
    }

    public Project(Long id, String name, String description, Long creatorId, String creatorNickname, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public static Project createProject(String name, String description, Long creatorId) {
        return createProject(name, description, creatorId, null);
    }

    public static Project createProject(String name, String description, Long creatorId, String creatorNickname) {
        return new Project(null, name, description, creatorId, creatorNickname, null, null);
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}
