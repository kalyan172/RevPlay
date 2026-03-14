package com.revplay.musicplatform.playlist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "user_likes", uniqueConstraints = @UniqueConstraint(name = "uk_user_like", columnNames = { "user_id",
        "likeable_id", "likeable_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "likeable_id", nullable = false)
    private Long likeableId;


    @Column(name = "likeable_type", nullable = false, length = 20)
    private String likeableType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;
}
