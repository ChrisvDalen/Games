## What this changes

<!-- One or two sentences. What is different after this lands? -->

## Why

<!-- What problem it solves, or what it adds to the game. -->

## How to see it

<!-- The steps a reviewer takes to watch it work: which map, which key, which
     test. If it is only visible in game, say where to walk. -->

## Checklist

- [ ] `./gradlew build` passes
- [ ] Rule changes are covered by a test that does not open a window
- [ ] No new compiler warnings (`-Xlint:all` is on)
- [ ] Content changes load cleanly — the title screen reports no problems
- [ ] `SaveData.CURRENT_VERSION` bumped and a migration added, if the save shape changed
- [ ] Generated art (`generatePixelFont`, `generateTilesetImage`) re-run and committed, if its source table changed

## Deliberately not done

<!-- Anything you left out on purpose, and why. Optional, but it saves a review
     round when the omission was a decision rather than an oversight. -->
