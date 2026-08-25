# assets/

Intentionally near-empty. Pour Perfect's gameplay is rendered procedurally in `core` (tubes and
liquids are drawn with libGDX `ShapeRenderer`, colors come from `LiquidColor`'s libGDX `Color`
constants) — no textures, fonts, or sound files are required to play the game.

This directory exists as the shared assets root referenced by:
- `android/pom.xml`'s `assetsDirectory` (android-maven-plugin configuration)
- `ios/robovm.xml`'s `<resources>` entry

If real art, sound effects, or a custom font are added later, put them here so both platform
launchers pick them up automatically.
