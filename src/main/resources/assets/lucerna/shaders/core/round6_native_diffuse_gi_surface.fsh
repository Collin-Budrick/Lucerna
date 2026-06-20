#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 0.08;
const float SURFACE_PROJECTION_GAIN = 0.16;
const float MATERIAL_RESPONSE_GAIN = 0.062;
const vec3 MAX_ADDITIVE_PER_DRAW = vec3(0.056, 0.046, 0.024);
const vec3 MIN_SURFACE_RADIANCE = vec3(0.48, 0.36, 0.16);
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec4 sourceSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

vec4 localSurfaceSample(vec2 uv) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    vec4 center = sourceSample(uv) * 0.40;
    vec4 cardinal = (
            sourceSample(uv + vec2(texel.x, 0.0))
            + sourceSample(uv + vec2(-texel.x, 0.0))
            + sourceSample(uv + vec2(0.0, texel.y))
            + sourceSample(uv + vec2(0.0, -texel.y))
    ) * 0.12;
    vec4 diagonal = (
            sourceSample(uv + texel * vec2(1.0, 1.0))
            + sourceSample(uv + texel * vec2(-1.0, 1.0))
            + sourceSample(uv + texel * vec2(1.0, -1.0))
            + sourceSample(uv + texel * vec2(-1.0, -1.0))
    ) * 0.03;
    return center + cardinal + diagonal;
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float sampledSignal(vec4 sampleValue) {
    return clamp(max(sampleValue.a, luminance(sampleValue.rgb) * 18.0), 0.0, 1.0);
}

float chromaSpan(vec3 color) {
    vec3 positiveColor = max(color, vec3(0.0));
    return max(max(positiveColor.r, positiveColor.g), positiveColor.b)
            - min(min(positiveColor.r, positiveColor.g), positiveColor.b);
}

vec4 broadSurfaceSample(vec2 uv) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    vec4 center = localSurfaceSample(uv) * 0.46;
    vec4 nearField = (
            localSurfaceSample(uv + vec2(texel.x * 3.0, 0.0))
            + localSurfaceSample(uv + vec2(-texel.x * 3.0, 0.0))
            + localSurfaceSample(uv + vec2(0.0, texel.y * 3.0))
            + localSurfaceSample(uv + vec2(0.0, -texel.y * 3.0))
    ) * 0.095;
    vec4 surfaceSpread = (
            localSurfaceSample(uv + vec2(0.045, 0.018))
            + localSurfaceSample(uv + vec2(-0.040, 0.026))
            + localSurfaceSample(uv + vec2(0.030, -0.034))
            + localSurfaceSample(uv + vec2(-0.052, -0.020))
    ) * 0.04;
    return center + nearField + surfaceSpread;
}

float materialCueMask(vec2 uv, vec4 surfaceSample) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = sampledSignal(surfaceSample);
    float dx = abs(sampledSignal(localSurfaceSample(uv + vec2(texel.x * 2.0, 0.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(-texel.x * 2.0, 0.0))));
    float dy = abs(sampledSignal(localSurfaceSample(uv + vec2(0.0, texel.y * 2.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(0.0, -texel.y * 2.0))));
    float edgeCue = smoothstep(0.010, 0.090, dx + dy);
    float chroma = chromaSpan(surfaceSample.rgb);
    float materialCue = smoothstep(0.004, 0.055, center + chroma * 1.4);
    return clamp(materialCue * (0.62 + edgeCue * 0.38), 0.0, 1.0);
}

float localGeometryCue(vec2 uv, vec4 surfaceSample) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float left = sampledSignal(localSurfaceSample(uv + vec2(-texel.x * 2.0, 0.0)));
    float right = sampledSignal(localSurfaceSample(uv + vec2(texel.x * 2.0, 0.0)));
    float up = sampledSignal(localSurfaceSample(uv + vec2(0.0, -texel.y * 2.0)));
    float down = sampledSignal(localSurfaceSample(uv + vec2(0.0, texel.y * 2.0)));
    float diagonalA = sampledSignal(localSurfaceSample(uv + texel * vec2(2.0, 2.0)))
            - sampledSignal(localSurfaceSample(uv + texel * vec2(-2.0, -2.0)));
    float diagonalB = sampledSignal(localSurfaceSample(uv + texel * vec2(-2.0, 2.0)))
            - sampledSignal(localSurfaceSample(uv + texel * vec2(2.0, -2.0)));
    float gradient = abs(right - left) + abs(down - up) + abs(diagonalA) * 0.45 + abs(diagonalB) * 0.45;
    float payloadLuma = luminance(surfaceSample.rgb);
    float materialContrast = chromaSpan(surfaceSample.rgb);
    float structure = smoothstep(0.012, 0.15, gradient + materialContrast * 0.75 + payloadLuma * 0.18);
    float flatFieldDamping = mix(0.52, 1.0, structure);
    return clamp(flatFieldDamping, 0.0, 1.0);
}

