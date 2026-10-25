# 0.6.2 — films 10–15 and ensemble scores

## Scope

Only the six films for catalogue 10–15 and their local `_score.ogg` recordings are redesigned. The nine invocations, world counterparts, server destruction code, network protocol and End of All are not redesigned. `SpellFilm` keeps its opaque layer, 960×540 maximum render target, controls, music lifetime and real-camera isolation.

## Environments

The old finite block arrangements are replaced by original 4 km terrain fields, atmospheric distance, continuous coast/forest/fault environments, solid urban district traversal and independent camera staging. The `Landscape` sampler is an authored terrain asset, never the Minecraft framebuffer. Six 1024² RGB textures encode integer/fractional altitude and material noise. They are generated deterministically by `tools/generate_cinematic_terrain.py` and require no download at runtime.

- **10 / Last Cataclysm:** city-scale black sun, failing crown, descending fractures, a travelling destructive front and collapsing districts.
- **11 / Verdant Ruin:** branching world-tree with noise-displaced foliage silhouettes, a surrounding forest and a propagating loss of leaves/life. Camera clearance prevents starting inside the terrain.
- **12 / Obsidian Cathedral:** hollow stone nave, spires, buttresses, recessed windows, masonry, approach lamps and opening doors. The camera enters the nave toward an animated circular portal. Doors, portal and rose follow the building's emergence transform.
- **13 / Abyssal Deluge:** coastal city, advancing high wave, foam/ripple shading and submerged streets. Underwater shots retain the city geometry, suspended bubbles and shafts; they do not replace the frame with solid blue.
- **14 / World Sunder:** a continuous landscape tears into a deep fault; walls, depth, strata and exposed magma replace the floating continental tabletop.
- **15 / Rune Apocalypse:** a city-wide overhead court and monoliths; one travelling front removes the city, leaving the landscape and court behind.

Building facades use exact grid traversal and ray/box intersections instead of heightfield steps through vertical walls. Terrain normals are spatially smoothed to avoid stair-step shading, and distant terrain blends into the actual sky instead of exposing the finite trace limit. These are procedural/stylized environments, not photoreal footage. The render cap bounds pixel count, not a universal FPS guarantee.

## Composition and rendering

`tools/generate_v062_scores.py` authors six independent note-level arrangements. Each uses a melodic subject, an eight-bar harmonic arc, cello/bass motion, countermelody, harp/pizzicato figures, choir, timpani/percussion and a timed climax plus resolution. Instruments enter at different stages rather than playing one duplicated line. Program changes and note events are preserved in `docs/compositions_v062.json`.

The recordings use FluidSynth 2.4.6 and GeneralUser GS 1.471 sample-based General MIDI instruments, not the previous additive oscillator voices. They are rendered ensemble mockups, **not a live orchestra**. Only finished Ogg recordings ship; neither FluidSynth nor the SoundFont is needed by players. Duration remains 34/36/38/40/42/45 seconds, stereo, 32 kHz; climax remains tied to the server impact clock. Foley is a separate quiet bed. Two-pass loudness mastering targets −18 LUFS / −2 dBTP, with measured results in `AUDIO_QA_0.6.2.json`.

Local films still use RECORDS. WORLD observers retain the existing PLAYERS charge/impact cues; these are not overlaid on the local score. Skip, camera opt-out and End of All priority are unchanged.

## Reproduction

```sh
python3 tools/generate_cinematic_terrain.py
python3 tools/generate_v062_scores.py \
  --soundfont /path/GeneralUser-GS-1.471.sf2 \
  --library /path/libfluidsynth.so
```

See `docs/third-party/MUSIC_SOURCES.md` and the original SoundFont license for provenance and its sample-origin caveat. New source textures and arrangements are authored for this project. No new dependency is added to the game.
