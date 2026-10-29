package com.feedback;

import java.sql.Timestamp;

/**
 * Feedback: a simple model class (POJO).
 * One object = one row of the 'feedback' table.
 */
public class Feedback {

    private int id;
    private String studentName;
    private String faculty;
    private int rating;
    private String comments;
    private Timestamp createdAt;

    public Feedback() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /** Converts the stored number (1-5) into the text shown to the user. */
    public String getRatingText() {
        switch (rating) {
            case 1: return "Very Bad";
            case 2: return "Bad";
            case 3: return "Good";
            case 4: return "Very Good";
            case 5: return "Excellent";
            default: return "Unknown";
        }
    }
}