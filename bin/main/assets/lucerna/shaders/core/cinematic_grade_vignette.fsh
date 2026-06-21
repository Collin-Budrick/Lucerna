#version 330

in vec2 texCoord;

out vec4 fragColor;

float radial(vec2 uv, vec2 center, vec2 radius, float falloff) {
    vec2 delta = (uv - center) / radius;
    return exp(-dot(delta, delta) * falloff);
}

float hash21(vec2 p) {
    p = fract(p * vec2(91.7, 241.3));
    p += dot(p, p + 31.9);
    return fract(p.x * p.y);
}

float noise(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

void main() {
    vec2 uv = texCoord;
    vec2 sun = vec2(0.118, 0.885);
    vec2 centered = uv * 2.0 - 1.0;

    float corner = smoothstep(0.55, 1.30, dot(centered, centered));
    float lowerFrame = 1.0 - smoothstep(0.10, 0.42, uv.y);
    float upperShoulder = smoothstep(0.72, 1.00, uv.y);
    float sideFrame = smoothstep(0.70, 1.05, abs(centered.x));
    float sunExclusion = 1.0 - radial(uv, sun, vec2(0.44, 0.32), 2.1);

    float cloudTexture = noise(uv * vec2(5.6, 2.8) + vec2(8.1, 1.7));
    float foliageContrast = smoothstep(0.20, 0.54, uv.y) * (1.0 - smoothstep(0.64, 0.84, uv.y));
    float waterContrast = smoothstep(0.48, 0.66, uv.y) * (1.0 - smoothstep(0.78, 0.92, uv.y));
    float bankWarmth = radial(uv, vec2(0.250, 0.485), vec2(0.520, 0.155), 1.9);
    float grassShadow = radial(uv, vec2(0.770, 0.270), vec2(0.390, 0.210), 1.45);
    float waterDepth = radial(uv, vec2(0.365, 0.405), vec2(0.590, 0.170), 1.55);
    float sunSideLift = radial(uv, vec2(0.150, 0.620), vec2(0.430, 0.250), 1.75);

    float gradeAlpha = 0.0;
    gradeAlpha += corner * 0.030;
    gradeAlpha += lowerFrame * 0.018;
    gradeAlpha += sideFrame * 0.014;
    gradeAlpha += upperShoulder * sunExclusion * 0.008;
    gradeAlpha += foliageContrast * mix(0.006, 0.014, cloudTexture);
    gradeAlpha += waterContrast * 0.007;
    gradeAlpha += grassShadow * 0.010;
    gradeAlpha += waterDepth * 0.007;

    vec3 neutralShoulder = vec3(0.030, 0.033, 0.040);
    vec3 coolShadow = vec3(0.012, 0.018, 0.027);
    vec3 warmMidtone = vec3(0.048, 0.042, 0.032);
    vec3 foliageCool = vec3(0.016, 0.026, 0.024);
    vec3 waterCool = vec3(0.012, 0.024, 0.040);
    vec3 gradeColor = mix(warmMidtone, coolShadow, clamp(corner + lowerFrame * 0.7 + grassShadow * 0.25, 0.0, 1.0));
    gradeColor = mix(gradeColor, neutralShoulder, upperShoulder * sunExclusion * 0.35);
    gradeColor = mix(gradeColor, foliageCool, foliageContrast * 0.20);
    gradeColor = mix(gradeColor, waterCool, waterDepth * 0.24);
    gradeColor += vec3(0.030, 0.026, 0.016) * bankWarmth * (0.28 + sunSideLift * 0.18);

    float topSafety = 1.0 - smoothstep(0.90, 0.995, uv.y);
    fragColor = vec4(gradeColor, clamp(gradeAlpha * topSafety, 0.0, 0.070));
}
