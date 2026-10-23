#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
// Billboards, rings, ribbons, flares. FxParams.x selects the mode.
uniform vec4 ColorModulator;
uniform vec4 FxParams;
uniform vec4 FxColor2;
uniform float FxTime;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    int mode = int(FxParams.x + 0.5);
    vec2 p = texCoord * 2.0 - 1.0;
    float r = length(p);
    vec3 col = vertexColor.rgb;
    float a = 0.0;
    if (mode == 0) {
        // Soft orb with hot core
        float g = exp(-r * r * 4.5);
        float core = exp(-r * r * 40.0);
        a = g * (1.0 - smoothstep(0.85, 1.0, r));
        col = mix(col, vec3(1.0), core * 0.8);
        a = max(a, core);
    } else if (mode == 1) {
        // Shockwave ring. FxParams.y = ring radius (0..1), FxParams.z = thickness
        float d = abs(r - FxParams.y);
        float ring = exp(-pow(d / max(FxParams.z, 0.001), 2.0));
        float ang = atan(p.y, p.x);
        float n = acNoise(vec3(cos(ang) * 6.0, sin(ang) * 6.0, FxTime * 1.5 + r * 8.0));
        ring *= 0.65 + 0.7 * n;
        float inner = (1.0 - smoothstep(0.0, FxParams.y, r)) * 0.18 * FxParams.w;
        a = (ring + inner) * (1.0 - smoothstep(0.96, 1.0, r));
        col = mix(col, FxColor2.rgb, clamp(d / max(FxParams.z, 0.001) * 0.6, 0.0, 1.0));
    } else if (mode == 2) {
        // Star flare: core + anamorphic spikes
        float core = exp(-r * r * 18.0);
        float sx = exp(-abs(p.y) * 60.0) * exp(-abs(p.x) * 2.2);
        float sy = exp(-abs(p.x) * 60.0) * exp(-abs(p.y) * 2.2);
        vec2 q = vec2(p.x + p.y, p.x - p.y) * 0.7071;
        float sd = (exp(-abs(q.y) * 90.0) * exp(-abs(q.x) * 4.0) + exp(-abs(q.x) * 90.0) * exp(-abs(q.y) * 4.0)) * 0.45;
        float halo = exp(-r * r * 3.0) * 0.35;
        a = core + sx + sy * 0.6 + sd + halo;
        col = mix(col, vec3(1.0), clamp(core * 1.2, 0.0, 1.0));
    } else if (mode == 3) {
        // Ribbon: u across width, v along length. FxParams.y = scroll speed, z = noise amount
        float x = abs(texCoord.x * 2.0 - 1.0);
        float core = exp(-x * x * 30.0);
        float glow = exp(-x * x * 4.0) * 0.55;
        float n = acFbm(vec3(texCoord.x * 3.0, texCoord.y * 0.25 - FxTime * FxParams.y, FxTime * 0.7));
        a = (core + glow * (1.0 - FxParams.z + FxParams.z * n * 1.6)) * (1.0 - smoothstep(0.92, 1.0, x));
        col = mix(col, vec3(1.0), core * 0.85);
    } else if (mode == 4) {
        // Smoke puff (alpha blended): noisy soft disc, darker color
        float n = acFbm(vec3(p * 2.2, FxTime * 0.15 + FxParams.y * 13.0));
        a = (1.0 - smoothstep(0.25, 1.0, r + (n - 0.5) * 0.7));
        col = col * (0.6 + 0.5 * n);
    } else if (mode == 5) {
        // Accretion disk / swirling annulus. y = inner radius, z = swirl speed
        float ang = atan(p.y, p.x);
        float band = smoothstep(FxParams.y, FxParams.y + 0.08, r) * (1.0 - smoothstep(0.75, 1.0, r));
        float sw = acFbm(vec3(cos(ang) * 2.5 + r * 4.0, sin(ang) * 2.5 - r * 4.0, FxTime * FxParams.z));
        float spiral = 0.5 + 0.5 * sin(ang * 3.0 + r * 18.0 - FxTime * FxParams.z * 6.0);
        a = band * (0.35 + 0.9 * sw * (0.6 + 0.4 * spiral));
        col = mix(FxColor2.rgb, col, smoothstep(FxParams.y, 0.95, r));
        col = mix(col, vec3(1.0), (1.0 - smoothstep(FxParams.y, FxParams.y + 0.15, r)) * band);
    } else {
        // Ground scorch glow: crackled hot decal
        float n = acFbm(vec3(p * 6.0, FxParams.y));
        float cracks = (1.0 - smoothstep(0.0, 0.08, abs(n - 0.5)));
        a = (cracks * 0.9 + exp(-r * r * 3.0) * 0.4) * (1.0 - smoothstep(0.7, 1.0, r));
    }
    a = clamp(a, 0.0, 1.0) * vertexColor.a;
    if (a < 0.002) discard;
    fragColor = vec4(col, a) * ColorModulator;
}
