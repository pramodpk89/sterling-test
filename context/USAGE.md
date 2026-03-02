# How to Use sterling-knowledge.md

`context/sterling-knowledge.md` is the authoritative Sterling OMS domain knowledge
file for this project. It makes Claude significantly more effective by giving it
the architecture, patterns, APIs, and conventions it needs upfront — without
hallucinating Sterling-specific details.

---

## Recommended: Claude Code (auto-loaded)

The `.claude/CLAUDE.md` file in this repo instructs Claude Code to read
`context/sterling-knowledge.md` before making any code changes. This means every
Claude Code session in this project automatically has Sterling domain knowledge.

```bash
# Open Claude Code in the project root
claude

# Ask Claude to implement a design doc
> Implement the design doc at designs/implement-order-hold-service.md
```

Claude will read the knowledge file, read the design doc, and generate all the
Java source files following Sterling patterns.

---

## Option: Feed as a system prompt to Claude CLI

For one-off tasks outside Claude Code:

```bash
claude --system-prompt "$(cat context/sterling-knowledge.md)" \
       "Implement the design doc at designs/implement-order-hold-service.md"
```

---

## Option: Use as a RAG source (vector database)

For larger teams or when the knowledge file grows too large for a context window,
chunk it into a vector database and retrieve relevant sections at query time.

Split on `## Section` headers — each section is a semantically coherent unit:

```python
import re

with open("context/sterling-knowledge.md") as f:
    content = f.read()

chunks = re.split(r"\n## ", content)
chunks = [c.strip() for c in chunks if c.strip()]
# Store each chunk with metadata: {"source": "sterling-knowledge.md", "section": "A"}
```

---

## Keeping the knowledge file up to date

| When | What to update |
|------|----------------|
| New Sterling API used | Add row to Section C table |
| New project convention established | Update Section E |
| New error code series introduced | Add to Section E error code table |
| Sterling version upgraded | Review Section A and Section D for breaking changes |
| New extension point discovered | Add pattern to Section B |

Treat `sterling-knowledge.md` like a living architecture decision record (ADR):
update it when the project evolves, and commit it alongside the code changes it describes.
