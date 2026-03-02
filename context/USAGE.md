# How to Use sterling-knowledge.md

`context/sterling-knowledge.md` is the authoritative Sterling OMS domain knowledge
file for this project. It makes any AI coding agent significantly more effective by
giving it the architecture, patterns, APIs, and conventions it needs upfront —
without hallucinating Sterling-specific details.

---

## Option 1: Feed it as a system prompt to Claude CLI

Pipe the file as a system prompt when starting a Claude CLI session:

```bash
# Single-session usage
claude --system-prompt "$(cat context/sterling-knowledge.md)"

# Or combine it with a task description
claude --system-prompt "$(cat context/sterling-knowledge.md)" \
       "Implement the design doc at designs/implement-order-hold-service.md"
```

**When to use:** One-off tasks, debugging sessions, or when you want Sterling
context only for a specific command.

---

## Option 2: Auto-load via .claude/CLAUDE.md (Claude Code)

The `.claude/CLAUDE.md` file in this repo already instructs Claude Code to read
`context/sterling-knowledge.md` before making any code changes. This means every
Claude Code session in this project automatically has Sterling domain knowledge
loaded.

To verify it's working:
```bash
# Open Claude Code in the project root
claude

# Ask Claude to summarise the Sterling architecture
# — it should answer accurately from sterling-knowledge.md without browsing
> What are the Sterling UE registration steps for createOrder?
```

**When to use:** Day-to-day development with Claude Code. This is the recommended
approach for the team.

---

## Option 3: Use as a RAG source (vector database)

For larger teams or when the knowledge file grows too large for a context window,
chunk it into a vector database and retrieve relevant sections at query time.

### Chunking strategy

Split on `## Section` headers — each section is a semantically coherent unit:

```python
import re

with open("context/sterling-knowledge.md") as f:
    content = f.read()

# Split on level-2 headers
chunks = re.split(r"\n## ", content)
chunks = [c.strip() for c in chunks if c.strip()]

# chunks[0] = intro, chunks[1] = Section A, etc.
# Store each chunk with metadata: {"source": "sterling-knowledge.md", "section": "A"}
```

### Embedding and retrieval

```python
# Example using OpenAI embeddings + a simple vector store
# Adapt to your preferred embedding model and vector DB

import openai, numpy as np

def embed(text):
    resp = openai.embeddings.create(model="text-embedding-3-small", input=text)
    return resp.data[0].embedding

chunk_embeddings = [(chunk, embed(chunk)) for chunk in chunks]

def retrieve(query, top_k=3):
    q_emb = np.array(embed(query))
    scored = [(c, np.dot(q_emb, np.array(e))) for c, e in chunk_embeddings]
    return [c for c, _ in sorted(scored, key=lambda x: -x[1])[:top_k]]

# At inference time, inject the retrieved chunks as additional context
relevant = retrieve("How do I register a User Exit?")
system_prompt = "\n\n".join(relevant)
```

**When to use:** Large repositories with many knowledge files, or when integrating
Sterling context into a multi-domain AI assistant.

---

## Option 4: Use with the implement_design.py script

The `tools/implement_design.py` script already embeds `sterling-knowledge.md` as
the system prompt for the Claude agent that implements design docs:

```bash
# Implement a design doc (Sterling context is baked in automatically)
python tools/implement_design.py designs/implement-order-hold-service.md
```

The script reads `context/sterling-knowledge.md` at runtime, so updating the
knowledge file immediately affects all future agent runs.

---

## Keeping the knowledge file up to date

| When | What to update |
|------|---------------|
| New Sterling API used | Add row to Section C table |
| New project convention established | Update Section E |
| New error code series introduced | Add to Section E error code table |
| Sterling version upgraded | Review Section A and Section D for breaking changes |
| New extension point discovered | Add pattern to Section B |

Treat `sterling-knowledge.md` like a living architecture decision record (ADR):
update it when the project evolves, and commit it alongside the code changes it
describes.
