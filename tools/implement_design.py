#!/usr/bin/env python3
"""
implement_design.py — Sterling OMS Design Doc Implementation Agent

Reads a Sterling design doc, uses the Claude Agents SDK to generate all
required Java source files, then validates the build and commits on success.

Usage:
    python tools/implement_design.py designs/implement-order-hold-service.md

Requirements:
    pip install anthropic
    ANTHROPIC_API_KEY must be set in environment (or .env file).
"""

import argparse
import os
import subprocess
import sys
import textwrap
from pathlib import Path


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent
KNOWLEDGE_FILE = REPO_ROOT / "context" / "sterling-knowledge.md"
MODEL = "claude-opus-4-6"   # Use the most capable model for code generation


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_text(path: Path) -> str:
    """Read a text file and return its contents, or raise FileNotFoundError."""
    if not path.exists():
        raise FileNotFoundError(f"Required file not found: {path}")
    return path.read_text(encoding="utf-8")


def run_cmd(cmd: list[str], cwd: Path | None = None) -> tuple[int, str, str]:
    """Run a subprocess command and return (returncode, stdout, stderr)."""
    result = subprocess.run(
        cmd,
        cwd=cwd or REPO_ROOT,
        capture_output=True,
        text=True,
    )
    return result.returncode, result.stdout, result.stderr


def print_section(title: str, content: str = "") -> None:
    width = 72
    print(f"\n{'=' * width}")
    print(f"  {title}")
    print(f"{'=' * width}")
    if content:
        print(content)


# ---------------------------------------------------------------------------
# System prompt (Sterling domain knowledge baked in)
# ---------------------------------------------------------------------------

def build_system_prompt(knowledge: str) -> str:
    return textwrap.dedent(f"""\
        You are an expert IBM Sterling OMS developer and coding agent.

        Your task is to implement a Sterling OMS feature from a design document.
        You will generate complete, production-ready Java source files.

        ## Sterling Domain Knowledge

        {knowledge}

        ## Output Instructions

        For each file you create, respond with a block in this exact format:

        === FILE: <relative/path/from/repo/root.java> ===
        <complete file contents>
        === END FILE ===

        Rules:
        - Generate COMPLETE files — no truncation, no "// ... rest of code".
        - Follow all Sterling coding patterns from the knowledge above.
        - When Sterling JARs are absent, use org.w3c.dom as a stub but include
          comments showing the real YFCDocument/YFCElement pattern.
        - Include Javadoc on all public methods.
        - Match the package, naming, and error code conventions in Section E.
        - After all files, output a "## Registration" section with the
          api_list.xml / services.xml XML snippets needed to wire up the code.
        """)


# ---------------------------------------------------------------------------
# Agent invocation
# ---------------------------------------------------------------------------

def generate_code(design_doc: str, system_prompt: str) -> str:
    """
    Call the Claude API with the design doc and return the raw response text.
    Uses the Anthropic Python SDK (anthropic>=0.40.0).
    """
    try:
        import anthropic
    except ImportError:
        print("ERROR: anthropic package not installed. Run: pip install anthropic")
        sys.exit(1)

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        # Try loading from .env in repo root
        env_file = REPO_ROOT / ".env"
        if env_file.exists():
            for line in env_file.read_text().splitlines():
                if line.startswith("ANTHROPIC_API_KEY="):
                    api_key = line.split("=", 1)[1].strip().strip('"').strip("'")
                    break
    if not api_key:
        print("ERROR: ANTHROPIC_API_KEY not set. Export it or add it to .env")
        sys.exit(1)

    client = anthropic.Anthropic(api_key=api_key)

    user_message = textwrap.dedent(f"""\
        Please implement all components described in the following Sterling OMS design document.
        Generate complete Java source files for every component listed in "Components to Create".
        Also generate stub unit test classes for every component.

        ## Design Document

        {design_doc}
        """)

    print_section("Calling Claude API", f"Model: {MODEL}\nPrompt tokens: ~{len(system_prompt)//4 + len(user_message)//4}")

    message = client.messages.create(
        model=MODEL,
        max_tokens=8192,
        system=system_prompt,
        messages=[{"role": "user", "content": user_message}],
    )

    return message.content[0].text


# ---------------------------------------------------------------------------
# File extraction
# ---------------------------------------------------------------------------

