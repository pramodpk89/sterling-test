Read `context/sterling-knowledge.md` fully, especially the Code Review Checklist at the end of Section E and all of Section B (Coding Patterns).

Arguments: $ARGUMENTS

If a file path is provided in the arguments, read that file.
If no arguments are given, ask: "Which file should I review? Provide a relative path (e.g. src/main/java/com/mycompany/sterling/ue/MyUE.java)"

Read the target file completely before reviewing.

---

Review the file against EXACTLY these 7 items. Check every single line — do not skip any:

**Item 1 — XML access**
Flag every call to `org.w3c.dom.Element.getAttribute()` that does NOT have a comment immediately above it showing the YFCDocument equivalent pattern. The comment must say something like `// In real Sterling: YFCElement.getAttribute(...)`.

**Item 2 — Empty-string check**
For every `getAttribute` result that is used in a condition or passed to another method, verify it is checked with `"".equals(value)` or `value.isEmpty()` — NOT just `value != null`. Flag any that only null-check.

**Item 3 — Error codes**
Every string passed as the first argument to `YFSException`, and every WARN/ERROR log message, must start with a code matching `MYCO_<MODULE>_<3-digit-number>` (e.g. `MYCO_HOLD_001`). Flag any free-form strings without this prefix.

**Item 4 — Logging levels**
- `SEVERE` / `ERROR`: only for API failures and unexpected exceptions
- `WARNING` / `WARN`: only for missing optional data and skipped rules
- `INFO`: only for significant business events (hold applied, order released, etc.)
- `FINE` / `DEBUG`: for attribute reads and decision traces
Flag any level that does not match its usage.

**Item 5 — API response null check**
For every call to `SterlingAPIHelper.invokeAPI(...)` or `SterlingAPIHelper.invokeAPIFailOpen(...)`, verify the returned `Document` is null-checked before any method is called on it. Flag if missing.

**Item 6 — Fail strategy**
- If the class name ends in `UE`: there must be a top-level `catch (Exception e)` that logs and swallows — never rethrows. Flag if the exception escapes.
- If the class name ends in `API` or `Service`: fail-closed is acceptable only if documented with a comment. Flag undocumented rethrows.

**Item 7 — Forbidden patterns**
Flag immediately if any of these appear:
- Any `import java.sql.*` or JDBC reference
- Any `import javax.persistence.*` or JPA reference
- Any `import com.yantra.*` that is NOT inside a comment (these won't compile without Sterling JARs)

---

Output format — use this structure exactly, no extra commentary:

```
## Sterling Review: <filename>

### PASS
- Item N: <one-line reason>

### FAIL
- Item N — Line <line-number>: `<code snippet>` — <what is wrong> — Fix: <specific fix>

### Summary
<one sentence overall assessment>
```

Only report on the 7 items above. Do not suggest style changes, refactors, or improvements beyond these checks.
