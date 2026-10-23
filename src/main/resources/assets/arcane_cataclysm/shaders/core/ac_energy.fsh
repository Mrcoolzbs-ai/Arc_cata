#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
// Volumetric-looking energy surfaces (spheres, beams, blades, walls).
// FxParams: x = mode, y = scroll speed, z = noise scale, w = extra
uniform vec4 ColorModulator;
uniform vec4 FxParams;
uniform vec4 FxColor2;
uniform float FxTime;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 viewNormal;
in vec3 viewDir;
in vec3 worldPos;
out vec4 fragColor;

void main() {
    int mode = int(FxParams.x + 0.5);
    float facing = abs(dot(normalize(viewNormal), normalize(viewDir)));
    float fres = 1.0 - facing;
    vec3 col = vertexColor.rgb;
    float a = 0.0;
    float k = max(FxParams.z, 0.1);
    if (mode == 0) {
        // Plasma (suns, meteors, fireballs)
        vec3 q = acWrap(texCoord, k);
        q.z = texCoord.y * k * 2.0 - FxTime * FxParams.y;
        float n = acFbm(q * 1.5 + vec3(FxTime * 0.2));
        float n2 = acFbm(q * 3.7 - vec3(FxTime * 0.35));
        float heat = clamp(n * 0.75 + n2 * 0.5, 0.0, 1.0);
        col = mix(FxColor2.rgb, vertexColor.rgb, smoothstep(0.25, 0.7, heat));
        col = mix(col, vec3(1.0, 0.98, 0.9), smoothstep(0.72, 0.95, heat) * facing);
        col *= .35 + .65 * facing;
        col += vertexColor.rgb * pow(fres, 4.0) * .4;
        a = 1.0;
        a *= mix(1.0, 0.35 + facing, FxParams.w);
        a += pow(fres, 3.0) * 0.6;
    } else if (mode == 1) {
        // Void sphere: black core, bright distorted rim (alpha-blended)
        float rim = pow(fres, 2.2);
        float n = acFbm(acWrap(texCoord, k) * 2.0 + vec3(0.0, 0.0, FxTime * FxParams.y));
        col = mix(vec3(0.0), vertexColor.rgb * 1.6, smoothstep(0.55, 1.0, rim + n * 0.25));
        col += FxColor2.rgb * pow(rim, 6.0) * 1.5;
        a = 1.0;
    } else if (mode == 2) {
        // Crystal / ice: faceted sparkle with fresnel
        float n = acNoise(floor(worldPos * 0.6) + vec3(FxParams.w));
        float spark = pow(acNoise(worldPos * 2.0 + vec3(FxTime * 2.0)), 8.0) * 3.0;
        col = mix(vertexColor.rgb, FxColor2.rgb, n);
        a = 0.18 + pow(fres, 2.0) * 0.9 + spark;
        col = mix(col, vec3(1.0), clamp(spark, 0.0, 1.0));
    } else if (mode == 3) {
        // Beam: hot core where facing the camera, scrolling energy along length
        float n = acFbm(acWrap(texCoord, k) * vec3(1.0, 1.0, 0.06) + vec3(0.0, 0.0, -FxTime * FxParams.y));
        float core = pow(facing, 2.5);
        col = mix(vertexColor.rgb, vec3(1.0), core * 0.9);
        col = mix(col, FxColor2.rgb, (1.0 - core) * n * 0.6);
        a = core * 0.9 + 0.25 + n * 0.45 * (1.0 - core);
        a *= 1.0 - smoothstep(0.7, 1.0, fres) * 0.6;
    } else if (mode == 4) {
        // Spectral blade: bright edges, faint body, runic flicker
        float lines = step(0.94, fract(texCoord.y * 14.0 - FxTime * 1.5)) * 0.5;
        col = mix(vertexColor.rgb, FxColor2.rgb, fres);
        col = mix(col, vec3(1.0), pow(fres, 4.0));
        a = 0.25 + pow(fres, 1.5) * 0.85 + lines;
    } else if (mode == 6) {
        // Opaque ritual masonry: broad material variation, directional form and sparse emissive seams.
        float n=acFbm(worldPos*.04);
        float lit=.45+.55*abs(dot(normalize(viewNormal),normalize(vec3(-.4,.8,.6))));
        col=vertexColor.rgb*(.45+.55*n)*lit;
        float seam=pow(max(0.,1.-abs(sin(worldPos.y*.3))),34.0);
        col+=FxColor2.rgb*seam*.45;
        a=1.0;
    } else {
        // Wall band (vertical shock/fire wall): v=0 bottom, v=1 top
        vec3 q = acWrap(texCoord, k) + vec3(0.0, 0.0, -FxTime * FxParams.y);
        float n = acFbm(q * vec3(1.0, 1.0, 2.0));
        float fade = pow(1.0 - texCoord.y, 1.6) * smoothstep(0.0, 0.08, texCoord.y + 0.02);
        float flame = smoothstep(texCoord.y * 0.9, texCoord.y * 0.9 + 0.35, n);
        col = mix(FxColor2.rgb, vertexColor.rgb, n);
        col = mix(col, vec3(1.0), pow(1.0 - texCoord.y, 6.0) * 0.6);
        a = fade * (0.35 + 0.65 * mix(1.0, flame, FxParams.w));
    }
    a = clamp(a, 0.0, 1.0) * vertexColor.a;
    if (a < 0.002) discard;
    fragColor = vec4(col, a) * ColorModulator;
}
