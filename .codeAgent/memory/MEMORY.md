# Workspace Memory

This file is a stable entrypoint. Dynamic memory content is stored in path-addressed Markdown files so cross-machine Git merges add or update independent files instead of rewriting one aggregate document.

## Scope
- Current workspace decisions, user preferences, reusable procedures, and failure shields extracted from prior local conversations.
- Treat Memory as prior local project context, not confirmed-current truth; verify drift-prone facts against the current repo before acting.

## Stable Layout
- `memory_summary.md`: lightweight startup index injected into prompts.
- `rollout_summaries/`: one file per summarized source, optimized for search and citations.
- `raw_memories/`: one file per source with raw stage-1 memory markdown.
- `raw_memories.md`: stable format note for the raw memory directory.
- `extensions/ad_hoc/`: local write-back notes and processed note evidence.

## Retrieval Flow
1. Read `memory_summary.md` first to decide whether Memory is relevant.
2. Use `search_memory` with concrete keywords from the current task.
3. Open matching `rollout_summaries/*.md` or `raw_memories/*.md` files only when the summary points to relevant evidence.
4. Prefer current source files, tests, and official docs over stale Memory when facts can drift.
