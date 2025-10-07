# Plagiarism Detection System (Java + Swing + Blockchain)

## Overview
A desktop application that compares two texts using Cosine Similarity and Jaccard Index, presents a verdict, and stores each check on a simple blockchain saved locally for tamper-evident history.

## Requirements
- Java 11+ (tested with OpenJDK 21)
- Linux/macOS/Windows

## Build
```bash
./scripts/build.sh
```
Produces `dist/plagiarism-detector.jar`.

## Run
```bash
java -jar dist/plagiarism-detector.jar
```

## Features
- Preprocessing: lowercase, remove punctuation/numbers, stopwords, duplicate words (for Jaccard via set).
- Similarity: Cosine (term frequency vectors) and Jaccard (set of unique tokens).
- Verdict: Safe (<30%), Moderate (30–70%), High (>70%).
- Blockchain: Each check becomes a block with score and metadata, validated with SHA-256 linking.
- Storage: Saved as NDJSON at `data/chain.ndjson`.
- Optional API client: `PlagiarismAPIClient` mock available for integration.

## Project Structure
- `src/main/java/com/example/plagiarism/*`
- `scripts/build.sh` build script
- `dist/` output JAR
- `data/` blockchain storage

## Future Enhancements
- File chooser to load text files
- Multiple document comparisons (pairwise matrix)
- Option to choose metric and thresholds
- Export reports (PDF/CSV)
- Real API integration over HTTP

## Diagrams
See `diagrams/architecture.puml` for class diagram and workflow.
