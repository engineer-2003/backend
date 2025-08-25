package io.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String level; // N1, N2, N3, N4, N5

    @Column(nullable = false, length = 20)
    private String wordType; // 명사, 형용사, 형용동사, 동사

    @Column(nullable = false, length = 100)
    private String word; // 일본어 단어

    @Column(length = 100)
    private String furigana; // 후리가나

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning; // 한국어 뜻

    @Column(columnDefinition = "TEXT")
    private String example; // 예문

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}