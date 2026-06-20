#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Round 7 shader-side denoise shaping resource.
//
// This resource deliberately preserves the existing public Mojang preview
// contract: one color sampler named InSampler plus texCoord. Because this path
// does not yet receive depth, normal, material id, motion, variance, or history
// inputs, edge awareness is inferred only from local luminance/chroma gradients
// in the supplied lighting texture. Controller validation is still required
// before this resource can be treated as a real denoise milestone.

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float CENTER_WEIGHT = 0.30;
const float NEAR_WEIGHT = 0.12;
const float DIAGONAL_WEIGHT = 0.070;
const float WIDE_WEIGHT = 0.045;
const float SIGNAL_GAIN = 0.46;
const float SIGNAL_FLOOR = 0.0025;
const float EDGE_REJECT_BASE = 0.010;
const float EDGE_REJECT_SIGNAL_SCALE = 0.060;
const float NEIGHBOR_MIN_WEIGHT = 0.12;
const float EDGE_CENTER_RESTORE = 0.64;
const vec3 MAX_ADDITIVE_PER_DRAW = vec3(0.030, 0.027, 0.018);

vec2 safeTexelSize() {
    return 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
}

vec4 sourceSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float chromaSpan(vec3 color) {
    vec3 positiveColor = max(color, vec3(0.0));
    return max(max(positiveColor.r, positiveColor.g), positiveColor.b)
            - min(min(positiveColor.r, positiveColor.g), positiveColor.b);
}

float signalConfidence(vec4 sampleValue) {
    float rgbSignal = luminance(sampleValue.rgb);
    float alphaSignal = clamp(sampleValue.a, 0.0, 1.0);
    return clamp(max(alphaSignal, rgbSignal * 24.0), 0.0, 1.0);
}

float sampleDissimilarity(vec4 center, vec4 neighbor) {
    float centerLum = luminance(center.rgb);
    float neighborLum = luminance(neighbor.rgb);
    float lumDelta = abs(centerLum - neighborLum);
    float chromaDelta = length(max(center.rgb, vec3(0.0)) - max(neighbor.rgb, vec3(0.0)));
    float confidenceDelta = abs(signalConfidence(center) - signalConfidence(neighbor));
    return lumDelta + chromaDelta * 0.55 + confidenceDelta * 0.22;
}

float guidedNeighborWeight(vec4 center, vec4 neighbor, float baseWeight) {
    float centerConfidence = signalConfidence(center);
    float neighborConfidence = signalConfidence(neighbor);
    float threshold = EDGE_REJECT_BASE + max(centerConfidence, neighborConfidence) * EDGE_REJECT_SIGNAL_SCALE;
    float edgeReject = 1.0 - smoothstep(threshold, threshold * 3.75, sampleDissimilarity(center, neighbor));
    float signalSupport = smoothstep(0.0015, 0.045, max(centerConfidence, neighborConfidence));
    float supportWeight = mix(NEIGHBOR_MIN_WEIGHT, 1.0, signalSupport);
    return baseWeight * edgeReject * supportWeight;
}

void accumulateGuidedSample(inout vec4 colorSum, inout float weightSum, vec4 center, vec2 uv, float baseWeight) {
    vec4 neighbor = sourceSample(uv);
    float guidedWeight = guidedNeighborWeight(center, neighbor, baseWeight);
    colorSum += neighbor * guidedWeight;
    weightSum += guidedWeight;
}

vec4 denoisedSample(vec2 uv) {
    vec2 texel = safeTexelSize();
    vec4 center = sourceSample(uv);
    vec4 colorSum = center * CENTER_WEIGHT;
    float weightSum = CENTER_WEIGHT;

    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(texel.x, 0.0), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(-texel.x, 0.0), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(0.0, texel.y), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(0.0, -texel.y), NEAR_WEIGHT);

    accumulateGuidedSample(colorSum, weightSum, center, uv + texel * vec2(1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + texel * vec2(-1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + texel * vec2(1.0, -1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + texel * vec2(-1.0, -1.0), DIAGONAL_WEIGHT);

    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(-texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(0.0, texel.y * 2.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv + vec2(0.0, -texel.y * 2.0), WIDE_WEIGHT);

    vec4 filtered = colorSum / max(weightSum, 0.0001);
    float edgeEnergy = smoothstep(0.018, 0.14, sampleDissimilarity(center, filtered));
    float restoreCenter = edgeEnergy * EDGE_CENTER_RESTORE;
    return mix(filtered, center, restoreCenter);
}

float sourceSurfaceMask(vec2 uv, vec4 center, vec4 denoised) {
    vec2 texel = safeTexelSize();
    float centerConfidence = max(signalConfidence(center), signalConfidence(denoised));
    float xGradient = abs(signalConfidence(sourceSample(uv + vec2(texel.x * 2.0, 0.0)))
            - signalConfidence(sourceSample(uv + vec2(-texel.x * 2.0, 0.0))));
    float yGradient = abs(signalConfidence(sourceSample(uv + vec2(0.0, texel.y * 2.0)))
            - signalConfidence(sourceSample(uv + vec2(0.0, -texel.y * 2.0))));
    float chromaCue = max(chromaSpan(center.rgb), chromaSpan(denoised.rgb));
    float sourceSupport = smoothstep(0.012, 0.20, centerConfidence + chromaCue * 0.50 + (xGradient + yGradient) * 0.42);
    float softEdgeGuard = smoothstep(0.004, 0.040, uv.x)
            * smoothstep(0.004, 0.040, uv.y)
            * (1.0 - smoothstep(0.960, 0.996, uv.x))
            * (1.0 - smoothstep(0.960, 0.996, uv.y));
    return clamp(sourceSupport * softEdgeGuard, 0.0, 1.0);
}

void main() {
    vec4 center = sourceSample(texCoord);
    vec4 denoised = denoisedSample(texCoord);
    float confidence = clamp(max(signalConfidence(center), signalConfidence(denoised)), 0.0, 1.0);
    float surfaceMask = sourceSurfaceMask(texCoord, center, denoised);

    vec3 shaped = denoised.rgb;
    shaped = max(shaped, vec3(SIGNAL_FLOOR) * confidence);
    shaped *= SIGNAL_GAIN * confidence * surfaceMask;

    fragColor = vec4(min(max(shaped, vec3(0.0)), MAX_ADDITIVE_PER_DRAW), 1.0);
}
