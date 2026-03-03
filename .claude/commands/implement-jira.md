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

**Step 2 — Fetch the Jira issue**
Run this curl command using the credentials from Step 1:
```
curl -s -u "<JIRA_EMAIL>:<JIRA_TOKEN>" "<JIRA_URL>/rest/api/3/issue/<ISSUE_KEY>"
```

Parse the JSON response. Extract:
- `fields.summary` → feature name
- `fields.description` → full story description (walk the Atlassian Document Format content tree: for each block in `content`, for each node in its `content`, collect `text` values)
- `fields.issuetype.name` → issue type
- `fields.status.name` → current status

If the response contains `errorMessages`, stop and print the error.

**Step 3 — Map story to Sterling component type**
From the description, determine:
- What Java files to create (look for class names, package names, file names mentioned)
- What Sterling component type each file is (User Exit / Custom API / Service / Agent)
- What business logic to implement (rules, conditions, error handling)
- What Sterling APIs are called (changeOrder, getOrderDetails, etc.)
- What error codes to use (look for MYCO_* codes; if none, derive from the feature name)
- Fail strategy (fail-open unless description says otherwise)

**Step 4 — Implement the code**
For each file identified in Step 3:

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

d. Implement ALL business logic from the story description — not a stub.

e. Include the registration snippet (api_list.xml / services.xml) as a Javadoc `<pre>` comment.

f. Place files at the correct path matching the package in the story.

**Step 5 — Compile**
Run `mvn compile`. If it fails, fix the errors and recompile until clean.

**Step 6 — Commit**
Stage only the newly created source files and commit with this message format:
```
feat(<issue-key>): <story summary in lowercase>

<2-3 bullet points summarising what was implemented>

Jira: <JIRA_URL>/browse/<issue-key>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

**Step 7 — Raise a PR**
Run:
```
gh pr create --title "<issue-key>: <story summary>" --body "$(cat <<'EOF'
## Summary
<bullet points from the story>

## Jira
<JIRA_URL>/browse/<issue-key>

## Test plan
- [ ] mvn compile passes
- [ ] Unit tests cover rule boundary conditions
- [ ] Verify Sterling registration stubs are present in Javadoc

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

If `gh` is not installed or not authenticated, skip the PR step and print the PR body so the developer can create it manually.

**Step 8 — Report**
Print:
1. Files created
2. Registration snippets (api_list.xml / services.xml)
3. Link to the PR (or instructions to create it manually)
