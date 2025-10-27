# System Diagrams

This document contains all the diagrams for the Advanced Plagiarism Detection System v2.0.

---

## 1. Use Case Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Plagiarism Detection System                  │
└─────────────────────────────────────────────────────────────────┘

         User/Student                                  System Admin
              │                                              │
              │                                              │
              ├─────► Upload Documents                      │
              │                                              │
              ├─────► Enter Text Manually                   │
              │                                              │
              ├─────► Select Algorithm ◄────────────────────┤
              │       (Cosine, Jaccard,                     │
              │        Levenshtein, N-Gram)                 │
              │                                              │
              ├─────► Check Plagiarism                      │
              │                                              │
              ├─────► Auto Find Source                      │
              │       (Web Discovery)                       │
              │                                              │
              ├─────► View Results                          │
              │       (Score, Verdict)                      │
              │                                              │
              ├─────► View Blockchain History               │
              │                                              │
              ├─────► Export Results                        │
              │       (JSON, CSV, Report)                   │
              │                                              │
              ├─────► Save/Load Blockchain ◄────────────────┤
              │                                              │
              └─────► View Block Details                    │
                                                             │
                                              Validate Blockchain
                                                             │
                                              Configure Settings
                                                             │
                                              Clear History
```

---

## 2. Class Diagram

```
┌─────────────────────────────────┐
│          Document               │
├─────────────────────────────────┤
│ - title: String                 │
│ - author: String                │
│ - date: String                  │
│ - text: String                  │
│ - sourceUrl: String             │
│ - plagiarismScore: double       │
├─────────────────────────────────┤
│ + getTitle(): String            │
│ + getAuthor(): String           │
│ + getText(): String             │
│ + getSourceUrl(): String        │
│ + getPlagiarismScore(): double  │
│ + setPlagiarismScore(double)    │
└─────────────────────────────────┘
              △
              │
              │ contains
              │
┌─────────────────────────────────┐
│            Block                │
├─────────────────────────────────┤
│ - index: int                    │
│ - timestamp: String             │
│ - document: Document            │
│ - previousHash: String          │
│ - hash: String                  │
├─────────────────────────────────┤
│ + getIndex(): int               │
│ + getTimestamp(): String        │
│ + getDocument(): Document       │
│ + getHash(): String             │
│ + getPreviousHash(): String     │
│ + calculateHash(): String       │
└─────────────────────────────────┘
              △
              │
              │ manages
              │
┌─────────────────────────────────┐
│         Blockchain              │
├─────────────────────────────────┤
│ - blocks: List<Block>           │
├─────────────────────────────────┤
│ + addBlock(Document): Block     │
│ + getBlocks(): List<Block>      │
│ + isChainValid(): boolean       │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│    <<interface>>                │
│   SimilarityAlgorithm           │
├─────────────────────────────────┤
│ + calculate(s1, s2): double     │
└─────────────────────────────────┘
              △
              │
    ┌─────────┼─────────────┬──────────────┐
    │         │             │              │
┌───────────┐ ┌──────────┐ ┌────────────┐ ┌────────────┐
│ Cosine    │ │ Jaccard  │ │Levenshtein │ │  N-Gram    │
│Similarity │ │  Index   │ │ Similarity │ │ Similarity │
└───────────┘ └──────────┘ └────────────┘ └────────────┘


┌─────────────────────────────────┐
│     PlagiarismChecker           │
├─────────────────────────────────┤
│ + checkPlagiarism(doc1, doc2,   │
│     algorithm): Result          │
│ + pairwiseCheck(List<Document>, │
│     algorithm): List<Result>    │
├─────────────────────────────────┤
│ + Result(score, verdict,        │
│          doc1, doc2)            │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│   SourceDiscoveryService        │
├─────────────────────────────────┤
│ + discoverOriginalSource(text): │
│     Optional<DiscoveredSource>  │
├─────────────────────────────────┤
│ + DiscoveredSource(text, url)   │
└─────────────────────────────────┘
              △
              │
┌─────────────────────────────────┐
│  AISourceDiscoveryService       │
├─────────────────────────────────┤
│ + findSourceWithAI(text):       │
│     Optional<DiscoveredSource>  │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│      StorageManager             │
├─────────────────────────────────┤
│ + saveChainToFile(Blockchain,   │
│     File): void                 │
│ + loadChainFromFile(File):      │
│     Blockchain                  │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│      SupabaseClient             │
├─────────────────────────────────┤
│ - supabaseUrl: String           │
│ - supabaseKey: String           │
├─────────────────────────────────┤
│ + syncBlockchain(blocks): void  │
│ + isConfigured(): boolean       │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│      ResultExporter             │
├─────────────────────────────────┤
│ + exportBlockchainToJSON(blocks,│
│     File): void                 │
│ + exportToCSV(results, File)    │
│ + generateReport(result, File)  │
└─────────────────────────────────┘


