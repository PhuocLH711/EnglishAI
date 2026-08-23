package adu.nttu.englishai.models;

public class SentenceRewriteExercise {

    private String id;
    private String originalSentence;
    private String prompt;
    private String answer;
    private String explanation;
    private String topic;
    private String level;
    private long xp;

    public SentenceRewriteExercise() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalSentence() { return originalSentence; }
    public void setOriginalSentence(String originalSentence) { this.originalSentence = originalSentence; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }
}
