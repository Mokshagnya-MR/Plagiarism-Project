package com.example.plagiarism;

import java.util.Objects;

public class Document {
    private final String title;
    private final String author;
    private final String submissionDate; // ISO-8601 date string
    private final String text;
    private double plagiarismScore; // 0.0 - 1.0

    public Document(String title, String author, String submissionDate, String text) {
        this.title = title == null ? "" : title;
        this.author = author == null ? "" : author;
        this.submissionDate = submissionDate == null ? "" : submissionDate;
        this.text = text == null ? "" : text;
        this.plagiarismScore = 0.0;
    }

    public String extractText() {
        return text;
    }

    public double calculateSimilarity(Document other, SimilarityAlgorithm algorithm) {
        if (other == null || algorithm == null) {
            return 0.0;
        }
        return PlagiarismChecker.computeSimilarity(this, other, algorithm);
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public String getText() {
        return text;
    }

    public double getPlagiarismScore() {
        return plagiarismScore;
    }

    public void setPlagiarismScore(double plagiarismScore) {
        this.plagiarismScore = plagiarismScore;
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

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", submissionDate='" + submissionDate + '\'' +
                ", plagiarismScore=" + plagiarismScore +
                '}';
    }
}
