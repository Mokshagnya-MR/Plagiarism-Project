#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/out"
DIST_DIR="$ROOT_DIR/dist"
MAIN_CLASS="com.example.plagiarism.SwingApp"

rm -rf "$OUT_DIR" "$DIST_DIR"
mkdir -p "$OUT_DIR" "$DIST_DIR"

# Compile
find "$ROOT_DIR/src/main/java" -name '*.java' > "$OUT_DIR/sources.txt"
javac -d "$OUT_DIR" @"$OUT_DIR/sources.txt"

# Create manifest
cat > "$OUT_DIR/manifest.mf" <<EOF
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS
Class-Path: .

EOF

# Package jar
jar cfm "$DIST_DIR/plagiarism-detector.jar" "$OUT_DIR/manifest.mf" -C "$OUT_DIR" .

echo "Built: $DIST_DIR/plagiarism-detector.jar"