def extract_files(response: str) -> dict[str, str]:
    """
    Parse the agent response and extract file path → content mappings.
    Expects blocks delimited by:
        === FILE: path/to/File.java ===
        ...content...
        === END FILE ===
    """
    files: dict[str, str] = {}
    lines = response.splitlines()
    current_path: str | None = None
    current_lines: list[str] = []

    for line in lines:
        if line.startswith("=== FILE:") and line.endswith("==="):
            # Start of a new file block
            if current_path:
                files[current_path] = "\n".join(current_lines)
            current_path = line[len("=== FILE:"):].rstrip("===").strip()
            current_lines = []
        elif line.strip() == "=== END FILE ===" and current_path:
            files[current_path] = "\n".join(current_lines)
            current_path = None
            current_lines = []
        elif current_path is not None:
            current_lines.append(line)

    return files


def write_files(files: dict[str, str]) -> list[Path]:
    """Write extracted files to disk, creating parent directories as needed."""
    written: list[Path] = []
    for rel_path, content in files.items():
        dest = REPO_ROOT / rel_path
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(content, encoding="utf-8")
        written.append(dest)
        print(f"  wrote: {rel_path}")
    return written


# ---------------------------------------------------------------------------
# Build validation
# ---------------------------------------------------------------------------

def run_build() -> bool:
    """Run mvn compile and return True on success."""
    pom = REPO_ROOT / "pom.xml"
    if not pom.exists():
        print("  SKIP: no pom.xml found — skipping mvn compile.")
        return True

    print_section("Running mvn compile")
    code, stdout, stderr = run_cmd(["mvn", "compile", "-q"], cwd=REPO_ROOT)
    if code == 0:
        print("  BUILD SUCCESS")
        return True
    else:
        print("  BUILD FAILED")
        print(stdout[-3000:] if len(stdout) > 3000 else stdout)
        print(stderr[-3000:] if len(stderr) > 3000 else stderr)
        return False


# ---------------------------------------------------------------------------
# Git commit
# ---------------------------------------------------------------------------

def git_commit(design_doc_path: Path, written_files: list[Path]) -> bool:
    """Stage the generated files and commit them."""
    print_section("Git commit")

    # Stage each written file
    for f in written_files:
        code, _, stderr = run_cmd(["git", "add", str(f.relative_to(REPO_ROOT))])
        if code != 0:
            print(f"  WARNING: git add failed for {f}: {stderr}")

    feature_name = design_doc_path.stem  # e.g. "implement-order-hold-service"
    commit_msg = (
        f"feat: implement {feature_name}\n\n"
        f"Auto-generated by implement_design.py from {design_doc_path.name}\n\n"
        f"Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
    )

    code, stdout, stderr = run_cmd(["git", "commit", "-m", commit_msg])
    if code == 0:
        print("  Committed successfully.")
        print(stdout.strip())
        return True
    else:
        print(f"  Commit failed: {stderr.strip()}")
        return False


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate Sterling OMS code from a design doc using Claude."
    )
    parser.add_argument(
        "design_doc",
        type=Path,
        help="Path to the design doc Markdown file (e.g. designs/implement-order-hold-service.md)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the agent response but do not write files or commit.",
    )
    parser.add_argument(
        "--no-commit",
        action="store_true",
        help="Write files and build, but do not git commit.",
    )
    args = parser.parse_args()

    # Resolve design doc path
    design_doc_path = args.design_doc
    if not design_doc_path.is_absolute():
        design_doc_path = Path.cwd() / design_doc_path
    if not design_doc_path.exists():
        print(f"ERROR: Design doc not found: {design_doc_path}")
        sys.exit(1)

    print_section("Sterling OMS Design Doc Implementor")
    print(f"  Design doc : {design_doc_path}")
    print(f"  Repo root  : {REPO_ROOT}")
    print(f"  Dry run    : {args.dry_run}")

    # Load files
    design_doc = load_text(design_doc_path)
    knowledge = load_text(KNOWLEDGE_FILE)
    system_prompt = build_system_prompt(knowledge)

    # Generate code
    response = generate_code(design_doc, system_prompt)

    if args.dry_run:
        print_section("Agent Response (dry run — no files written)")
        print(response)
        return

    # Extract and write files
    print_section("Extracting and writing files")
    files = extract_files(response)
    if not files:
        print("WARNING: No files found in agent response.")
        print("Raw response (first 2000 chars):")
        print(response[:2000])
        sys.exit(1)

    written = write_files(files)

    # Build
    build_ok = run_build()

    # Commit
    if build_ok and not args.no_commit:
        git_commit(design_doc_path, written)
    elif not build_ok:
        print("\nSkipping commit due to build failure. Fix compile errors and commit manually.")
        sys.exit(1)

    print_section("Done", f"Generated {len(written)} file(s).")


if __name__ == "__main__":
    main()
