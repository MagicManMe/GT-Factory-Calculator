# GregTech Factory Planner — project context

In-game production planner for GregTech: New Horizons (MC 1.7.10), modeled on
Factorio's Helmod / Factory Planner. Client-side only mod: reads GT recipes live,
computes machine counts / overclocks / EU/t for user-built plans. Owner identity:
**magicmanme** (personal account — never use the euro-wall work identity for
packages, remotes, or authorship).

## Build & test

- `./gradlew build` — jar in `build/libs`; `./gradlew runClient` — dev client
- GTNH buildscript (GTNHGradle): edit only `gradle.properties` / `dependencies.gradle`,
  never `build.gradle.kts`. Java 25 toolchain auto-provisions; Jabel compiles modern
  syntax to Java 8 bytecode.
- Spotless is enforced: run `./gradlew spotlessApply` before committing.
- In-game smoke test: create world, press `P` (keybind opens the planner).

## Architecture (`com.magicmanme.gtfactoryplanner`)

- `data/` — `RecipeIndex`: lazy, session-cached index over GT5U's
  `RecipeMap.ALL_RECIPE_MAPS` → normalized `PlannerRecipe` (chanced outputs stored
  as expected values; 0-amount inputs = circuits/catalysts, not consumed).
  `ItemKey` (Item+meta, NBT ignored) / `FluidKey` (registry name) are the node keys.
- `model/` — `Plan` (targets + ordered lines) → `PlanLine` (recipe + `MachineConfig`:
  voltage tier, parallels, perfectOC, machineHeat).
- `solver/` — `SequentialSolver`: top-down demand propagation (Factory Planner's
  sequential engine). `Overclocks` wraps GT's own `OverclockCalculator` — never
  reimplement OC math. Future: matrix/LP solver for loops & multi-output chains.
- `client/` — `PlannerState` (static session plan + solve result), `SearchCatalog`
  (precomputed lowercase product names), `gui/` (ModularUI2 screens).

## Design rules

- Recipes are ALWAYS user-chosen, never auto-picked (GT items have many recipes;
  auto-picking is what breaks generic calculators).
- Read recipe data live from the GT API — no JSON dumps, no transcription.
- Numeric updates (rate, tier) refresh live via `IKey.dynamicKey`; structural
  changes (add/remove lines) rebuild via `PlannerScreen.reopen()`.

## Hard-won API gotchas (do not rediscover these)

1. **MUI2 `ModularScreen(owner)` calls the virtual `buildUI()` inside its
   constructor** — subclass fields are still null there. For parameterized screens,
   extend `ModularScreen` and pass `super(owner, context -> buildPanel(params))`
   where the lambda captures constructor PARAMETERS (locals), never fields.
   `CustomModularScreen` is only safe with zero instance-field reliance.
2. MUI2 1.7.10 (GTNewHorizons/ModularUI2, pkg `com.cleanroommc.modularui`):
   client-only screens = `ClientGUI.open(screen)`, no sync managers needed. Use
   plain `value.*` (StringValue/DoubleValue + `.Dynamic`) not `value.sync.*`.
   Best reference: `test/TestGuis.java` in the MUI2 repo.
3. GT5U classes dropped the `GT_` prefix: `RecipeMap`, `GTRecipe`, `GTValues`,
   `OverclockCalculator` (old tutorials are stale). `GTRecipe.mSpecialValue` =
   coil heat (EBF) / fusion start energy / fuel value depending on map.
4. Dynamic list rebuild in MUI2: `ListWidget.removeAll()` + `.child(...)` works;
   don't build 100k `setEnabledIf` rows (too slow) — rebuild capped result lists.
5. PowerShell 5.1 mangles git commit messages containing double quotes — avoid
   `"` inside `-m @'...'@` here-strings.
6. Dev-env log noise that is NOT an error: the splash "crash report" joke
   ("THIS IS NOT A ERROR"), antlr4 jar ASM parse failures, IC2 config
   ParseExceptions, BiblioCraft mixin warnings, dreamcraft "Missing
   dependencies to load X script" lines (scripts for absent pack mods).
7. **NEI shows no GT machine tabs in dev runClient — expected, unfixable-ish,
   cosmetic.** NEI's plugin discovery falls back to a classpath scanner
   (`ClassDiscoverer` for `NEI*Config.class`) in RFG dev and misses GT5U's
   `NEIGTConfig`. GT recipes are still fully present in `ALL_RECIPE_MAPS`
   (which is what we read). NEI works fine in the packaged GTNH instance.
8. Bare GT5U dev ≠ GTNH pack recipes. The pack's recipe layer is
   **NewHorizonsCoreMod ("dreamcraft")** — we include it as
   `runtimeOnlyNonPublishable` so runClient is recipe-accurate (~102k recipes
   vs ~93k without). Example gate this explains: GT5U only outputs HOT ingots
   from EBF when material blast temp > 1750K (`ProcessingDust.java`);
   stainless is 1700K → direct ingot, no hot stage. Final verification of
   recipe behavior belongs in a real GTNH instance (drop the jar in mods/).

## Useful facts

- Dev stack loads 64 mods (GT5U dev jar pulls much of GTNH): index = ~93k recipes /
  165 maps / ~18k products, builds in ~150 ms; search catalog ~60 ms. Lazy build on
  first GUI open is fine.
- Deps pinned in `dependencies.gradle`: GT5-Unofficial 5.09.52.573,
  ModularUI2 2.3.72-1.7.10, GTNHLib 0.11.4, NEI 2.8.100-GTNH (check GTNH Nexus
  maven-metadata for updates).
- Repo: https://github.com/MagicManMe/GT-Factory-Calculator (MIT).

## Roadmap (see README for the full list)

Done: recipe index, sequential solver, planner table UI, NEI-style recipe picker
(machine tabs + icon rows + rich tooltips; play-tested OK), NEI-style token
search ("naq ingot" matches "Naquadah Ingot"), EBF-family coil selection
(coil button on heat-gated lines; heat = coilHeat + 100*(tier-2) via GT's
HeatingCoilLevel enum, feeds OverclockCalculator heat OC/discount) — both
play-tested OK in the dreamcraft dev instance.
Next: plan persistence (NBT/file) → OreDict unification → fuller multiblock
presets (parallels, GT++ speed/EU modifiers) → matrix solver (free/eliminated
byproduct control) → AE2 pattern / Level Maintainer export (novel differentiator).
