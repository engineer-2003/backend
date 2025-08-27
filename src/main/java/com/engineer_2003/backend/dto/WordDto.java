package com.engineer_2003.backend.dto;

import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.entity.AdjectiveForm;
import com.engineer_2003.backend.entity.VerbForm;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDto {
    private Long id;
    private String levelName;
    private String partOfSpeech;
    private String word;
    private String reading;
    private String meaning;
    private String exampleSentence;
    private String exampleTranslation;
    private Map<String, String> forms;

    public WordDto() {
        this.forms = new HashMap<>();
    }

    public WordDto(Word word) {
        this.id = word.getId();
        this.levelName = word.getLevelName();
        this.partOfSpeech = word.getPartOfSpeech();
        this.word = word.getWord();
        this.reading = word.getReading();
        this.meaning = word.getMeaning();
        this.exampleSentence = word.getExampleSentence();
        this.exampleTranslation = word.getExampleTranslation();
        this.forms = new HashMap<>();

        // 형용사 활용형 추가 (null 체크 강화)
        if (word.getAdjectiveForms() != null && !word.getAdjectiveForms().isEmpty()) {
            AdjectiveForm adjForm = word.getAdjectiveForms().iterator().next();
            if (adjForm != null) {
                addFormIfNotEmpty("미연형", adjForm.getMizenForm());
                addFormIfNotEmpty("부정형", adjForm.getNegativeForm());
                addFormIfNotEmpty("て형", adjForm.getTeForm());
                addFormIfNotEmpty("과거형", adjForm.getPastForm());
                addFormIfNotEmpty("가정형", adjForm.getConditionalForm());
                addFormIfNotEmpty("정중형", adjForm.getPoliteForm());
            }
        }

        // 동사 활용형 추가 (null 체크 강화)
        if (word.getVerbForms() != null && !word.getVerbForms().isEmpty()) {
            VerbForm verbForm = word.getVerbForms().iterator().next();
            if (verbForm != null) {
                addFormIfNotEmpty("ます형", verbForm.getMasuForm());
                addFormIfNotEmpty("부정형", verbForm.getNegativeForm());
                addFormIfNotEmpty("청유형", verbForm.getVolitionalForm());
                addFormIfNotEmpty("가정형", verbForm.getConditionalForm());
                addFormIfNotEmpty("명령형", verbForm.getImperativeForm());
                addFormIfNotEmpty("가능형", verbForm.getPotentialForm());
                addFormIfNotEmpty("て형", verbForm.getTeForm());
                addFormIfNotEmpty("た형", verbForm.getTaForm());
                addFormIfNotEmpty("과거형", verbForm.getPastForm());
            }
        }
    }

    /**
     * 비어있지 않은 활용형만 추가
     */
    private void addFormIfNotEmpty(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            this.forms.put(key, value.trim());
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getReading() { return reading; }
    public void setReading(String reading) { this.reading = reading; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

    public String getExampleTranslation() { return exampleTranslation; }
    public void setExampleTranslation(String exampleTranslation) { this.exampleTranslation = exampleTranslation; }

    public Map<String, String> getForms() { return forms; }
    public void setForms(Map<String, String> forms) { this.forms = forms; }

    @Override
    public String toString() {
        return "WordDto{" +
                "id=" + id +
                ", word='" + word + '\'' +
                ", reading='" + reading + '\'' +
                ", meaning='" + meaning + '\'' +
                ", forms=" + forms +
                '}';
    }
}