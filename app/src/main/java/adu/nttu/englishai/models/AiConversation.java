package adu.nttu.englishai.models;

public class AiConversation {

    private String id;
    private String title;
    private String topicCode;
    private String topicName;
    private long createdAt;
    private long updatedAt;

    public AiConversation() {
        // Firestore cần constructor rỗng
    }

    public AiConversation(
            String id,
            String title,
            String topicCode,
            String topicName,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.topicCode = topicCode;
        this.topicName = topicName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}