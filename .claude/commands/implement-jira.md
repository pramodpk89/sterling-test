Read `context/sterling-knowledge.md` fully before writing any code.

Arguments: $ARGUMENTS
Expected: a Jira issue key (e.g. `KAN-6`)

If no argument is provided, stop and respond:
"Usage: /implement-jira <issue-key>  (e.g. /implement-jira KAN-6)"

---

Execute these steps in order. Do not skip any step.

**Step 1 — Load Jira credentials**
Read the file `.jira` in the project root. Parse the three lines:
- JIRA_URL
- JIRA_EMAIL
- JIRA_TOKEN

If `.jira` is missing, stop and respond:
"Missing .jira credentials file. Create it with JIRA_URL, JIRA_EMAIL, JIRA_TOKEN."

---

**Step 2 — Fetch the Jira issue**
```
curl -s -u "<JIRA_EMAIL>:<JIRA_TOKEN>" "<JIRA_URL>/rest/api/3/issue/<ISSUE_KEY>"
```

Parse the JSON response. Extract:
- `fields.summary` → feature name
- `fields.description` → full story description (walk Atlassian Document Format: collect all `text` node values recursively)
- `fields.issuetype.name` → issue type
- `fields.status.name` → current status

If the response contains `errorMessages`, stop and print the error.

---

**Step 3 — Create a feature branch**

Derive the branch name from the issue key and summary:
- Lowercase the summary
- Replace spaces and special characters with hyphens
- Truncate to 50 characters
- Final format: `feature/<ISSUE-KEY>-<slug>`
- Example: `feature/KAN-6-implement-high-value-order-hold-rule`

Determine the base branch (main or master):
```
git remote show origin | grep "HEAD branch"
```

Switch to the base branch and pull latest, then create and switch to the feature branch:
```
git checkout <base-branch>
git pull origin <base-branch>
git checkout -b feature/<ISSUE-KEY>-<slug>
```

If the branch already exists, stop and respond:
"Branch feature/<ISSUE-KEY>-<slug> already exists. Delete it or use a different issue key."

---

**Step 4 — Map story to Sterling component type**
From the description, determine:
- What Java files to create (class names, package names, file names)
- What Sterling component type each file is (User Exit / Custom API / Service / Agent)
- What business logic to implement (rules, conditions, error handling)
- What Sterling APIs are called (changeOrder, getOrderDetails, etc.)
- What error codes to use (look for MYCO_* codes; derive if absent)
- Fail strategy (fail-open unless description says otherwise)

---

**Step 5 — Implement the code**
For each file identified in Step 4:

a. Apply the correct pattern from Section B of `context/sterling-knowledge.md`

b. Sterling JARs are absent — use `org.w3c.dom` with YFC pattern comments:
   ```java
   // In real Sterling: YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
   // YFCElement root = inDoc.getDocumentElement();
   // String val = root.getAttribute("Attr"); // returns "" not null
   Element root = inputDoc.getDocumentElement(); // stub
   String val = SterlingAPIHelper.safeGetAttribute(root, "Attr");
   ```

c. Use `SterlingAPIHelper` from `com.mycompany.sterling.util` for all attribute reads and API calls.

d. Implement ALL business logic from the story — not a stub.

e. Include the registration snippet (api_list.xml / services.xml) as a Javadoc `<pre>` comment.

f. Place files at the correct path matching the package in the story.

---

**Step 6 — Compile**
Run `mvn compile`. If it fails, fix the errors and recompile until clean.

---

**Step 7 — Commit to the feature branch**
Stage only the newly created source files and commit:
```
feat(<issue-key>): <story summary in lowercase>

<2-3 bullet points summarising what was implemented>

Jira: <JIRA_URL>/browse/<issue-key>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

Get the commit SHA for use in later steps:
```
git rev-parse HEAD
```

---

**Step 8 — Push the feature branch**
```
git push -u origin feature/<ISSUE-KEY>-<slug>
```

Build the GitHub commit URL from the remote origin URL and the commit SHA:
- Strip `.git` suffix from remote URL
- Append `/commit/<SHA>`

---

**Step 9 — Raise a PR**
```
gh pr create \
  --base <base-branch> \
  --head feature/<ISSUE-KEY>-<slug> \
  --title "<ISSUE-KEY>: <story summary>" \
  --body "$(cat <<'EOF'
## Summary
<2-3 bullet points from the story>

## Jira
<JIRA_URL>/browse/<ISSUE-KEY>

## Test plan
- [ ] mvn compile passes
- [ ] Unit tests cover rule boundary conditions
- [ ] Sterling registration stubs present in Javadoc

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

Capture the PR URL printed by `gh pr create`.

If `gh` is not installed or not authenticated, skip and print the PR body for manual creation. Set PR_URL to "(PR not created — create manually)".

---

**Step 10 — Update Jira**

**10a — Post a comment with commit and PR details**

Post one comment containing both the commit SHA and the PR link:
```
curl -s -u "<JIRA_EMAIL>:<JIRA_TOKEN>" \
  -X POST \
  -H "Content-Type: application/json" \
  "<JIRA_URL>/rest/api/3/issue/<ISSUE_KEY>/comment" \
  -d '{
    "body": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "✅ Implemented by Claude Code", "marks": [{"type": "strong"}] }
          ]
        },
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "Branch: " },
            { "type": "text", "text": "feature/<ISSUE-KEY>-<slug>", "marks": [{"type": "code"}] }
          ]
        },
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "Commit: " },
            { "type": "text", "text": "<COMMIT_SHA>", "marks": [{"type": "code"}] },
            { "type": "text", "text": " — <COMMIT_URL>" }
          ]
        },
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "PR: <PR_URL>" }
          ]
        },
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "Files: <comma-separated list of generated files>" }
          ]
        }
      ]
    }
  }'
```

If the POST fails, log the error but do not stop.

**10b — Transition issue to "In Progress"**

Fetch available transitions:
```
curl -s -u "<JIRA_EMAIL>:<JIRA_TOKEN>" \
  "<JIRA_URL>/rest/api/3/issue/<ISSUE_KEY>/transitions"
```

Find the transition named `"In Progress"`. If found, apply it:
```
curl -s -u "<JIRA_EMAIL>:<JIRA_TOKEN>" \
  -X POST \
  -H "Content-Type: application/json" \
  "<JIRA_URL>/rest/api/3/issue/<ISSUE_KEY>/transitions" \
  -d '{"transition": {"id": "<TRANSITION_ID>"}}'
```

If no match or call fails, skip silently.

---

**Step 11 — Report**
Print a summary:
```
✅ Done — <ISSUE-KEY>: <summary>

Branch:  feature/<ISSUE-KEY>-<slug>
Commit:  <COMMIT_SHA>
         <COMMIT_URL>
PR:      <PR_URL>
Jira:    <JIRA_URL>/browse/<ISSUE-KEY>  (status → In Progress)

Files created:
  - <file1>
  - <file2>

Registration snippets to add to api_list.xml / services.xml:
<paste snippets here>
```
