// Arcane Cataclysm — shared shader helpers
float acHash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.11, 0.17, 0.13));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float acNoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(acHash(i + vec3(0, 0, 0)), acHash(i + vec3(1, 0, 0)), f.x),
                   mix(acHash(i + vec3(0, 1, 0)), acHash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(acHash(i + vec3(0, 0, 1)), acHash(i + vec3(1, 0, 1)), f.x),
                   mix(acHash(i + vec3(0, 1, 1)), acHash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}
float acFbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * acNoise(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return v;
}
// Seamless cylindrical coordinates for UVs where u wraps around (0..1).
vec3 acWrap(vec2 uv, float k) {
    float a = uv.x * 6.2831853;
    return vec3(cos(a) * k, sin(a) * k, uv.y);
}