┌─────────────────────────────────┐
│      EnhancedSwingApp           │
├─────────────────────────────────┤
│ - frame: JFrame                 │
│ - textArea1: JTextArea          │
│ - textArea2: JTextArea          │
│ - algorithmBox: JComboBox       │
│ - blockchain: Blockchain        │
│ - supabaseClient: SupabaseClient│
├─────────────────────────────────┤
│ + displayForm(): void           │
│ - onCheck(): void               │
│ - onAutoFindSource(): void      │
│ - onSave(): void                │
│ - onLoad(): void                │
│ - exportResults(format): void   │
└─────────────────────────────────┘
```

---

## 3. System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                           │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │  EnhancedSwingApp    │      │   ConsoleMain        │          │
│  │  (GUI Interface)     │      │  (CLI Interface)     │          │
│  └──────────────────────┘      └──────────────────────┘          │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                        Business Logic Layer                        │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │ PlagiarismChecker    │      │ SourceDiscoveryService│         │
│  │ - checkPlagiarism()  │      │ - discoverSource()   │          │
│  └──────────────────────┘      └──────────────────────┘          │
│                                                                    │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │ TextPreprocessor     │      │ AISourceDiscovery    │          │
│  │ - preprocess()       │      │ - findWithAI()       │          │
│  └──────────────────────┘      └──────────────────────┘          │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                      Algorithm Layer                               │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
│  │  Cosine    │  │  Jaccard   │  │Levenshtein │  │  N-Gram    │ │
│  │ Similarity │  │   Index    │  │ Similarity │  │ Similarity │ │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘ │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                        Data Layer                                  │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │   Blockchain         │      │   Document           │          │
│  │   - addBlock()       │      │   - text, metadata   │          │
│  │   - isChainValid()   │      │                      │          │
│  └──────────────────────┘      └──────────────────────┘          │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     Persistence Layer                              │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │  StorageManager      │      │  SupabaseClient      │          │
│  │  (Local Files)       │      │  (Cloud Sync)        │          │
│  └──────────────────────┘      └──────────────────────┘          │
│                                                                    │
│  ┌──────────────────────┐                                         │
│  │  ResultExporter      │                                         │
│  │  (JSON/CSV/Report)   │                                         │
│  └──────────────────────┘                                         │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     External Services                              │
│  ┌──────────────────────┐      ┌──────────────────────┐          │
│  │  Web APIs            │      │  Supabase Database   │          │
│  │  (Source Discovery)  │      │  (Blockchain Sync)   │          │
│  └──────────────────────┘      └──────────────────────┘          │
└───────────────────────────────────────────────────────────────────┘
```

---

## 4. Sequence Diagram - Check Plagiarism Flow

```
User          UI           PlagiarismChecker    Algorithm    Blockchain    Storage
 │             │                   │                │            │           │
 │──Upload─────►                   │                │            │           │
 │   Docs      │                   │                │            │           │
 │             │                   │                │            │           │
 │──Select─────►                   │                │            │           │
 │ Algorithm   │                   │                │            │           │
 │             │                   │                │            │           │
 │──Click──────►                   │                │            │           │
 │  Check      │                   │                │            │           │
 │             │                   │                │            │           │
 │             │──checkPlagiarism──►                │            │           │
 │             │   (doc1, doc2)    │                │            │           │
 │             │                   │                │            │           │
 │             │                   │──calculate─────►            │           │
 │             │                   │   (text1,text2)│            │           │
 │             │                   │                │            │           │
 │             │                   │◄─score─────────┤            │           │
 │             │                   │                │            │           │
 │             │◄───Result─────────┤                │            │           │
 │             │  (score, verdict) │                │            │           │
 │             │                   │                │            │           │
 │             │──────────addBlock─────────────────────────────► │           │
 │             │                   │                │   (doc)    │           │
 │             │                   │                │            │           │
 │             │                   │                │            │──save─────►
 │             │                   │                │            │  Chain    │
 │             │                   │                │            │           │
 │◄─Display────┤                   │                │            │           │
 │   Result    │                   │                │            │           │
 │             │                   │                │            │           │
```

---

## 5. Sequence Diagram - Auto Find Source Flow

