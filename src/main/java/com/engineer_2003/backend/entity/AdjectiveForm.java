package com.engineer_2003.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "adjective_forms")
public class AdjectiveForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "mizen_form")
    private String mizenForm;

    @Column(name = "negative_form")
    private String negativeForm;

    @Column(name = "te_form")
    private String teForm;

    @Column(name = "past_form")
    private String pastForm;

    @Column(name = "conditional_form")
    private String conditionalForm;

    @Column(name = "polite_form")
    private String politeForm;

    // 추가 활용형들
    @Column(name = "adverbial_form")
    private String adverbialForm; // 부사형

    @Column(name = "noun_form")
    private String nounForm; // 명사형

    // 기본 생성자
    public AdjectiveForm() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Word getWord() { return word; }
    public void setWord(Word word) { this.word = word; }

    public String getMizenForm() { return mizenForm; }
    public void setMizenForm(String mizenForm) { this.mizenForm = mizenForm; }

    public String getNegativeForm() { return negativeForm; }
    public void setNegativeForm(String negativeForm) { this.negativeForm = negativeForm; }

    public String getTeForm() { return teForm; }
    public void setTeForm(String teForm) { this.teForm = teForm; }

    public String getPastForm() { return pastForm; }
    public void setPastForm(String pastForm) { this.pastForm = pastForm; }

    public String getConditionalForm() { return conditionalForm; }
    public void setConditionalForm(String conditionalForm) { this.conditionalForm = conditionalForm; }

    public String getPoliteForm() { return politeForm; }
    public void setPoliteForm(String politeForm) { this.politeForm = politeForm; }

    public String getAdverbialForm() { return adverbialForm; }
    public void setAdverbialForm(String adverbialForm) { this.adverbialForm = adverbialForm; }

    public String getNounForm() { return nounForm; }
    public void setNounForm(String nounForm) { this.nounForm = nounForm; }

    @Override
    public String toString() {
        return "AdjectiveForm{" +
                "id=" + id +
                ", negativeForm='" + negativeForm + '\'' +
                ", teForm='" + teForm + '\'' +
                ", pastForm='" + pastForm + '\'' +
                ", politeForm='" + politeForm + '\'' +
                '}';
    }
}