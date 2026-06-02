# GregTech Factory Planner

An **in-game production planner** for [GregTech: New Horizons](https://github.com/GTNewHorizons) (Minecraft 1.7.10) — the equivalent of Factorio's [Helmod](https://mods.factorio.com/mod/helmod) / [Factory Planner](https://github.com/ClaudeMetz/FactoryPlanner).

Set a target output rate, pick the recipes to use, and get machine counts, overclocks, and EU/t — computed from **live GT recipe data**, not hand-typed YAML or stale web dumps.

## Why

Every existing GTNH planning tool lives outside the game ([gtnh-flow](https://github.com/OrderedSet86/gtnh-flow), web calculators) or isn't GT-aware (JECalculation). Being an in-game mod means:

- **Zero recipe transcription** — we read `RecipeMap.ALL_RECIPE_MAPS` directly; whatever the running pack has, the planner has.
- **Exact overclock math** — we call GT's own `OverclockCalculator`, so EU/t and durations match what machines actually do (regular OCs, perfect OCs, EBF coil heat).
- **Client-side only** — works on any server, like NEI.

## Status: usable alpha (0.2.0)

- [x] GTNH ExampleMod-based project, deps on GT5-Unofficial / ModularUI2 / GTNHLib / NEI (+ dreamcraft in the dev runtime for pack-accurate recipes)
- [x] `RecipeIndex` — normalized index of all GT machine recipes (chanced outputs as expected values), with a what-makes-X lookup (~102k recipes, ~150 ms)
- [x] Plan model (`Plan` → `PlanLine` + `MachineConfig`) and **sequential solver** (top-down demand propagation, Factory Planner style)
- [x] ModularUI2 planner UI: target + rate, production-line table (recipe | tier | machines | EU/t), needed-inputs/surplus balance, live totals — keybind `P`
- [x] NEI-style recipe picker: machine tabs, icon rows with amount badges + rich tooltips, click-ingredient drill-down
- [x] NEI-style token search ("naq ingot" finds Naquadah Ingot) in both pickers
- [x] EBF-family coil selection (GT `HeatingCoilLevel`, heat OCs + EU discounts via GT's own `OverclockCalculator`)
- [ ] Plan persistence (NBT/file) + exchange-string export
- [ ] OreDictionary unification of equivalent inputs
- [ ] Fuller multiblock presets (parallels, GT++ speed/EU modifiers, Mega multis)
- [ ] Matrix/LP solver for loops & multi-output chains (platline, distillation), with per-byproduct "void OK / minimize" control
- [ ] NEI integration (open planner from NEI, bookmark sync)
- [ ] Export plan → AE2 patterns / Level Maintainer config

Releases are built by CI on tag push (`git tag X.Y.Z && git push origin X.Y.Z`).

## Architecture

```
com.magicmanme.gtfactoryplanner
├── data/      RecipeIndex over GT's recipe maps; ItemKey/FluidKey/PlannerRecipe
├── model/     Plan, PlanLine, MachineConfig (voltage tier, parallels, heat, ...)
├── solver/    SequentialSolver (demand propagation) + Overclocks (GT OC wrapper)
└── client/    Keybind + GUI
```

The solver design follows Factory Planner's two-tier approach: a fast sequential
tree solver as the default, with a matrix solver (Ax = b over items × recipes)
planned for cyclic/multi-output chains. Recipes are always **user-chosen, never
auto-picked** — GT items routinely have many recipes, and auto-picking is what
breaks generic calculators on GregTech.

## Building

```
./gradlew build        # jar lands in build/libs
./gradlew runClient    # dev client (Java toolchain auto-provisioned)
```

Standard [GTNH buildscript](https://github.com/GTNewHorizons/GTNHGradle); config lives in `gradle.properties` / `dependencies.gradle`.

## License

MIT. Inspired by Helmod (Helfima) and Factory Planner (Therenas), both MIT-licensed Factorio mods.