float sourceSupportMask(vec2 uv, float signal, vec4 surfaceSample) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float left = sampledSignal(localSurfaceSample(uv + vec2(-texel.x * 4.0, 0.0)));
    float right = sampledSignal(localSurfaceSample(uv + vec2(texel.x * 4.0, 0.0)));
    float up = sampledSignal(localSurfaceSample(uv + vec2(0.0, -texel.y * 4.0)));
    float down = sampledSignal(localSurfaceSample(uv + vec2(0.0, texel.y * 4.0)));
    float neighborSupport = max(max(left, right), max(up, down));
    float chroma = chromaSpan(surfaceSample.rgb);
    float sourceDriven = smoothstep(0.018, 0.22, max(signal, neighborSupport * 0.72 + chroma * 0.55));
    float softEdgeGuard = smoothstep(0.005, 0.045, uv.x)
            * smoothstep(0.005, 0.045, uv.y)
            * (1.0 - smoothstep(0.955, 0.995, uv.x))
            * (1.0 - smoothstep(0.955, 0.995, uv.y));
    return clamp(mix(0.18, 1.0, sourceDriven) * softEdgeGuard, 0.0, 1.0);
}

float surfaceResponseBreakup(vec2 uv, float signal, vec4 surfaceSample) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = sampledSignal(surfaceSample);
    float horizontal = abs(sampledSignal(localSurfaceSample(uv + vec2(texel.x * 5.0, 0.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(-texel.x * 5.0, 0.0))));
    float vertical = abs(sampledSignal(localSurfaceSample(uv + vec2(0.0, texel.y * 5.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(0.0, -texel.y * 5.0))));
    float directionalCue = smoothstep(0.006, 0.13, max(horizontal, vertical) + center * 0.24);
    float lowFrequency = sin((uv.x * 17.0) + (uv.y * 9.0) + center * 3.0) * 0.5 + 0.5;
    float microBreakup = mix(0.82, 1.0, lowFrequency) * mix(0.72, 1.0, directionalCue);
    return microBreakup * smoothstep(0.015, 0.20, signal);
}

void main() {
    vec4 nativeLighting = localSurfaceSample(texCoord);
    vec4 broadLighting = broadSurfaceSample(texCoord);
    float localSignal = sampledSignal(nativeLighting);
    float broadSignal = sampledSignal(broadLighting);
    float signal = clamp(localSignal * 0.62 + broadSignal * 0.38, 0.0, 1.0);
    float surfaceMask = sourceSupportMask(texCoord, signal, broadLighting);
    float materialMask = materialCueMask(texCoord, broadLighting);
    float geometryMask = localGeometryCue(texCoord, broadLighting);
    float shapedProjection = surfaceMask * materialMask * geometryMask * surfaceResponseBreakup(texCoord, signal, broadLighting);
    vec3 sourceTint = max(max(broadLighting.rgb, vec3(0.0)), MIN_SURFACE_RADIANCE * signal);
    sourceTint *= mix(vec3(1.0), normalize(sourceTint + vec3(0.0001)) * luminance(sourceTint) * 3.0, materialMask * 0.28);
    vec3 localLight = max(nativeLighting.rgb, vec3(0.0)) * FINAL_COMPOSITE_GAIN * signal * shapedProjection;
    vec3 projectedLight = sourceTint * SURFACE_PROJECTION_GAIN * signal * shapedProjection;
    vec3 materialResponse = MIN_SURFACE_RADIANCE * MATERIAL_RESPONSE_GAIN * signal * shapedProjection * materialMask * geometryMask;

    vec3 additiveLight = min(localLight + projectedLight + materialResponse, MAX_ADDITIVE_PER_DRAW);
    fragColor = vec4(additiveLight, 1.0);
}
