package adu.nttu.englishai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata của một bộ đề TOEIC.
 *
 * Nội dung câu hỏi được lưu riêng trong collection toeicQuestions.
 */
public class ToeicTest implements Serializable {

    private String id;

    private String title;
    private String sourceName;
    private String sourceReference;

    private int year;
    private int totalQuestions;
    private int durationMinutes;

    private boolean hasListening;
    private boolean hasReading;

    private List<Integer> availableParts;

    public ToeicTest() {
        availableParts = new ArrayList<>();
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isHasListening() {
        return hasListening;
    }

    public void setHasListening(boolean hasListening) {
        this.hasListening = hasListening;
    }

    public boolean isHasReading() {
        return hasReading;
    }

    public void setHasReading(boolean hasReading) {
        this.hasReading = hasReading;
    }

    public List<Integer> getAvailableParts() {
        if (availableParts == null) {
            availableParts = new ArrayList<>();
        }
        return availableParts;
    }

    public void setAvailableParts(List<Integer> availableParts) {
        this.availableParts =
                availableParts == null
                        ? new ArrayList<>()
                        : availableParts;
    }
}
