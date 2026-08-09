package adu.nttu.englishai.models;

public class AiMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "model";

    private String id;
    private String role;
    private String content;
    private long createdAt;

    // Ảnh người dùng gửi
    private String imageUri;

    public AiMessage() {
        // Constructor rỗng cho Firestore
    }

    public AiMessage(
            String id,
            String role,
            String content,
            long createdAt
    ) {
        this(
                id,
                role,
                content,
                createdAt,
                null
        );
    }

    public AiMessage(
            String id,
            String role,
            String content,
            long createdAt,
            String imageUri
    ) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.imageUri = imageUri;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }

    public boolean hasImage() {
        return imageUri != null
                && !imageUri.trim().isEmpty();
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
        return content == null ? "" : content;
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

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}