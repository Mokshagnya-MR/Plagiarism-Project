# PlantUML Diagrams

This directory contains all the system diagrams in PlantUML format for the Advanced Plagiarism Detection System v2.0.

## Viewing the Diagrams

You can view these diagrams using:

1. **Online PlantUML Editor**:
   - Visit https://www.plantuml.com/plantuml/uml/
   - Copy and paste the content of any `.puml` file

2. **VS Code Extension**:
   - Install the "PlantUML" extension
   - Open any `.puml` file and use `Alt+D` to preview

3. **IntelliJ IDEA**:
   - Install the "PlantUML integration" plugin
   - Right-click on any `.puml` file and select "Show Diagram"

4. **Command Line**:
   ```bash
   # Install PlantUML
   sudo apt-get install plantuml

   # Generate PNG images
   plantuml diagrams/*.puml
   ```

## Diagram Files

### 1. `use-case.puml`
Shows all the interactions users can have with the system, including:
- Document upload and text entry
- Algorithm selection
- Plagiarism checking
- Source discovery
- Result viewing and export
- Blockchain management
- System administration tasks

### 2. `class-diagram.puml`
Comprehensive class structure showing:
- All classes, interfaces, and their relationships
- Key methods and properties
- Inheritance and composition relationships
- Design patterns used

### 3. `architecture.puml`
Layered architecture diagram displaying:
- Presentation Layer (GUI and Console)
- Business Logic Layer
- Algorithm Layer
- Data Layer
- Persistence Layer
- External Services

### 4. `sequence-check-plagiarism.puml`
Step-by-step flow for checking plagiarism:
- Document upload
- Algorithm execution
- Result generation
- Blockchain update
- Storage operations

### 5. `sequence-auto-find-source.puml`
Automated source discovery flow:
- Web search for original source
- Content extraction
- Fallback to manual source
- Automatic plagiarism check

### 6. `component-diagram.puml`
High-level component interactions:
- UI components
- Core processing components
- Data management
- Storage and export
- External integrations

### 7. `state-diagram.puml`
Application state transitions:
- Initial ready state
- Document loading states
- Processing states
- Result states
- Export and save states

### 8. `activity-diagram.puml`
Complete workflow from start to finish:
- Document loading
- Algorithm selection
- Source handling (manual/auto)
- Processing steps
- Result generation
- Storage and export

### 9. `deployment-diagram.puml`
System deployment architecture:
- Local machine setup
- JAR file structure
- Local file system
- Cloud services (Supabase)
- External APIs

### 10. `data-flow-diagram.puml`
How data flows through the system:
- Input processing
- Algorithm execution
- Result generation
- Storage operations
- Output generation

## Generating Images

To generate PNG images from all diagrams:

```bash
cd diagrams
plantuml *.puml
```

This will create `.png` files for each diagram.

To generate SVG (scalable) images:

```bash
plantuml -tsvg *.puml
```

## Notes

- All diagrams follow PlantUML syntax
- Diagrams are designed to be clear and comprehensive
- Each diagram focuses on a specific aspect of the system
- Relationships and dependencies are clearly shown
- Color coding and grouping improve readability
