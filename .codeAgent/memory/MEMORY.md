# Workspace Memory

This file is the stable entrypoint for workspace Memory. Detailed selected inputs and compact rollout summaries remain path-addressed so this file does not accumulate dynamic task content.

## Scope
- Prior workspace decisions, user preferences, reusable procedures, architecture clues, and failure shields extracted from local conversations.
- Treat Memory as prior project context, not confirmed-current truth; verify drift-prone details against the current repository before acting.

## Stable Layout
- `memory_summary.md`: lightweight startup index with retrieval clues.
- `raw_memories.md`: detailed selected phase-2 inputs consolidated by source ID.
- `rollout_summaries/`: compact per-rollout routing summaries.
- `extensions/ad_hoc/`: user-requested write-back notes and their processed evidence.

## Retrieval Flow
1. Read `memory_summary.md` to determine whether prior workspace context is relevant.
2. Search Memory using concrete paths, symbols, commands, or short error phrases from the current task.
3. Open a matching `rollout_summaries/*.md` file for a compact overview.
4. Consult the corresponding section of `raw_memories.md` when detailed semantics, tests, or failure guardrails are needed.
5. Prefer current source files, tests, and official documentation over Memory whenever facts may have drifted.
