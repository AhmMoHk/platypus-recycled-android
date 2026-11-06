# Unity Android port kit (tooling only)

The full kit that is handed to the user also carries files that come from the game and therefore are not kept
here: `patches.json` (exact text edits of the decompiled game code), the reconstructed shaders in
`Overrides/Assets/Shader`, the TextMeshPro shaders, and the touch-control / icon images.

- `setup_project.bat` / `tools/setup.ps1`: find the game, export it with AssetRipper 2.0.0, add the port, patch.
- `build_android.bat` / `tools/build.ps1`: batch-mode Unity build of the APK.
- `Overrides/`: the port's own code (Rewired stand-in, touch controls, mod-file installer, quality presets,
  build script, import rules).
- `dev/`: the Linux compile-check loop used while writing the port (csc against the game's Managed DLLs).
