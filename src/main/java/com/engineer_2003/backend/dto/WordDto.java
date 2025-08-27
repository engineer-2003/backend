package com.engineer_2003.backend.dto;

import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.entity.AdjectiveForm;
import com.engineer_2003.backend.entity.VerbForm;
import java.util.Map;
import java.util.HashMap;

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

        // 형용사 활용형 추가
        if (word.getAdjectiveForms() != null && !word.getAdjectiveForms().isEmpty()) {
            AdjectiveForm adjForm = word.getAdjectiveForms().iterator().next();
            forms.put("미연형", adjForm.getMizenForm());
            forms.put("부정형", adjForm.getNegativeForm());
            forms.put("て형", adjForm.getTeForm());
            forms.put("과거형", adjForm.getPastForm());
            forms.put("가정형", adjForm.getConditionalForm());
            forms.put("정중형", adjForm.getPoliteForm());
        }


        // 동사 활용형 추가
        if (word.getVerbForms() != null && !word.getVerbForms().isEmpty()) {
            VerbForm verbForm = word.getVerbForms().iterator().next();
            forms.put("ます형", verbForm.getMasuForm());
            forms.put("부정형", verbForm.getNegativeForm());
            forms.put("청유형", verbForm.getVolitionalForm());
            forms.put("가정형", verbForm.getConditionalForm());
            forms.put("명령형", verbForm.getImperativeForm());
            forms.put("가능형", verbForm.getPotentialForm());
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
}