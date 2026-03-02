# Claude Code Instructions — Sterling OMS Project

## 1. Always load Sterling domain knowledge first

Before making **any** code changes in this project, read the full Sterling knowledge base:

```
context/sterling-knowledge.md
```

This file contains:
- Sterling OMS architecture (APIs, Services, User Exits, Agents, Pipelines)
- Canonical coding patterns for Custom APIs and User Exits
- XML handling rules (`YFCDocument`/`YFCElement` — never raw DOM)
- All common APIs with input/output XML structure
- Configuration reference (`yfs.properties`, `api_list.xml`, `services.xml`)
- Project-specific conventions (packages, naming, error codes, logging)

If you skip reading it, you will likely generate code that violates Sterling
patterns and requires a full rewrite.

## 2. Sterling coding patterns (summary — full detail in context file)

- **XML**: Always use `YFCDocument`/`YFCElement`. When Sterling JARs are absent,
  use `org.w3c.dom` as a stub but add comments showing the real Sterling pattern.
- **Attribute access**: `YFCElement.getAttribute()` returns `""` not `null` — always
  check for empty string.
- **Error codes**: Format is `MYCO_<MODULE>_<NNN>`. Never use free-form strings.
- **Logging**: ERROR for exceptions, WARN for missing optional data, INFO for
  significant events, DEBUG for traces. Always include the error code in the message.
- **Fail strategy**: UEs are fail-open by default. Services may be fail-closed.
  Match what the design doc says.
- **No raw DB access**: All data access via Sterling APIs only.

## 3. Implementing a design doc

When asked to implement a feature from a design doc:

1. Read the design doc fully before writing any code.
2. Note the **Type** (Custom API / UE / Service / Agent).
3. Note all **Components to Create** and **Components to Modify**.
4. Implement each component following the patterns in `context/sterling-knowledge.md`.
5. Add registration stubs (comments) for `api_list.xml`/`services.xml` entries.
6. Write unit test stubs that cover the test matrix from the design doc.
7. Run `mvn compile` to validate no compile errors before presenting the results.

## 4. Project conventions

| Item | Convention |
|------|-----------|
| Package root | `com.mycompany.sterling` |
| Custom API suffix | `*API` |
| User Exit suffix | `*UE` |
| Service suffix | `*Service` |
| Agent suffix | `*Agent` |
| Utility suffix | `*Helper` or `*Util` |
| Error code format | `MYCO_<MODULE>_<NNN>` |
| Property prefix | `mycompany.<module>.<property>` |

## 5. Do not

- Use raw `org.w3c.dom.Element.getAttribute()` without a comment showing the YFC equivalent.
- Call Sterling APIs without null-checking the returned `Document`.
- Throw exceptions from a UE without confirming fail-closed is the intended strategy.
- Add Sterling JARs to the POM — they are marked `provided` and come from the server classpath.
- Commit generated code without running `mvn compile` first.
