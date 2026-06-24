source_id: external-codex-9ec7de335dbb943f
updated_at: 2026-04-08T12:05:43.1146036+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/modeauction-propskillstips-view.md

## Task group
Generate Lua from prefab annotation for `ModeAuction` UI.

## Scope / applicability
Applies when adding lightweight prefab-backed Lua Views in this project, especially under `mlf/modulesMC/ModeAuction`, using the `$generate-lua-from-prefab` skill and `ModeAuction` style.

## Search keywords
`generate-lua-from-prefab`, `ModeAuction`, `UI_PropSkillsTips_Auction`, `UI_PropSkillsTips_Auction_View`, `UI_PropSkillsTips_Auction_Helper.lua`, `FRAME_UI_PropSkillsTips_Auction`, `UIDefineMC.lua`, `ModulesConf.lua`, `Base_View`, `UIPrioType.Prio_TopLayer`.

## Input evidence
User provided:
- Annotation file path: `<absolute-path>/UI/UI_PropSkillsTips_Auction_Helper.lua`
- `className`: `UI_PropSkillsTips_Auction_View`
- `prefab名字`: `UI_PropSkillsTips_Auction`
- `生成lua路径`: `<absolute-path>/ModeAuction`
- Reference module: `mlf/modulesMC/ModeAuction`

## Reusable decisions / implementation details
- Created `ModeAuction/UI_PropSkillsTips_Auction_View.lua` as a lightweight `Base_View` style panel.
- Annotation fields found were only two `UILabel`s: `m_Label_name` and `m_Label_des`; treat it as a simple tips panel.
- `Active(args)` behavior:
  - Reads display name from aliases like `name`, `skillName`, `title`.
  - Reads description from aliases like `des`, `desc`, `skillDesc`, `content`.
  - If caller passes a raw string, use it as description fallback to avoid indexing errors.
  - Handles non-table/string/number args defensively to avoid runtime index failures.
  - Supports optional `pos` for positioning.
- Did not add close button/business events because only prefab annotation was supplied, no interaction document.
- Added Unity `.meta` for the new Lua resource.

## Registration points
- Added panel ID in `lua/UIDefineMC.lua`:
  - `FRAME_UI_PropSkillsTips_Auction = 50431`
  - Inserted before `FRAME_MCAPP_END_INDEX` / in the relevant MC UI define section.
- Added prefab view config in `mlf/ModulesConf.lua` under `ModeAuction.views`:
  - Prefab/view name: `UI_PropSkillsTips_Auction`
  - Priority: `UIPrioType.Prio_TopLayer`

## Guardrails / failure notes
- Use minimal insertion in `UIDefineMC.lua` and only the `ModeAuction` section of `ModulesConf.lua`; avoid broad registry rewrites.
- Current environment lacked `lua`/`luac`, so only static inspection was done; do not assume interpreter syntax validation was run.
- Keep this panel dependency-light; if later adding behavior, preserve external-data-driven tips usage.

## Source clues
- Existing `UI_BattleTips_Auction_View` in `ModeAuction` was used as evidence that tips panels in this module can be very thin.
- Generic skill tips like `CommanderSkillTip` suggested allowing externally supplied display data and optional anchor/position, but not importing complex config logic for this simple auction tips panel.