```
User          UI        SourceDiscovery    WebAPI    UI        Checker    Blockchain
 │             │               │             │        │           │           │
 │──Enter──────►               │             │        │           │           │
 │ Submission  │               │             │        │           │           │
 │             │               │             │        │           │           │
 │──Click──────►               │             │        │           │           │
 │  Auto Find  │               │             │        │           │           │
 │             │               │             │        │           │           │
 │             │──discover─────►             │        │           │           │
 │             │  Source(text) │             │        │           │           │
 │             │               │             │        │           │           │
 │             │               │──search─────►        │           │           │
 │             │               │   (excerpt) │        │           │           │
 │             │               │             │        │           │           │
 │             │               │◄─results────┤        │           │           │
 │             │               │             │        │           │           │
 │             │◄──source──────┤             │        │           │           │
 │             │  (text, url)  │             │        │           │           │
 │             │               │             │        │           │           │
 │             │──populate─────────────────────────►  │           │           │
 │             │  TextArea2    │             │        │           │           │
 │             │               │             │        │           │           │
 │             │────────────checkPlagiarism──────────►│           │           │
 │             │               │             │        │           │           │
 │             │◄────────────────Result───────────────┤           │           │
 │             │               │             │        │           │           │
 │             │────────────────────addBlock──────────────────────►           │
 │             │               │             │        │           │           │
 │◄─Display────┤               │             │        │           │           │
 │   Result    │               │             │        │           │           │
 │             │               │             │        │           │           │
```

---

## 6. Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interface                            │
│  ┌─────────────────────┐         ┌─────────────────────┐        │
│  │  EnhancedSwingApp   │         │    SettingsDialog   │        │
│  └─────────────────────┘         └─────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                    │                           │
                    ▼                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Core Components                             │
│  ┌────────────────────────────────────────────────────┐         │
│  │          PlagiarismChecker                         │         │
│  │  - checkPlagiarism(doc1, doc2, algorithm)         │         │
│  │  - pairwiseCheck(documents, algorithm)            │         │
│  └────────────────────────────────────────────────────┘         │
│                           │                                      │
│  ┌────────────────────────┼─────────────────────┐              │
│  │                        │                     │               │
│  ▼                        ▼                     ▼               │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐      │
│  │TextPreproc. │  │ Similarity   │  │SourceDiscovery   │      │
│  │             │  │ Algorithms   │  │   Service        │      │
│  └─────────────┘  └──────────────┘  └──────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Management                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │   Blockchain    │  │    Document     │  │   AppConfig     ││
│  │   Management    │  │    Model        │  │                 ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Storage & Export                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │ StorageManager  │  │ SupabaseClient  │  │ ResultExporter  ││
│  │  (Local File)   │  │  (Cloud Sync)   │  │ (JSON/CSV/TXT)  ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Data Flow Diagram

```
                    ┌─────────────────┐
                    │      User       │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Input Text    │
                    │   or Upload     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │Text Preprocessor│
                    │ - Tokenization  │
                    │ - Normalization │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Select Algo    │
              ┌─────┤  & Parameters   │─────┐
              │     └─────────────────┘     │
              │                             │
              ▼                             ▼
    ┌─────────────────┐         ┌─────────────────┐
    │ Manual Compare  │         │ Auto Discover   │
    │  (2 Documents)  │         │    Source       │
    └────────┬────────┘         └────────┬────────┘
             │                           │
             │                           ▼
             │                  ┌─────────────────┐
             │                  │  Web Search API │
             │                  │   (Extract)     │
             │                  └────────┬────────┘
             │                           │
             └─────────┬─────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  Similarity     │
              │  Calculation    │
              │ (Algorithm)     │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   Score &       │
              │   Verdict       │
              └────────┬────────┘
                       │
                       ├────────────────────────┐
                       │                        │
                       ▼                        ▼
              ┌─────────────────┐      ┌─────────────────┐
              │   Add to        │      │   Display       │
              │   Blockchain    │      │   Result        │
              └────────┬────────┘      └─────────────────┘
                       │
                       ├────────────────────────┐
                       │                        │
                       ▼                        ▼
              ┌─────────────────┐      ┌─────────────────┐
              │  Local Storage  │      │  Supabase Cloud │
              │  (File System)  │      │     Sync        │
              └─────────────────┘      └─────────────────┘
```

---

