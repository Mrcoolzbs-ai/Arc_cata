#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
// Procedural magic circle. FxParams: x = style seed, y = rotation (radians),
// z = reveal progress (0..1, sweeps around), w = number of star points.
uniform vec4 ColorModulator;
uniform vec4 FxParams;
uniform vec4 FxColor2;
uniform float FxTime;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.2831853;

float line(float d, float w) {
    return exp(-pow(d / w, 2.0));
}
float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}
vec2 rot(vec2 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}
float glyphs(vec2 p, float r0, float r1, float cells, float seed) {
    float r = length(p);
    if (r < r0 || r > r1) return 0.0;
    float ang = atan(p.y, p.x) / TAU + 0.5;
    float cell = floor(ang * cells);
    float fx = fract(ang * cells);
    float fy = (r - r0) / (r1 - r0);
    vec2 g = vec2(fx, fy) * 3.0;
    vec2 gi = floor(g);
    vec2 gf = fract(g);
    float v = 0.0;
    float h = acHash(vec3(cell, gi.x + gi.y * 3.0, seed));
    float hv = acHash(vec3(cell + 31.0, gi.y, seed + 7.0));
    float hh = acHash(vec3(cell + 57.0, gi.x, seed + 3.0));
    if (h > 0.45) v = max(v, line(abs(gf.x - 0.5), 0.09) * step(0.15, gf.y) * step(gf.y, 0.85));
    if (hv > 0.5) v = max(v, line(abs(gf.y - 0.5), 0.09) * step(0.15, gf.x) * step(gf.x, 0.85));
    if (hh > 0.72) v = max(v, line(abs(length(gf - 0.5) - 0.28), 0.07));
    float margin = step(0.12, fx) * step(fx, 0.88);
    return v * margin;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.0) discard;
    float seed = FxParams.x;
    float rotA = FxParams.y;
    float reveal = FxParams.z;
    float points = max(FxParams.w, 3.0);

    float v = 0.0;
    // Outer double ring
    v += line(r - 0.975, 0.008) + line(r - 0.935, 0.005);
    // Rotating glyph band
    vec2 pg = rot(p, rotA);
    v += glyphs(pg, 0.80, 0.925, 28.0, seed) * 0.95;
    v += line(r - 0.795, 0.005);
    // Ticks
    float ang = atan(pg.y, pg.x);
    float ticks = step(0.92, fract(ang / TAU * 96.0)) * step(0.70, r) * step(r, 0.78);
    v += ticks * 0.8;
    v += line(r - 0.695, 0.006);
    // Counter-rotating star polygon
    vec2 ps = rot(p, -rotA * 1.6);
    float step_ = floor(points * 0.5 - 0.5);
    step_ = max(step_, 2.0);
    float sd = 10.0;
    for (int i = 0; i < 9; i++) {
        if (float(i) >= points) break;
        float a0 = TAU * float(i) / points;
        float a1 = TAU * mod(float(i) + step_, points) / points;
        vec2 A = vec2(cos(a0), sin(a0)) * 0.68;
        vec2 B = vec2(cos(a1), sin(a1)) * 0.68;
        sd = min(sd, segDist(ps, A, B));
        // small circles on vertices
        v += line(length(ps - A) - 0.055, 0.006) * 0.9;
    }
    v += line(sd, 0.007);
    // Inner rings + inner glyphs
    vec2 pi2 = rot(p, rotA * 2.3);
    v += glyphs(pi2, 0.30, 0.38, 14.0, seed + 11.0) * 0.8;
    v += line(r - 0.40, 0.005) + line(r - 0.29, 0.005);
    // Center sigil
    float cs = 10.0;
    for (int i = 0; i < 4; i++) {
        float a0 = TAU * float(i) / 4.0 + rotA;
        vec2 A = vec2(cos(a0), sin(a0)) * 0.22;
        cs = min(cs, segDist(p, -A, A));
    }
    v += line(cs, 0.006) * 0.8;
    v += line(r - 0.08, 0.01);
    v += exp(-r * r * 60.0) * 0.6;

    // Soft inner glow fill and shimmer
    float fill = (1.0 - smoothstep(0.0, 1.0, r)) * 0.10;
    float shimmer = 0.75 + 0.25 * sin(FxTime * 3.0 + r * 20.0 - ang * 2.0);

    // Reveal sweep: draws the circle in, like it is being inscribed
    float ra = atan(p.y, p.x) / TAU + 0.5;
    float rv = (1.0 - smoothstep(reveal - 0.04, reveal, ra)) ;
    rv = max(rv, step(1.0, reveal));
    float edgeSpark = exp(-pow((ra - reveal) * 60.0, 2.0)) * step(reveal, 0.999) * smoothstep(0.6, 1.0, r);

    float a = clamp(v * shimmer + fill, 0.0, 1.5) * rv + edgeSpark * 2.0;
    vec3 col = mix(vertexColor.rgb, FxColor2.rgb, clamp(v * 0.35, 0.0, 1.0));
    col = mix(col, vec3(1.0), clamp(edgeSpark, 0.0, 1.0));
    a = clamp(a, 0.0, 1.0) * vertexColor.a;
    if (a < 0.002) discard;
    fragColor = vec4(col, a) * ColorModulator;
}
