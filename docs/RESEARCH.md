# Research notes (condensed from the original design sessions)

Reference material for future milestones — especially the matrix solver and AE2
export. Gathered 2026-06 from primary sources; links inline.

## Factorio prior art: Helmod & Factory Planner

Both are MIT-licensed Lua mods that read recipe data live from the game.

- **Factory Planner** (https://github.com/ClaudeMetz/FactoryPlanner) is the model
  we follow. Two-tier solver in `modfiles/backend/calculation/`:
  - `sequential_engine.lua` — default. Top-down: desired products seed an
    aggregate; per line, production ratio = demand/output (priority product or
    max-ratio when multiple outputs); subfloors solved first then balanced into
    the parent; byproducts net-subtracted from ingredient demand; self-feeding
    fuel handled by recursive demand bump. (Our `SequentialSolver` mirrors this.)
  - `matrix_engine.lua` — opt-in linear solver. Builds **Ax = b**: rows = items,
    cols = recipes (+ pseudo-recipe columns), x = machine counts, b = desired
    output. Solved by Gaussian elimination / RREF (ported from GNU Octave).
    Item classification is the key UX: **free** items (raw inputs, byproducts,
    user-chosen) get pseudo-recipe columns and may have nonzero net; **eliminated**
    items (intermediates) are constrained to net zero. Over-constrained → add free
    vars; under-constrained → user must remove recipes;
    `find_linearly_dependent_cols()` flags the culprits to guide the user.
- **Helmod** (https://github.com/Helfima/helmod): same domain, hierarchical
  production blocks, optional matrix mode. Known weaknesses to do better than:
  no user control over which byproducts to void vs minimize
  (https://github.com/Helfima/helmod/issues/214), fluid temperatures ignored
  (https://github.com/Helfima/helmod/issues/259 — analogous to our coil-heat
  constraint, which we should treat as first-class).
- UX patterns adopted: production-line table, click-ingredient drill-down,
  fractional machine counts, time-base selection, exchange-string export
  (Factory Planner), nested subfloors (planned).

## GTNH prior art (and why this mod exists)

- **gtnh-flow** (https://github.com/OrderedSet86/gtnh-flow) — offline Python
  balancer; recipes hand-typed in YAML; propagation solver; explicitly cannot
  solve parallel-edge/multi-output cases. Our in-game recipe access removes its
  biggest pain (transcription); an LP solver would remove the second.
- **nesql-exporter** (https://github.com/D-Cysteine/nesql-exporter) — dumps GTNH
  data to HSQLDB; powers https://shadowtheage.github.io/gtnh/ (by the YAFC
  author — YAFC uses Google OR-Tools LP for modded Factorio; the proven approach
  for hard recipe graphs: https://github.com/ShadowTheAge/yafc).
- **NotEnoughRecipeDumps** (https://github.com/Hermanoid/NotEnoughRecipeDumps) —
  NEI-scraping JSON dumps; broader but messier than nesql.
- **JECalculation** — generic in-game calculator (1.12+); breaks on GT machines
  (https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/15077);
  its good idea we kept: user supplies the recipe choice, never auto-pick.
- **Web tools**: https://gtnh.samiracle.fr/ (flowchart builder),
  https://divran.github.io/greginator/ (OC calc; exporter broken ≥2.3.1).
- **Unfilled niche** (our differentiators): in-game GT-aware planner; coil heat as
  a constraint; plan → AE2 pattern / ME Level Maintainer export (no tool does
  this; GTNH endgame is AE2-driven — https://wiki.gtnewhorizons.com/wiki/Automation).

## GT mechanics formulas (cross-check against GT5U source, which we call anyway)

- Voltage tiers `GTValues.V[]`: ULV=8, LV=32, MV=128, HV=512, EV=2048, IV=8192,
  LuV=32768, ZPM=131072, UV=524288, then UHV..MAX; names in `GTValues.VN`.
- Overclock (authoritative: `gregtech.api.util.OverclockCalculator`, which
  `solver/Overclocks` delegates to): per OC, power x4; duration /2 (regular) or
  /4 (perfect OC, e.g. LCR). OC count = log4(machinePower / recipePower), clamped
  by voltage-tier difference unless amperage OC.
- EBF coil heat: 5% EU/t discount per 900 K above recipe heat
  (`0.95^floor(delta/900)`); one perfect (/4) OC per 1800 K above, remainder
  regular. Recipe heat lives in `GTRecipe.mSpecialValue`.
- Recipe tier: `max(1, ceil(log4(EUt/8)))`. Machine count for a target rate:
  keep fractional for math, round up for builds (fraction = duty cycle).
- Multiblock parallels multiply runs per cycle (e.g. Mega EBF = 256); GT++ multis
  have machine-specific parallel/speed rules — needs per-machine presets, not a
  single formula (future `MachineConfig` presets milestone).

## Toolchain references

- GTNH dev wiki: https://wiki.gtnewhorizons.com/wiki/Development ·
  GT calc wiki: https://wiki.gtnewhorizons.com/wiki/GT_Calculations
- MUI2 source + the best example file:
  https://github.com/GTNewHorizons/ModularUI2 → `src/main/java/.../test/TestGuis.java`
- Dependency versions: https://nexus.gtnewhorizons.com/repository/public/com/github/GTNewHorizons/<Artifact>/maven-metadata.xml
- GTNH accepts community addons (open-source license required; PR via GitHub /
  Discord #mod-dev); standalone CurseForge/Modrinth publishing is wired into
  `gradle.properties`.
