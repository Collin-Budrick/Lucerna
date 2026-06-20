#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 0.08;
const float SURFACE_PROJECTION_GAIN = 0.18;
const float MATERIAL_RESPONSE_GAIN = 0.07;
const vec3 MAX_ADDITIVE_PER_DRAW = vec3(0.065, 0.052, 0.026);
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

float surfaceBoundsMask(vec2 uv) {
    float left = smoothstep(0.08, 0.22, uv.x);
    float right = 1.0 - smoothstep(0.86, 0.98, uv.x);
    float top = smoothstep(0.10, 0.22, uv.y);
    float bottom = 1.0 - smoothstep(0.90, 1.0, uv.y);
    float vertical = 0.58 + smoothstep(0.22, 0.74, uv.y) * 0.34;
    return clamp(left * right * top * bottom * vertical, 0.0, 1.0);
}

float materialCueMask(vec2 uv, vec4 surfaceSample) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = sampledSignal(surfaceSample);
    float dx = abs(sampledSignal(localSurfaceSample(uv + vec2(texel.x * 2.0, 0.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(-texel.x * 2.0, 0.0))));
    float dy = abs(sampledSignal(localSurfaceSample(uv + vec2(0.0, texel.y * 2.0)))
            - sampledSignal(localSurfaceSample(uv + vec2(0.0, -texel.y * 2.0))));
    float edgeCue = smoothstep(0.010, 0.090, dx + dy);
    float chroma = max(max(surfaceSample.r, surfaceSample.g), surfaceSample.b)
            - min(min(surfaceSample.r, surfaceSample.g), surfaceSample.b);
    float materialCue = smoothstep(0.004, 0.055, center + chroma * 1.4);
    return clamp(materialCue * (0.62 + edgeCue * 0.38), 0.0, 1.0);
}

float organicSurfaceBreakup(vec2 uv, float signal) {
    float sweep = sin((uv.x * 19.0) + (uv.y * 11.0)) * 0.5 + 0.5;
    float cross = sin((uv.x - uv.y) * 27.0) * 0.5 + 0.5;
    float lowFrequency = mix(sweep, cross, 0.35);
    return mix(0.76, 1.0, lowFrequency) * smoothstep(0.015, 0.20, signal);
}

void main() {
    vec4 nativeLighting = localSurfaceSample(texCoord);
    vec4 broadLighting = broadSurfaceSample(texCoord);
    float localSignal = sampledSignal(nativeLighting);
    float broadSignal = sampledSignal(broadLighting);
    float signal = clamp(localSignal * 0.62 + broadSignal * 0.38, 0.0, 1.0);
    float surfaceMask = surfaceBoundsMask(texCoord);
    float materialMask = materialCueMask(texCoord, broadLighting);
    float shapedProjection = surfaceMask * materialMask * organicSurfaceBreakup(texCoord, signal);
    vec3 sourceTint = max(max(broadLighting.rgb, vec3(0.0)), MIN_SURFACE_RADIANCE * signal);
    vec3 localLight = max(nativeLighting.rgb, vec3(0.0)) * FINAL_COMPOSITE_GAIN * signal * shapedProjection;
    vec3 projectedLight = sourceTint * SURFACE_PROJECTION_GAIN * signal * shapedProjection;
    vec3 materialResponse = MIN_SURFACE_RADIANCE * MATERIAL_RESPONSE_GAIN * signal * shapedProjection * materialMask;

    vec3 additiveLight = min(localLight + projectedLight + materialResponse, MAX_ADDITIVE_PER_DRAW);
    fragColor = vec4(additiveLight, 1.0);
}
