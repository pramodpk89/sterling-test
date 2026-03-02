# Sterling OMS Customizations

An AI-assisted development setup for IBM Sterling OMS customizations — User Exits, Custom APIs, Services, and Agents.

## Project Structure

```
sterling-test/
├── context/
│   ├── sterling-knowledge.md   # Sterling OMS domain knowledge (feed to AI)
│   └── USAGE.md                # How to use the knowledge file with Claude
├── designs/
│   ├── TEMPLATE.md             # Design doc template — copy for each feature
│   └── implement-order-hold-service.md  # Example design doc
├── src/main/java/com/mycompany/sterling/
│   ├── ue/                     # User Exits (YFS*UE implementations)
│   ├── service/                # Custom services
│   ├── api/                    # Custom APIs (YIFCustomApi extensions)
│   ├── agent/                  # Background agents (YFSAbstractTask)
│   └── util/                   # Shared utilities
├── tools/
│   └── implement_design.py     # AI agent: design doc → generated code
├── .claude/
│   └── CLAUDE.md               # Claude Code instructions (auto-loaded)
└── pom.xml
```

## Prerequisites

- Java 11+
- Maven 3.8+
- Python 3.11+ (for the AI agent script)
- `ANTHROPIC_API_KEY` environment variable set
- Sterling OMS JARs installed locally (see [Adding Sterling JARs](#adding-sterling-jars))

## Getting Started

```bash
git clone https://github.com/pramodpk89/sterling-test.git
cd sterling-test
pip install anthropic
```

## Implementing a Feature with AI

### 1. Write a design doc

Copy the template and fill it in:

```bash
cp designs/TEMPLATE.md designs/implement-my-feature.md
# Edit the new file
```

### 2. Run the implementation agent

```bash
python tools/implement_design.py designs/implement-my-feature.md
```

The script will:
1. Load `context/sterling-knowledge.md` as the AI system prompt
2. Call Claude to generate all Java source files
3. Run `mvn compile` to validate
4. Git commit on success

**Options:**

```bash
# Preview only — no files written
python tools/implement_design.py --dry-run designs/my-feature.md

# Write files + compile, but don't commit
python tools/implement_design.py --no-commit designs/my-feature.md
```

### 3. Using Claude Code directly

Claude Code automatically reads `.claude/CLAUDE.md` and `context/sterling-knowledge.md`
before making any changes. Just open the project and start coding:

```bash
claude
```

## Adding Sterling JARs

Sterling JARs are not redistributable. Install them into your local Maven repo from
your Sterling installation:

```bash
mvn install:install-file -Dfile=$STERLING_HOME/jar/yfsjapi.jar \
  -DgroupId=com.yantra -DartifactId=yfsjapi -Dversion=10.0 -Dpackaging=jar

mvn install:install-file -Dfile=$STERLING_HOME/jar/yfcapi.jar \
  -DgroupId=com.yantra -DartifactId=yfcapi -Dversion=10.0 -Dpackaging=jar

mvn install:install-file -Dfile=$STERLING_HOME/jar/yifclient.jar \
  -DgroupId=com.yantra -DartifactId=yifclient -Dversion=10.0 -Dpackaging=jar
```

Then uncomment the Sterling dependencies in `pom.xml`.

## Building

```bash
mvn compile        # compile only
mvn test           # compile + run tests
mvn package        # build JAR → target/sterling-customizations-1.0.0-SNAPSHOT.jar
```

## Deploying to Sterling

1. Copy the JAR to `$STERLING_HOME/extensions/global/lib/`
2. Register components in `$STERLING_HOME/extensions/global/api_list.xml`
3. Restart the Sterling application server

See `context/sterling-knowledge.md` Section D for full configuration details.

## Conventions

| Component | Package | Suffix | Error code prefix |
|-----------|---------|--------|-------------------|
| Custom API | `com.mycompany.sterling.api` | `*API` | `MYCO_<MODULE>_NNN` |
| User Exit | `com.mycompany.sterling.ue` | `*UE` | |
| Service | `com.mycompany.sterling.service` | `*Service` | |
| Agent | `com.mycompany.sterling.agent` | `*Agent` | |
| Utility | `com.mycompany.sterling.util` | `*Helper` / `*Util` | |
