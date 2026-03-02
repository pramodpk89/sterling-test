# Sterling OMS Customizations

An AI-assisted development setup for IBM Sterling OMS customizations — User Exits, Custom APIs, Services, and Agents.

## Project Structure

```
sterling-test/
├── context/
│   ├── sterling-knowledge.md   # Sterling OMS domain knowledge (loaded by Claude automatically)
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
├── .claude/
│   └── CLAUDE.md               # Claude Code instructions (auto-loaded)
└── pom.xml
```

## Prerequisites

- Java 11+
- Maven 3.8+
- [Claude Code](https://claude.ai/code) CLI installed
- Sterling OMS JARs installed locally (see [Adding Sterling JARs](#adding-sterling-jars))

## Getting Started

```bash
git clone https://github.com/pramodpk89/sterling-test.git
cd sterling-test
claude   # opens Claude Code — Sterling context loads automatically
```

## Implementing a Feature with Claude

### 1. Write a design doc

Copy the template and fill in all sections:

```bash
cp designs/TEMPLATE.md designs/implement-my-feature.md
# Edit designs/implement-my-feature.md
```

### 2. Ask Claude to implement it

Open Claude Code in the project root and ask:

```
Implement the design doc at designs/implement-my-feature.md
```

Claude will:
1. Read `context/sterling-knowledge.md` for Sterling domain knowledge (auto-loaded via `.claude/CLAUDE.md`)
2. Read the design doc fully
3. Generate all Java source files listed under "Components to Create"
4. Follow Sterling coding patterns (YFCDocument, error codes, fail strategy, etc.)
5. Add registration stubs for `api_list.xml` / `services.xml`

### 3. Validate and commit

```bash
mvn compile
git add src/ && git commit -m "feat: implement my-feature"
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
