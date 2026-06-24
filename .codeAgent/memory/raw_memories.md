# Raw Memories

Raw memory entries are stored as one Markdown file per source under `raw_memories/`.

This file is intentionally stable and does not list entries. Keeping the dynamic data in path-addressed files makes cross-machine Git merges line-independent: two machines that learn different sources add different files instead of rewriting one aggregate document.

File format for `raw_memories/<source-id>.md`:

```text
source_id: <stable source id>
updated_at: <ISO-8601 timestamp>
cwd: <workspace>
rollout_summary_file: rollout_summaries/<slug>.md

<raw memory markdown>
```
