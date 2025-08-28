// VerbForm.java (수정된 버전)
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

    // 추가된 활용형들
    @Column(name = "te_form")
    private String teForm;

    @Column(name = "ta_form")
    private String taForm;

    @Column(name = "past_form")
    private String pastForm;

    @Column(name = "present_form")
    private String presentForm;

    @Column(name = "causative_form")
    private String causativeForm;

    @Column(name = "passive_form")
    private String passiveForm;

    // 기본 생성자
    public VerbForm() {}

    // 전체 생성자
    public VerbForm(Word word, String masuForm, String negativeForm, String volitionalForm,
                    String conditionalForm, String imperativeForm, String potentialForm,
                    String teForm, String taForm, String pastForm) {
        this.word = word;
        this.masuForm = masuForm;
        this.negativeForm = negativeForm;
        this.volitionalForm = volitionalForm;
        this.conditionalForm = conditionalForm;
        this.imperativeForm = imperativeForm;
        this.potentialForm = potentialForm;
        this.teForm = teForm;
        this.taForm = taForm;
        this.pastForm = pastForm;
    }

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

    // 새로 추가된 활용형들의 Getter/Setter
    public String getTeForm() { return teForm; }
    public void setTeForm(String teForm) { this.teForm = teForm; }

    public String getTaForm() { return taForm; }
    public void setTaForm(String taForm) { this.taForm = taForm; }

    public String getPastForm() { return pastForm; }
    public void setPastForm(String pastForm) { this.pastForm = pastForm; }

    public String getPresentForm() { return presentForm; }
    public void setPresentForm(String presentForm) { this.presentForm = presentForm; }

    public String getCausativeForm() { return causativeForm; }
    public void setCausativeForm(String causativeForm) { this.causativeForm = causativeForm; }

    public String getPassiveForm() { return passiveForm; }
    public void setPassiveForm(String passiveForm) { this.passiveForm = passiveForm; }

    @Override
    public String toString() {
        return "VerbForm{" +
                "id=" + id +
                ", masuForm='" + masuForm + '\'' +
                ", negativeForm='" + negativeForm + '\'' +
                ", teForm='" + teForm + '\'' +
                ", taForm='" + taForm + '\'' +
                ", pastForm='" + pastForm + '\'' +
                '}';
    }
}