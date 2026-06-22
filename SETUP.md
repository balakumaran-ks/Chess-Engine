# Setup Guide

Complete instructions for downloading, installing prerequisites, and running the Chess Engine project on macOS, Linux, and Windows.

## Prerequisites

The project requires two tools:

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| Java Development Kit (JDK) | 17 | Compile and run Java source code |
| Apache Maven | 3.6+ | Build tool: dependency management, compilation, testing, packaging |

No other software is needed for phases 1-4 (board, move generation, make/unmake, check detection). Later phases (UCI interface, MongoDB integration) add optional dependencies documented in `docs/DATABASE_INTEGRATION.md`.

---

## 1. Install the JDK (Java 17+)

### macOS

**Option A: Homebrew (recommended)**

```bash
# Install Homebrew if you don't have it
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OpenJDK 17
brew install openjdk@17

# Link it so the system finds it
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Set JAVA_HOME (add to ~/.zshrc or ~/.bash_profile)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

**Option B: Oracle JDK or Adoptium (manual download)**

1. Go to https://adoptium.net/ (Eclipse Temurin) or https://www.oracle.com/java/technologies/downloads/
2. Download the macOS `.pkg` installer for Java 17 (Temurin 17)
3. Run the installer
4. Set `JAVA_HOME`:
   ```bash
   echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home' >> ~/.zshrc
   source ~/.zshrc
   ```

**Verify:**
```bash
java -version
# Should print: openjdk version "17.x.x" or similar
javac -version
# Should print: javac 17.x.x
```

### Linux (Ubuntu/Debian)

```bash
# Update package index
sudo apt update

# Install OpenJDK 17 (JDK includes javac)
sudo apt install -y openjdk-17-jdk

# Set JAVA_HOME (add to ~/.bashrc)
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### Linux (Fedora/RHEL/CentOS)

```bash
sudo dnf install -y java-17-openjdk-devel
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
source ~/.bashrc
```

### Linux (Arch/Manjaro)

```bash
sudo pacman -S jdk17-openjdk
# JAVA_HOME is typically set automatically; verify with:
# archlinux-java status
```

### Windows

**Option A: Winget (Windows Package Manager)**

```powershell
# Open PowerShell as Administrator
winget install EclipseAdoptium.Temurin.17.JDK
```

**Option B: Manual download**

1. Go to https://adoptium.net/
2. Download the Windows `.msi` installer for Temurin 17
3. Run the installer — it will set `JAVA_HOME` automatically if you check the option
4. If `JAVA_HOME` wasn't set, set it manually:
   - Open System Properties → Environment Variables (or search "Environment Variables" in Start menu)
   - Under "System variables" (or "User variables"), click "New"
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot\` (adjust version)
   - Find the `Path` variable, click Edit, add `%JAVA_HOME%\bin` as a new entry

**Verify (open a new Command Prompt or PowerShell):**
```cmd
java -version
javac -version
```

---

## 2. Install Apache Maven

### macOS

```bash
brew install maven
```

**Manual alternative:**

1. Download Maven from https://maven.apache.org/download.cgi (binary tar.gz: `apache-maven-3.9.x-bin.tar.gz`)
2. Extract:
   ```bash
   cd ~/Downloads
   tar xzf apache-maven-3.9.x-bin.tar.gz
   sudo mv apache-maven-3.9.x /opt/maven
   ```
3. Add to PATH:
   ```bash
   echo 'export M2_HOME=/opt/maven' >> ~/.zshrc
   echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.zshrc
   source ~/.zshrc
   ```

### Linux (Ubuntu/Debian)

```bash
sudo apt install -y maven
```

### Linux (Fedora/RHEL)

```bash
sudo dnf install -y maven
```

### Linux (Arch/Manjaro)

```bash
sudo pacman -S maven
```

### Linux (manual)

```bash
cd /tmp
wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
sudo tar xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
sudo ln -s /opt/apache-maven-3.9.6 /opt/maven
echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Windows

**Option A: Winget**
```powershell
winget install Apache.Maven
```

**Option B: Manual download**

1. Download from https://maven.apache.org/download.cgi (binary zip: `apache-maven-3.9.x-bin.zip`)
2. Extract to `C:\Program Files\Apache\maven`
3. Set `M2_HOME`:
   - Open Environment Variables (same steps as Java above)
   - New variable: Name `M2_HOME`, Value `C:\Program Files\Apache\maven`
4. Add to `Path`: New entry `%M2_HOME%\bin`
5. Close and reopen Command Prompt

**Verify on all platforms:**
```bash
mvn -version
# Should print Maven version 3.6+ and Java 17
```

---

## 3. Download the Project

### Option A: Clone with Git (if hosted on a repository)

```bash
git clone <repository-url>
cd chess-engine
```

### Option B: Download as ZIP

1. Download the project ZIP file
2. Extract it:
   - macOS: Double-click the ZIP, or `unzip chess-engine.zip`
   - Linux: `unzip chess-engine.zip`
   - Windows: Right-click → "Extract All..."
3. Navigate into the project folder:
   ```bash
   cd chess-engine
   ```

### Option C: Transfer files directly

If you have the source files (e.g., on a USB drive or cloud storage), copy the entire project folder to your working directory. The folder should contain:
- `pom.xml` (Maven build file)
- `src/main/java/engine/` (source code)
- `src/test/java/engine/` (test code)
- `docs/` (documentation)
- `README.md`, `LICENSE`, etc.

---

## 4. Verify Your Setup

Before building, confirm all tools are installed and on your PATH.

### macOS / Linux

```bash
java -version    # Must show 17 or higher
javac -version   # Must show 17 or higher
mvn -version     # Must show Maven 3.6+ with Java 17
```

