# Recorded ensemble instrument sources

Six new local film scores in 0.6.2 use original compositions authored in `tools/generate_v062_scores.py`.

## Instrument bank

- GeneralUser GS 1.471, by S. Christian Collins.
- Author's project: https://schristiancollins.com/generaluser.php
- Versioned mirror used for this render: https://github.com/ROCKNIX/generaluser-gs
- Bank: `GeneralUser GS v1.471.sf2`.
- License: `GeneralUser-GS-LICENSE.txt` in this directory, copied verbatim from the versioned mirror.

The license permits unrestricted music creation, including commercial recordings. It also explicitly documents uncertainty about the provenance of some inherited samples. That caveat is preserved; this project does not claim every historical sample has independently verified provenance. The bank itself is not distributed in the mod. Only the rendered original recordings are included.

## Renderer

FluidSynth 2.4.6 (LGPL), https://github.com/FluidSynth/fluidsynth/releases/tag/v2.4.6, is used offline as a development tool. It is not bundled, linked into, or required by the Minecraft mod. FFmpeg/Vorbis are also offline encoding tools.

Scores use General MIDI strings, cello, horns, trombones, choir, flute, oboe, harp, tubular bells, pizzicato, timpani and orchestral percussion. These recordings are sampled-instrument ensemble mockups, not a live orchestral performance.
