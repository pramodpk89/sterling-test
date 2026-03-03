# Developer Usage Guide

This guide explains how to use the AI-powered Sterling OMS development workflow day to day.

---

## The Two Workflows

### Workflow A — Jira story to PR (recommended)

Write the story in Jira → run one command → get a PR.

```
/implement-jira KAN-4
```

### Workflow B — Design doc to code

Write a local design doc → run one command → get compiled code.

```
/implement designs/my-feature.md
```

Use Workflow B when the story lives outside Jira, or when you want to design in detail before generating code.

---

## Workflow A — Step by Step

### Prerequisites (one-time)

1. Create a `.jira` file in the project root:
   ```
   JIRA_URL=https://yourcompany.atlassian.net
   JIRA_EMAIL=you@yourcompany.com
   JIRA_TOKEN=<api-token from https://id.atlassian.com/manage-profile/security/api-tokens>
   ```
2. Run `gh auth login` for automatic PR creation (optional).

### Running it

```bash
cd sterling-test
claude                        # open Claude Code — context loads automatically
/implement-jira KAN-4         # type this at the Claude Code prompt
```

### What Claude does automatically

| Step | What happens |
|------|-------------|
| Fetch | Calls Jira REST API, reads summary + full description |
| Analyse | Identifies Java files, packages, component type (UE/API/Service/Agent) |
| Implement | Writes all business logic from the story's acceptance criteria |
| Compile | Runs `mvn compile`, fixes any errors |
| Commit | `git commit` with message referencing the Jira key |
| PR | `gh pr create` with summary and Jira link |

### What you do

Review the PR. Merge when happy.

---

## Workflow B — Step by Step

### 1. Copy the template

```bash
cp designs/TEMPLATE.md designs/implement-my-feature.md
```

### 2. Fill in the design doc

Open `designs/implement-my-feature.md` and complete every section:
- **Components to Create** — list every Java file with package and type
- **Business Logic** — number each rule clearly
- **Error Handling** — specify fail-open or fail-closed and all error codes
- **Input / Output XML** — paste sample documents

See `designs/implement-order-hold-service.md` for a fully worked example.

### 3. Implement

```bash
claude                                             # open Claude Code
/implement designs/implement-my-feature.md        # generates code + compiles
```

### 4. Commit and push

```bash
git add src/
git commit -m "feat: implement my-feature"
git push
```

---

## Writing Stories That Work Well

The `/implement-jira` command is only as good as the story. Follow this checklist when writing stories in Jira:

- [ ] State the **file name and package** explicitly (e.g. `HighValueHoldRule.java` in `com.mycompany.sterling.holds`)
- [ ] Describe **what the class does** in plain English
- [ ] Include a **sample input XML** element
- [ ] Include the **output XML** or action (e.g. the `changeOrder` XML to build)
- [ ] List **error codes** using the `MYCO_<MODULE>_NNN` format
- [ ] State the **fail strategy** — fail-open (order proceeds on error) or fail-closed (error blocks the order)
- [ ] List **unit test cases** as bullet points with expected return values

---

## Keeping `sterling-knowledge.md` Up to Date

`context/sterling-knowledge.md` is the source of truth that makes generated code correct. Update it when:

| Event | What to update |
|-------|---------------|
| New Sterling API used in the project | Add a row to the Section C table |
| New error code series introduced | Add to the Section E error code table |
| New project convention agreed | Update Section E |
| Sterling version upgraded | Review Section A and Section D |
| New UE interface or extension point discovered | Add pattern to Section B |

Commit `sterling-knowledge.md` alongside the code change that prompted the update.

---

## Troubleshooting

**`/implement-jira` says "Missing .jira credentials file"**
Create `.jira` in the project root with `JIRA_URL`, `JIRA_EMAIL`, and `JIRA_TOKEN`.

**`/implement-jira` returns a Jira error**
Check that your API token is valid and has read access to the project. Regenerate at https://id.atlassian.com/manage-profile/security/api-tokens.

**`mvn compile` fails after code generation**
Claude will attempt to fix compile errors automatically. If it cannot, run `/review-sterling` on the generated file — it will identify the exact issue.

**PR creation fails**
Run `gh auth login` and try again. If `gh` is not installed, Claude prints the PR body so you can create it manually on GitHub.
