package com.engineer_2003.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "verb_forms")
public class VerbForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "masu_form")
    private String masuForm;

    @Column(name = "negative_form")
    private String negativeForm;

    @Column(name = "volitional_form")
    private String volitionalForm;

    @Column(name = "conditional_form")
    private String conditionalForm;

    @Column(name = "imperative_form")
    private String imperativeForm;

    @Column(name = "potential_form")
    private String potentialForm;

    // 기본 생성자
    public VerbForm() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Word getWord() { return word; }
    public void setWord(Word word) { this.word = word; }

    public String getMasuForm() { return masuForm; }
    public void setMasuForm(String masuForm) { this.masuForm = masuForm; }

    public String getNegativeForm() { return negativeForm; }
    public void setNegativeForm(String negativeForm) { this.negativeForm = negativeForm; }

    public String getVolitionalForm() { return volitionalForm; }
    public void setVolitionalForm(String volitionalForm) { this.volitionalForm = volitionalForm; }

    public String getConditionalForm() { return conditionalForm; }
    public void setConditionalForm(String conditionalForm) { this.conditionalForm = conditionalForm; }

    public String getImperativeForm() { return imperativeForm; }
    public void setImperativeForm(String imperativeForm) { this.imperativeForm = imperativeForm; }

    public String getPotentialForm() { return potentialForm; }
    public void setPotentialForm(String potentialForm) { this.potentialForm = potentialForm; }
}