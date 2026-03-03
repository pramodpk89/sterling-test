# Sterling OMS AI Developer

An AI-powered development tool for IBM Sterling OMS. Pick a Jira story, run one command — Claude reads the story, writes the Sterling Java code, compiles it, commits, and raises a PR.

---

## How It Works

```
Developer picks a Jira story
         │
         ▼
  /implement-jira KAN-4       ← one command in Claude Code
         │
         ├─ Fetches story details from Jira API
         ├─ Creates feature branch  feature/KAN-4-<slug>  from main
         ├─ Reads Sterling knowledge base (architecture, patterns, APIs)
         ├─ Generates Java files (UE / Custom API / Service / Agent)
         ├─ Runs mvn compile — fixes any errors automatically
         ├─ Commits to feature branch with message linking back to Jira
         ├─ Pushes feature branch
         ├─ Raises a GitHub PR  (feature/KAN-4-<slug> → main)
         └─ Updates Jira: commit SHA + PR link + status → In Progress
```

No manual coding. No copy-pasting. The developer reviews the PR.

---

## One-Time Setup

### 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 11+ | Already present on Sterling dev machines |
| Maven | 3.8+ | Already present on Sterling dev machines |
| Claude Code | latest | Install from https://claude.ai/code |
| GitHub CLI (`gh`) | latest | For automatic PR creation — optional |

### 2. Clone the repo

```bash
git clone https://github.com/pramodpk89/sterling-test.git
cd sterling-test
```

### 3. Create the Jira credentials file

Create a file named `.jira` in the project root (it is gitignored — never committed):

```
JIRA_URL=https://yourcompany.atlassian.net
JIRA_EMAIL=you@yourcompany.com
JIRA_TOKEN=<your-api-token>
```

**Getting your Jira API token:**
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click **Create API token**
3. Copy the token into the `.jira` file

### 4. Log in to GitHub CLI (for automatic PRs)

```bash
gh auth login
```

If you skip this, `/implement-jira` will print the PR body for you to create manually.

---

## Daily Workflow — Jira to PR

### Step 1 — Open your Jira board and pick a story

Example: `https://yourcompany.atlassian.net/jira/software/projects/KAN/boards/1`

Note the issue key, e.g. **KAN-4**.

### Step 2 — Open Claude Code in the project root

```bash
cd sterling-test
claude
```

Sterling domain knowledge loads automatically. You will see the Claude Code prompt.

### Step 3 — Run the command

```
/implement-jira KAN-4
```

Claude will:

1. Read your `.jira` credentials file
2. Call the Jira API and fetch the full story (summary, description, acceptance criteria)
3. Read `context/sterling-knowledge.md` — Sterling architecture, patterns, and APIs
4. Identify what Java files to create from the story description
5. Implement all business logic described in the story
6. Run `mvn compile` — if there are errors, fix them and recompile
7. Commit with a message that includes the Jira issue key and link
8. Run `gh pr create` to raise a PR

### Step 4 — Review the PR

Claude prints the PR link. Open it in GitHub, review the generated code, and merge.

---

## Writing Good Jira Stories for This Tool

The quality of the generated code depends on the quality of the story. Include these in the story description:

| Field | Example |
|-------|---------|
| **What file to create** | `HighValueHoldRule.java` in `com.mycompany.sterling.holds` |
| **What the class does** | "Checks OrderTotal against threshold, calls changeOrder to apply hold" |
| **Input XML** | Paste a sample `<Order .../>` element |
| **Output / action** | Paste the `changeOrder` XML to build |
| **Error handling** | "If OrderTotal missing: log WARN MYCO_HOLD_001, return false" |
| **Method signature** | `public boolean evaluateHighValueHold(YFSEnvironment env, Document doc)` |
| **Unit test cases** | "OrderTotal=5000.00 → no hold; OrderTotal=5000.01 → hold applied" |

See `designs/implement-order-hold-service.md` for a fully worked example.

---

## Other Slash Commands

All commands are typed inside a Claude Code session (`claude` in the project root).

### `/implement-jira <issue-key>`
Full automated flow: Jira → code → compile → commit → PR.
```
/implement-jira KAN-4
/implement-jira KAN-6
```

