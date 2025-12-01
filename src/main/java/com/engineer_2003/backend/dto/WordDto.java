package com.engineer_2003.backend.dto;

import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.entity.AdjectiveForm;
import com.engineer_2003.backend.entity.VerbForm;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.LinkedHashMap;

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
        this.forms = new LinkedHashMap<>();
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
        this.forms = new LinkedHashMap<>();

        // 형용사 활용형
        if (word.getAdjectiveForms() != null && !word.getAdjectiveForms().isEmpty()) {
            AdjectiveForm adjForm = word.getAdjectiveForms().iterator().next();
            if (adjForm != null) {
                addFormIfNotEmpty("mizenForm", adjForm.getMizenForm());         // 1. 미연형
                addFormIfNotEmpty("negativeForm", adjForm.getNegativeForm());   // 2. 부정형
                addFormIfNotEmpty("teForm", adjForm.getTeForm());               // 3. て형
                addFormIfNotEmpty("pastForm", adjForm.getPastForm());           // 4. 과거형
                addFormIfNotEmpty("conditionalForm", adjForm.getConditionalForm()); // 5. 가정형
                addFormIfNotEmpty("politeForm", adjForm.getPoliteForm());       // 6. 정중형
            }
        }

        // 동사 활용형
        if (word.getVerbForms() != null && !word.getVerbForms().isEmpty()) {
            VerbForm verbForm = word.getVerbForms().iterator().next();
            if (verbForm != null) {
                addFormIfNotEmpty("masuForm", verbForm.getMasuForm());          // 1. ます형
                addFormIfNotEmpty("negativeForm", verbForm.getNegativeForm());  // 2. 부정형
                addFormIfNotEmpty("volitionalForm", verbForm.getVolitionalForm());// 3. 청유형
                addFormIfNotEmpty("conditionalForm", verbForm.getConditionalForm());// 4. 가정형
                addFormIfNotEmpty("imperativeForm", verbForm.getImperativeForm());// 5. 명령형
                addFormIfNotEmpty("potentialForm", verbForm.getPotentialForm());// 6. 가능형
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