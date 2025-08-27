package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Column(name = "part_of_speech", nullable = false)
    private String partOfSpeech;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String reading;

    @Column(nullable = false)
    private String meaning;

    @Column(name = "example_sentence")
    private String exampleSentence;

    @Column(name = "example_translation")
    private String exampleTranslation;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AdjectiveForm> adjectiveForms = new HashSet<>();

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VerbForm> verbForms = new HashSet<>();

    // 급수, 품사, 단어, 읽기, 의미, 예문, 번역만 포함하는 생성자
    public Word(String levelName, String partOfSpeech, String word, String reading, String meaning, String exampleSentence, String exampleTranslation) {
        this.levelName = levelName;
        this.partOfSpeech = partOfSpeech;
        this.word = word;
        this.reading = reading;
        this.meaning = meaning;
        this.exampleSentence = exampleSentence;
        this.exampleTranslation = exampleTranslation;
        this.createdAt = LocalDateTime.now();
    }
}
