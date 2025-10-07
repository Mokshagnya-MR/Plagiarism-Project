package com.example.plagiarism;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Document {
    private String title;
    private String author;
    private String submissionDate; // ISO date (yyyy-MM-dd)
    private String text;
    private double plagiarismScore; // 0.0 .. 1.0

    public Document() {
    }

    public Document(String title, String author, String submissionDate, String text) {
        this.title = title;
        this.author = author;
        this.submissionDate = submissionDate;
        this.text = text;
    }

    public static Document ofToday(String title, String author, String text) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return new Document(title, author, today, text);
    }

    public String extractText() {
        return text == null ? "" : text;
    }

    public double calculateSimilarity(Document other) {
        PlagiarismChecker checker = new PlagiarismChecker();
        return checker.calculateSimilarity(this, other, PlagiarismChecker.Metric.COSINE);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getPlagiarismScore() {
        return plagiarismScore;
    }

    public void setPlagiarismScore(double plagiarismScore) {
        this.plagiarismScore = plagiarismScore;
    }

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", submissionDate='" + submissionDate + '\'' +
                ", plagiarismScore=" + plagiarismScore +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(title, document.title) &&
                Objects.equals(author, document.author) &&
                Objects.equals(submissionDate, document.submissionDate) &&
                Objects.equals(text, document.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, submissionDate, text);
    }
}
