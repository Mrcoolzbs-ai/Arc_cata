# 0.6.3 — cinematic polish for films 10–15

## Fix: film score survives the camera toggle

Before 0.6.3 the local `_score.ogg` of a film was stopped as soon as the film stopped being "eligible", and pressing the cinematic-camera key (C) made it ineligible. The picture switched back to the real world and the music disappeared; the world charge cue had already been stopped and the impact cue was suppressed, so the cast became silent.

Now the score belongs to the cast, not to the camera mode:

- Toggling C (off or back on) never stops or restarts a running score.
- The score still stops on skip (V), End of All, spectator mode, logout and at the natural end of the film (`impact + 100` ticks).
- A score is never started late (after 20 ticks), because it would be out of sync with the server impact clock; in that case the normal charge/impact cues play instead.
- The impact cue now plays whenever no score is playing (instead of whenever the film is ineligible), so there is never a silent cast.
- Vanilla music stays suppressed while a film score is playing, even with the camera off.

`StylePreviewQA` checks that the same `SoundInstance` keeps playing across an off/on camera toggle; `test_film_contract.py` guards the logic.

## Graphics

Only `shaders/include/ac_film.glsl` changed; shot timing, cameras, routing, render target cap and terrain textures are the same as 0.6.2.

Shared pipeline:

- Per-film art direction: sun/moon direction and colour, sky palette, fog density/height, exposure.
- Height-integrated aerial fog with forward sun scattering replaces the uniform milky fog, so distance reads in layers.
- Sky: wide Mie glow and sun halo, lit two-sample clouds with bright rims, stars (films 10, 12, 15), moon disk (12), aurora (15).
- Terrain: soft heightfield sun shadows, concavity occlusion, slope/strata/macro-variation materials, detail fade with distance.
- City: buildings cast sun shadows onto streets and each other; three facade families (warm stone, concrete, glass towers with Fresnel sky reflection), floor slabs, pilasters, roof parapets, beacons, foot occlusion.
- Drifting particles per film: embers, pollen/falling leaves, cathedral dust, sea spray, ash, rune sparks.
- Grade: filmic curve, gentle S-curve contrast, saturation, split toning, softer vignette and light film grain.

Per film:

- **10 / Last Cataclysm:** cool shadow fill against the red eclipse light, calmer windows, ember fall.
- **11 / Verdant Ruin:** golden-hour light, lumpier multi-lobe canopies, crowned limbs on every forest tree; withering now goes through autumn gold/red into a dusk sky.
- **12 / Obsidian Cathedral:** moonlit night, low ground mist, stars, lamp halos.
- **13 / Abyssal Deluge:** sunlit wave translucency, foam lighting, distant lightning (disabled when screenFlash=false).
- **14 / World Sunder:** the flat sand plain became ridged green highlands; the fault opens into banded basalt cliffs with glowing cracks and heat haze above the magma.
- **15 / Rune Apocalypse:** deep night palette, aurora, softer court rays.

The cost is higher than 0.6.2 (extra shadow rays), still bounded by the 960×540 film target. These remain procedural/stylised scenes, not photoreal footage.

Offline preview: `python3 tools/tests/render_film_qa.py` → `previews/cinematic-0.6.3/frames`.
