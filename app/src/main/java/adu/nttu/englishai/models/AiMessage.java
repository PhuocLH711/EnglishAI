package adu.nttu.englishai.models;

public class AiMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "model";

    private String id;
    private String role;
    private String content;
    private long createdAt;

    public AiMessage() {
        // Constructor rỗng cho Firestore
    }

    public AiMessage(
            String id,
            String role,
            String content,
            long createdAt
    ) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}