## 8. Deployment Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     User's Local Machine                         │
│                                                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │               Java Runtime Environment                  │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │     plagiarism-app-enhanced.jar                  │  │    │
│  │  │                                                  │  │    │
│  │  │  ┌────────────────┐  ┌────────────────────┐    │  │    │
│  │  │  │  GUI (Swing)   │  │  Console Interface │    │  │    │
│  │  │  └────────────────┘  └────────────────────┘    │  │    │
│  │  │                                                  │  │    │
│  │  │  ┌─────────────────────────────────────────┐   │  │    │
│  │  │  │      Core Business Logic                │   │  │    │
│  │  │  │  - PlagiarismChecker                    │   │  │    │
│  │  │  │  - Similarity Algorithms                │   │  │    │
│  │  │  │  - Blockchain                           │   │  │    │
│  │  │  └─────────────────────────────────────────┘   │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Local File System                          │    │
│  │  - plagiarism_chain.txt                                │    │
│  │  - blockchain_export.json                              │    │
│  │  - results_export.csv                                  │    │
│  │  - config.properties                                   │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Cloud Services                              │
│                                                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Supabase (PostgreSQL)                      │    │
│  │  - Blockchain data sync                                │    │
│  │  - User authentication (optional)                      │    │
│  │  - Real-time sync                                      │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Web Search APIs                            │    │
│  │  - Source discovery services                           │    │
│  │  - Content extraction                                  │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. State Diagram - Application States

```
                    ┌─────────────┐
                    │   Initial   │
                    │    Ready    │
                    └──────┬──────┘
                           │
                           │ User loads documents
                           ▼
                    ┌─────────────┐
                    │  Documents  │
                    │   Loaded    │
                    └──────┬──────┘
                           │
                ┌──────────┼──────────┐
                │                     │
                │ Manual Check        │ Auto Discover
                ▼                     ▼
         ┌─────────────┐       ┌─────────────┐
         │  Comparing  │       │ Discovering │
         │  Documents  │       │   Source    │
         └──────┬──────┘       └──────┬──────┘
                │                     │
                │                     │ Source found
                │                     ▼
                │              ┌─────────────┐
                │              │   Source    │
                │              │  Populated  │
                │              └──────┬──────┘
                │                     │
                └──────────┬──────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Calculating │
                    │ Similarity  │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Results   │
                    │  Generated  │
                    └──────┬──────┘
                           │
                ┌──────────┼──────────┐
                │                     │
                │ Add to Blockchain   │ Export Results
                ▼                     ▼
         ┌─────────────┐       ┌─────────────┐
         │  Blockchain │       │  Exporting  │
         │   Updated   │       │     Data    │
         └──────┬──────┘       └──────┬──────┘
                │                     │
                └──────────┬──────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  Complete   │
                    │   (Ready)   │
                    └─────────────┘
```

---

## 10. Activity Diagram - Plagiarism Detection Workflow

```
                        [Start]
                           │
                           ▼
                  ┌─────────────────┐
                  │  Load Documents │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ Select Algorithm│
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
              ┌───┤  Source Type?   ├───┐
              │   └─────────────────┘   │
              │                         │
     [Manual] │                         │ [Auto]
              │                         │
              ▼                         ▼
    ┌──────────────────┐      ┌──────────────────┐
    │ Both documents   │      │  Search web for  │
    │   provided?      │      │  original source │
    └────────┬─────────┘      └────────┬─────────┘
             │                         │
        [Yes]│ [No]                    ▼
             │  │            ┌──────────────────┐
             │  └──►[Error]  │  Source found?   │
             │               └────────┬─────────┘
             │                        │
             │                   [Yes]│ [No]
             │                        │  │
             │                        │  └──► ┌──────────────────┐
             │                        │       │ Prompt user for  │
             │                        │       │  manual source   │
             │                        │       └────────┬─────────┘
             │                        │                │
             │                        ▼                │
             └────────────────►┌──────────────────┐◄──┘
                               │ Preprocess Texts │
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │  Apply Selected  │
                               │    Algorithm     │
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │ Calculate Score  │
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │ Determine Verdict│
                               │ - Safe (<40%)    │
                               │ - Moderate (40-70)
                               │ - Suspicious (>70)
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │  Create Block    │
                               │  in Blockchain   │
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │  Save/Sync Data  │
                               └────────┬─────────┘
                                        │
                                        ▼
                               ┌──────────────────┐
                               │  Display Results │
                               └────────┬─────────┘
                                        │
                                        ▼
                                     [End]
```

---

## Notes

- **Use Case Diagram**: Shows all the interactions users can have with the system
- **Class Diagram**: Shows the relationships between classes and their methods/properties
- **Architecture Diagram**: Shows the layered architecture with clear separation of concerns
- **Sequence Diagrams**: Show the flow of operations for key features
- **Component Diagram**: Shows how major components interact
- **Data Flow Diagram**: Shows how data moves through the system
- **Deployment Diagram**: Shows how the application is deployed
- **State Diagram**: Shows the different states the application can be in
- **Activity Diagram**: Shows the step-by-step workflow for plagiarism detection

These diagrams provide a comprehensive view of the system's design, architecture, and functionality.