### Windows (Command Prompt or PowerShell)

```cmd
java -version
javac -version
mvn -version
```

If any command says "command not found" or "not recognized":
- **Java/javac**: `JAVA_HOME` is not set or `JAVA_HOME/bin` is not on `PATH`
- **Maven**: `M2_HOME` is not set or `M2_HOME/bin` is not on `PATH`
- Re-open your terminal after changing environment variables

---

## 5. Build and Test

All commands below run from the project root directory (the folder containing `pom.xml`).

### Full build with tests

```bash
mvn clean test
```

This will:
1. Download JUnit 5 and Maven plugins (only the first time — cached afterward in `~/.m2/`)
2. Compile all source files in `src/main/java/`
3. Compile all test files in `src/test/java/`
4. Run all tests via the Surefire plugin
5. Report results in the console

**Expected output on success:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running engine.board.BoardTest
[INFO] Running engine.constants.SquareTest
[INFO] Running engine.constants.PieceTest
...
[INFO] Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Compile without tests (faster)

```bash
mvn clean compile
```

### Run a specific test class

```bash
mvn test -Dtest=BoardTest
mvn test -Dtest=SquareTest
```

### Build the JAR (after implementing the UCI entry point)

```bash
mvn clean package
# Output: target/chess-engine-0.1.0.jar
```

### Run the built JAR

```bash
java -jar target/chess-engine-0.1.0.jar
```

---

## 6. Project Structure

```
chess-engine/
├── pom.xml                              # Maven build configuration
├── README.md                            # Project overview
├── SETUP.md                             # This file
├── LICENSE                              # MIT license
├── CONTRIBUTING.md                      # Code style and commit conventions
├── src/
│   ├── main/java/engine/
│   │   ├── constants/                   # Square, Piece, Color, File, Rank enums
│   │   ├── board/                       # Board, FenParser, UndoInfo
│   │   ├── move/                        # Move, MoveList, MoveGenerator,
│   │   │                                #   AttackTables, MagicBitboards, MoveFlag
│   │   └── utils/                       # SquareUtils bitboard utilities
│   │
│   └── test/java/engine/
│       ├── board/                       # BoardTest, FenParser tests
│       ├── constants/                   # SquareTest, PieceTest, etc.
│       └── utils/                       # SquareUtilsTest
│
└── docs/                                # All design documentation
    ├── ARCHITECTURE.md
    ├── BOARD_DESIGN.md
    ├── BOARD_OPERATIONS.md
    └── ...
```

---

## 7. Troubleshooting

### "java: command not found" or "'java' is not recognized"

Java is not on your PATH. Go back to [Section 1](#1-install-the-jdk-java-17) and verify `JAVA_HOME` is set and `$JAVA_HOME/bin` (or `%JAVA_HOME%\bin` on Windows) is on your PATH. Restart your terminal after making changes.

### "mvn: command not found"

Maven is not on your PATH. Go back to [Section 2](#2-install-apache-maven) and verify `M2_HOME` is set and `$M2_HOME/bin` is on your PATH. Restart your terminal.

### "mvn -version" shows Java 1.8 instead of 17

Maven found an older JDK. Set `JAVA_HOME` explicitly to the JDK 17 path:
- macOS/Linux: `export JAVA_HOME=/path/to/jdk-17` (add to shell profile)
- Windows: Set the `JAVA_HOME` environment variable in System Properties

### "BUILD FAILURE" with "cannot find symbol"

A compilation error. Check that all imports are present and that the file structure matches the package declarations. Review the error output for the specific file and line.

### "Cannot resolve symbol SquareUtils" or similar

The `engine/utils/SquareUtils.java` file is missing or the package doesn't match. Ensure the file exists at `src/main/java/engine/utils/SquareUtils.java` and declares `package engine.utils;`.

### Tests fail with "OutOfMemoryError" on perft depth 5+

Perft depth 5 explores ~4.8M nodes. The default JVM heap is usually sufficient, but if needed, increase it:

```bash
MAVEN_OPTS="-Xmx512m" mvn test
```

### Maven downloads are very slow

The first `mvn clean test` downloads JUnit 5 and all Maven plugins (about 20MB). Subsequent builds use the local cache in `~/.m2/repository/` and are fast. If your network is slow, the first build may take several minutes.

### Port conflicts or firewall issues

None. The chess engine runs entirely locally in phases 1-4. No network connections, ports, or external services are required.

---

## 8. Next Steps After Setup

Once `mvn clean test` passes:

1. Read `docs/INDEX.md` for a map of all documentation
2. Read `docs/ARCHITECTURE.md` for the system design
3. Read `docs/BOARD_OPERATIONS.md` for perft and make/unmake details
4. Run specific test classes to verify components:
   ```bash
   mvn test -Dtest=SquareTest          # Enum tests
   mvn test -Dtest=BoardTest           # Board and FEN tests
   mvn test -Dtest=engine.board.*      # All board-package tests
   ```

---

## 9. IDE Setup (Optional)

### IntelliJ IDEA

1. File → Open → select the project folder
2. IntelliJ will detect the `pom.xml` and import the Maven project
3. Confirm the JDK is set to 17: File → Project Structure → Project SDK → 17
4. Run tests by right-clicking on test files and selecting "Run"

### VS Code

1. Install the "Extension Pack for Java" (Microsoft)
2. Open the project folder
3. The extension will detect Maven automatically
4. Run tests by clicking the "Run" icon next to test methods

### Eclipse

1. File → Import → Maven → Existing Maven Projects
2. Select the project folder
3. Eclipse will import and configure the project
4. Run tests by right-clicking on test files → Run As → JUnit Test
