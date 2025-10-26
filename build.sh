#!/bin/bash
set -e

echo "Building Advanced Plagiarism Detection System..."

echo "Step 1: Generating sources list..."
find src -name "*.java" > sources.list

echo "Step 2: Compiling Java sources..."
javac -d out @sources.list

echo "Step 3: Creating JAR files..."
mkdir -p out_jar

echo "  - Creating original UI JAR..."
jar --create --file out_jar/plagiarism-app.jar --main-class com.example.plagiarism.SwingApp -C out .

echo "  - Creating enhanced UI JAR..."
jar --create --file out_jar/plagiarism-app-enhanced.jar --main-class com.example.plagiarism.ui.EnhancedSwingApp -C out .

echo ""
echo "Build complete!"
echo ""
echo "Run with:"
echo "  java -jar out_jar/plagiarism-app.jar              (Original UI)"
echo "  java -jar out_jar/plagiarism-app-enhanced.jar     (Enhanced UI - Recommended)"
echo ""
echo "Or use console mode:"
echo "  java -cp out com.example.plagiarism.ConsoleMain"
