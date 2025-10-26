# Quick Start Guide

## What You Have

This is a fully-featured plagiarism detection system with:

1. **Enhanced Modern UI** (`EnhancedSwingApp.java`)
   - Clean, professional interface
   - Progress bars and visual feedback
   - File upload support
   - History panel with blockchain view
   - Settings dialog

2. **AI-Powered Source Discovery** (`AISourceDiscoveryService.java`)
   - Automatically finds original sources online
   - Supports Anthropic Claude and OpenAI GPT
   - Falls back to web search if AI unavailable
   - Configurable via settings

3. **Multiple Similarity Algorithms**
   - Cosine Similarity
   - Jaccard Index
   - Levenshtein Distance
   - N-Gram Analysis (trigrams)

4. **Blockchain Storage** (Local Files)
   - Tamper-proof blockchain for audit trails
   - SHA-256 hashing
   - Stores to `~/plagiarism_chain.txt` by default
   - JSON/CSV export options

5. **Local Storage** (No Cloud Required)
   - All data stored on your computer
   - Configuration: `~/.plagiarism_checker_config.properties`
   - Blockchain data: `~/plagiarism_chain.txt`
   - Optional Supabase cloud sync (can be disabled)

## Building the Application

### Prerequisites

You need Java 11 or higher. Check if you have it:

```bash
java -version
```

If not installed, download from:
- https://www.oracle.com/java/technologies/downloads/
- Or use OpenJDK: https://openjdk.org/

### Compile and Run

**Option 1: Use the build script (easiest)**

```bash
./build.sh
java -jar out_jar/plagiarism-app-enhanced.jar
```

**Option 2: Manual compilation**

```bash
# Compile
find src -name "*.java" > sources.list
javac -d out @sources.list

# Create JAR
mkdir -p out_jar
jar --create --file out_jar/plagiarism-app-enhanced.jar \
    --main-class com.example.plagiarism.ui.EnhancedSwingApp -C out .

# Run
java -jar out_jar/plagiarism-app-enhanced.jar
```

**Option 3: Console mode (no GUI)**

```bash
java -cp out com.example.plagiarism.ConsoleMain
```

## Using the Application

### Basic Plagiarism Check

1. Launch the application
2. Paste or upload text in "Submission Document"
3. Paste or upload text in "Source Document"
4. Select an algorithm (Cosine is good default)
5. Click "Check Plagiarism"

### Auto-Discovery Feature

1. Paste the submission text in the first box
2. Click "Auto-Find Source" button
3. The app will:
   - Use AI to generate smart search queries (if API key configured)
   - Search the web for likely sources
   - Automatically compare with found sources
   - Show similarity results

### Configuring AI Features (Optional)

To enable AI source discovery, set environment variables:

**For Anthropic Claude:**
```bash
export ANTHROPIC_API_KEY="your-api-key-here"
```

**For OpenAI GPT:**
```bash
export OPENAI_API_KEY="your-api-key-here"
```

Then in the app, go to Tools → Settings and:
- Enable "AI Source Discovery"
- Select your AI model (Anthropic or OpenAI)

Without API keys, the app still works using basic web search.

### Viewing History

- The "Blockchain History" panel shows all past checks
- Click "View Details" to see full information
- All data is stored locally

### Exporting Results

File menu options:
- Export to JSON (structured data)
- Export to CSV (spreadsheet)
- Export Detailed Report (human-readable)

## Configuration

Settings are stored in `~/.plagiarism_checker_config.properties`:

- `similarity.threshold.safe` - Below this is safe (default: 30%)
- `similarity.threshold.high` - Above this is high risk (default: 70%)
- `ai.enabled` - Enable AI source discovery
- `ai.model` - Choose "anthropic" or "openai"
- `supabase.enabled` - Cloud sync (can disable for local-only)
- `blockchain.auto_save` - Automatically save after each check

## Troubleshooting

**"javac: command not found"**
- Install Java Development Kit (JDK), not just Java Runtime (JRE)

**"AI API error: 401"**
- Your API key is invalid or not set
- App will fall back to basic web search automatically

**"Display not found" or GUI won't start**
- You're in a headless environment (no display)
- Use console mode: `java -cp out com.example.plagiarism.ConsoleMain`

**Where is my data stored?**
- Configuration: `~/.plagiarism_checker_config.properties`
- Blockchain: `~/plagiarism_chain.txt` (or custom location)
- Everything is local - no cloud storage unless you enable Supabase

## Features Summary

✅ Multiple similarity algorithms
✅ AI-powered source finding
✅ Modern graphical interface
✅ Console mode for headless systems
✅ Blockchain audit trail
✅ Local file storage (no cloud required)
✅ JSON/CSV export
✅ Configurable thresholds
✅ File upload support
✅ History tracking

Enjoy using the Advanced Plagiarism Detection System!