### `/implement <design-doc-path>`
Implement from a local design doc instead of Jira. Useful when the story isn't in Jira yet or you want to design first.
```
/implement designs/implement-order-hold-service.md
```

### `/review-sterling <file-path>`
Audits a Java file against 7 Sterling-specific checks and returns a PASS/FAIL report.
```
/review-sterling src/main/java/com/mycompany/sterling/ue/BeforeCreateOrderHoldUE.java
```

### `/new-ue <ClassName> <ApiName>`
Scaffolds a new User Exit stub with the correct Sterling structure, error codes, and `api_list.xml` snippet.
```
/new-ue OrderHold createOrder
/new-ue ShipmentAlert confirmShipment
```

### `/new-api <ClassName>`
Scaffolds a Custom API stub.
```
/new-api PriceOverride
```

### `/new-agent <ClassName>`
Scaffolds an Agent stub.
```
/new-agent HoldReview
```

---

## Project Structure

```
sterling-test/
├── .jira                       # Jira credentials (gitignored — never committed)
├── .claude/
│   ├── CLAUDE.md               # Auto-loaded Sterling instructions for Claude
│   └── commands/               # Slash command definitions
│       ├── implement-jira.md   → /implement-jira
│       ├── implement.md        → /implement
│       ├── review-sterling.md  → /review-sterling
│       ├── new-ue.md           → /new-ue
│       ├── new-api.md          → /new-api
│       └── new-agent.md        → /new-agent
├── context/
│   ├── sterling-knowledge.md   # Sterling OMS domain knowledge (auto-loaded)
│   └── USAGE.md                # Detailed usage guide
├── designs/
│   ├── TEMPLATE.md             # Design doc template
│   └── implement-order-hold-service.md  # Worked example
├── src/main/java/com/mycompany/sterling/
│   ├── ue/                     # User Exits
│   ├── service/                # Custom services
│   ├── api/                    # Custom APIs
│   ├── agent/                  # Background agents
│   ├── holds/                  # Hold rule classes
│   └── util/                   # Shared utilities
└── pom.xml
```

---

## Adding Sterling JARs

Sterling JARs are not redistributable. Install them from your Sterling installation into your local Maven repo:

**Windows:**
```cmd
mvn install:install-file -Dfile=%STERLING_HOME%\jar\yfsjapi.jar ^
  -DgroupId=com.yantra -DartifactId=yfsjapi -Dversion=10.0 -Dpackaging=jar

mvn install:install-file -Dfile=%STERLING_HOME%\jar\yfcapi.jar ^
  -DgroupId=com.yantra -DartifactId=yfcapi -Dversion=10.0 -Dpackaging=jar

mvn install:install-file -Dfile=%STERLING_HOME%\jar\yifclient.jar ^
  -DgroupId=com.yantra -DartifactId=yifclient -Dversion=10.0 -Dpackaging=jar
```

Then uncomment the Sterling dependencies in `pom.xml`.

Until JARs are installed, generated code uses `org.w3c.dom` stubs. Each stub includes a comment showing the real Sterling `YFCDocument` / `YFCElement` pattern to use once JARs are available.

---

## Building

```bash
mvn compile        # compile only
mvn test           # compile + run tests
mvn package        # build JAR → target/sterling-customizations-1.0.0-SNAPSHOT.jar
```

---

## Deploying to Sterling

1. Copy the JAR to `%STERLING_HOME%\extensions\global\lib\`
2. Copy registration entries into `%STERLING_HOME%\extensions\global\api_list.xml`
   (snippets are printed by every slash command after code generation)
3. Restart the Sterling application server

See `context/sterling-knowledge.md` Section D for full configuration details.

---

## Conventions

| Component | Package | Suffix | Error code prefix |
|-----------|---------|--------|-------------------|
| Custom API | `com.mycompany.sterling.api` | `*API` | `MYCO_<MODULE>_NNN` |
| User Exit | `com.mycompany.sterling.ue` | `*UE` | |
| Service | `com.mycompany.sterling.service` | `*Service` | |
| Agent | `com.mycompany.sterling.agent` | `*Agent` | |
| Utility | `com.mycompany.sterling.util` | `*Helper` / `*Util` | |
