# AGENTS.md

## Repo purpose
- This repository contains small coding drafts and examples, including Java, Kotlin, shell scripts, and algorithm practice.
- Keep changes focused and minimal; avoid broad refactors unless explicitly requested.

## Working norms
- Prefer surgical edits in the relevant subproject instead of cross-repo cleanup.
- Read local build files and existing code patterns before changing code.
- Update documentation only when behavior or usage changes.
- Do not modify unrelated sample projects as part of a targeted task.

## Project layout
- `algo/` contains algorithm and problem-solving code.
- `java-only/`, `kotlin-only/`, and `spring-java/` are separate JVM sample projects.
- `shell/` contains utility and automation scripts.

## Validation
- Validate at the smallest useful scope.
- For Maven-based changes, prefer running module-specific commands before repo-wide commands.
- Avoid introducing new tools or frameworks unless the user asks for them.

## Notes for agents
- Check for deeper `AGENTS.md` files before editing inside subdirectories.
- Preserve existing style and naming conventions within each subproject.
