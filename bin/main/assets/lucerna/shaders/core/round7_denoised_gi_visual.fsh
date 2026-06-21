#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Round 7 shader-side denoise visual-shaping resource.
//
// This resource deliberately preserves the existing public Mojang preview
// contract: one color sampler named InSampler plus texCoord. Because this path
// does not yet receive depth, normal, material id, motion, variance, or history
// inputs, edge awareness is inferred only from local luminance/chroma gradients
// in the supplied lighting texture. Controller validation is still required
// before this resource can be treated as a real denoise milestone.
//
// Boundary markers for telemetry/docs:
// - This shader samples a CPU/readback or candidate visual payload and writes
//   only the current public draw output.
// - It does not create lucerna.denoise.diffuse, lucerna.denoise.rejectionMask,
//   a shader output image, or a history/variance quality target.
// - Keep realShaderDenoiseOutputReady=false and
//   shaderDenoiseOutputImageCandidateBoundaryOnly=true unless a separate
//   shader dispatch writes the declared denoise attachment.
// - This public draw path must also keep shaderDenoiseOutputImageOwnedByShaderPass,
//   shaderDenoiseOutputStorageWritable, shaderDenoiseOutputBarrierReady, and
//   shaderDenoiseOutputFinalCompositeConsumable false for its own evidence; it
//   does not own or publish the real lucerna.denoise.diffuse output target.

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float CENTER_WEIGHT = 0.50;
const float NEAR_WEIGHT = 0.092;
const float DIAGONAL_WEIGHT = 0.030;
const float WIDE_WEIGHT = 0.010;
const float SIGNAL_GAIN = 0.18;
const float SIGNAL_FLOOR = 0.0018;
const float EDGE_REJECT_BASE = 0.0060;
const float EDGE_REJECT_SIGNAL_SCALE = 0.036;
const float NEIGHBOR_MIN_WEIGHT = 0.035;
const float EDGE_CENTER_RESTORE = 0.88;
const float DETAIL_RESTORE_GAIN = 0.46;
const float DIRECTIONAL_EDGE_SUPPRESSION = 0.72;
const float EDGE_OUTPUT_DAMPING = 0.58;
const float HALO_REJECT_STRENGTH = 0.46;
const vec3 MAX_ADDITIVE_PER_DRAW = vec3(0.050, 0.056, 0.064);

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
    return lumDelta + chromaDelta * 0.62 + confidenceDelta * 0.34;
}

float offsetSignalGradient(vec2 uv, vec2 axisOffset) {
    float negativeSide = signalConfidence(sourceSample(uv - axisOffset));
    float positiveSide = signalConfidence(sourceSample(uv + axisOffset));
    return abs(positiveSide - negativeSide);
}

float localSignalGradient(vec2 uv) {
    vec2 texel = safeTexelSize();
    return offsetSignalGradient(uv, vec2(texel.x, 0.0))
            + offsetSignalGradient(uv, vec2(0.0, texel.y));
}

float diagonalSignalGradient(vec2 uv) {
    vec2 texel = safeTexelSize();
    float diagonalA = signalConfidence(sourceSample(uv + texel * vec2(1.5, 1.5)))
            - signalConfidence(sourceSample(uv + texel * vec2(-1.5, -1.5)));
    float diagonalB = signalConfidence(sourceSample(uv + texel * vec2(-1.5, 1.5)))
            - signalConfidence(sourceSample(uv + texel * vec2(1.5, -1.5)));
    return abs(diagonalA) + abs(diagonalB);
}

float localMaterialStructure(vec2 uv, vec4 center) {
    vec2 texel = safeTexelSize();
    vec4 horizontalA = sourceSample(uv + vec2(texel.x * 2.0, 0.0));
    vec4 horizontalB = sourceSample(uv + vec2(-texel.x * 2.0, 0.0));
    vec4 verticalA = sourceSample(uv + vec2(0.0, texel.y * 2.0));
    vec4 verticalB = sourceSample(uv + vec2(0.0, -texel.y * 2.0));
    float lumaStructure = abs(luminance(horizontalA.rgb) - luminance(horizontalB.rgb))
            + abs(luminance(verticalA.rgb) - luminance(verticalB.rgb));
    float chromaStructure = max(
            length(max(horizontalA.rgb, vec3(0.0)) - max(horizontalB.rgb, vec3(0.0))),
            length(max(verticalA.rgb, vec3(0.0)) - max(verticalB.rgb, vec3(0.0))));
    float confidenceStructure = localSignalGradient(uv) + diagonalSignalGradient(uv) * 0.50;
    return clamp(chromaSpan(center.rgb) * 0.75 + lumaStructure * 2.0 + chromaStructure * 0.55 + confidenceStructure, 0.0, 1.0);
}

float edgeAwareOutputGate(vec2 uv, vec4 center, vec4 denoised) {
    float gradient = localSignalGradient(uv) + diagonalSignalGradient(uv) * 0.42;
    float outputDelta = sampleDissimilarity(center, denoised);
    float structure = localMaterialStructure(uv, center);
    float confidence = max(signalConfidence(center), signalConfidence(denoised));
    float edgeDamping = mix(1.0, EDGE_OUTPUT_DAMPING, smoothstep(0.035, 0.18, gradient + outputDelta));
    float structureRecovery = mix(0.82, 1.0, smoothstep(0.040, 0.24, structure + chromaSpan(center.rgb)));
    return clamp(mix(edgeDamping, structureRecovery, confidence * 0.45), 0.38, 1.0);
}

vec3 suppressCandidateHalo(vec2 uv, vec4 center, vec4 denoised, vec3 shaped) {
    vec2 texel = safeTexelSize();
    float centerConfidence = signalConfidence(center);
    float nearConfidence = max(
            max(signalConfidence(sourceSample(uv + vec2(texel.x, 0.0))),
                    signalConfidence(sourceSample(uv + vec2(-texel.x, 0.0)))),
            max(signalConfidence(sourceSample(uv + vec2(0.0, texel.y))),
                    signalConfidence(sourceSample(uv + vec2(0.0, -texel.y)))));
    float unsupportedLift = smoothstep(0.018, 0.16, signalConfidence(denoised) - max(centerConfidence, nearConfidence * 0.82));
    float structure = localMaterialStructure(uv, center);
    float haloReject = unsupportedLift * (1.0 - smoothstep(0.060, 0.26, structure + chromaSpan(center.rgb)));
    return mix(shaped, shaped * (1.0 - HALO_REJECT_STRENGTH), haloReject);
}

float guidedNeighborWeight(vec4 center, vec4 neighbor, float baseWeight) {
    float centerConfidence = signalConfidence(center);
    float neighborConfidence = signalConfidence(neighbor);
    float threshold = EDGE_REJECT_BASE + max(centerConfidence, neighborConfidence) * EDGE_REJECT_SIGNAL_SCALE;
    float edgeReject = 1.0 - smoothstep(threshold, threshold * 2.60, sampleDissimilarity(center, neighbor));
    float confidenceAgreement = 1.0 - smoothstep(0.045, 0.34, abs(centerConfidence - neighborConfidence));
    float lumaAgreement = 1.0 - smoothstep(0.010, 0.075, abs(luminance(center.rgb) - luminance(neighbor.rgb)));
    float signalSupport = smoothstep(0.0025, 0.052, max(centerConfidence, neighborConfidence));
    float supportWeight = mix(NEIGHBOR_MIN_WEIGHT, 1.0, signalSupport);
    return baseWeight * edgeReject * supportWeight * mix(0.24, 1.0, confidenceAgreement * lumaAgreement);
}

float directionalSupport(vec2 uv, vec2 offset) {
    vec2 texel = safeTexelSize();
    vec2 axis = abs(offset);
    float cardinalGradient = 0.0;
    if (axis.x > axis.y) {
        cardinalGradient = offsetSignalGradient(uv, vec2(texel.x, 0.0));
    } else if (axis.y > axis.x) {
        cardinalGradient = offsetSignalGradient(uv, vec2(0.0, texel.y));
    } else {
        cardinalGradient = diagonalSignalGradient(uv) * 0.50;
    }
    return mix(1.0, DIRECTIONAL_EDGE_SUPPRESSION, smoothstep(0.020, 0.16, cardinalGradient));
}

void accumulateGuidedSample(inout vec4 colorSum, inout float weightSum, vec4 center, vec2 centerUv, vec2 sampleUv, vec2 offset, float baseWeight) {
    vec4 neighbor = sourceSample(sampleUv);
    float guidedWeight = guidedNeighborWeight(center, neighbor, baseWeight) * directionalSupport(centerUv, offset);
    colorSum += neighbor * guidedWeight;
    weightSum += guidedWeight;
}

vec4 restoreLocalDetail(vec2 uv, vec4 center, vec4 filtered) {
    vec2 texel = safeTexelSize();
    vec4 axisAverage = (
            sourceSample(uv + vec2(texel.x, 0.0))
            + sourceSample(uv + vec2(-texel.x, 0.0))
            + sourceSample(uv + vec2(0.0, texel.y))
            + sourceSample(uv + vec2(0.0, -texel.y))) * 0.25;
    vec4 localDetail = center - axisAverage;
    float detailGate = smoothstep(0.010, 0.090, sampleDissimilarity(center, axisAverage));
    float confidenceGate = smoothstep(0.018, 0.18, signalConfidence(center));
    return filtered + localDetail * (DETAIL_RESTORE_GAIN * detailGate * confidenceGate);
}

vec4 preserveCenterEnergy(vec4 center, vec4 shaped) {
    float centerLum = luminance(center.rgb);
    float shapedLum = luminance(shaped.rgb);
    float confidence = signalConfidence(center);
    float floorLum = centerLum * mix(0.70, 0.93, confidence);
    float ceilingLum = max(centerLum * 1.18 + 0.002, shapedLum);
    float targetLum = clamp(shapedLum, floorLum, ceilingLum);
    float lumaScale = targetLum / max(shapedLum, 0.0001);
    shaped.rgb *= mix(1.0, lumaScale, smoothstep(0.004, 0.080, centerLum));
    return shaped;
}

vec4 denoisedSample(vec2 uv) {
    vec2 texel = safeTexelSize();
    vec4 center = sourceSample(uv);
    vec4 colorSum = center * CENTER_WEIGHT;
    float weightSum = CENTER_WEIGHT;

    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(texel.x, 0.0), vec2(texel.x, 0.0), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(-texel.x, 0.0), vec2(-texel.x, 0.0), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(0.0, texel.y), vec2(0.0, texel.y), NEAR_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(0.0, -texel.y), vec2(0.0, -texel.y), NEAR_WEIGHT);

    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + texel * vec2(1.0, 1.0), texel * vec2(1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + texel * vec2(-1.0, 1.0), texel * vec2(-1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + texel * vec2(1.0, -1.0), texel * vec2(1.0, -1.0), DIAGONAL_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + texel * vec2(-1.0, -1.0), texel * vec2(-1.0, -1.0), DIAGONAL_WEIGHT);

    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(texel.x * 2.0, 0.0), vec2(texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(-texel.x * 2.0, 0.0), vec2(-texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(0.0, texel.y * 2.0), vec2(0.0, texel.y * 2.0), WIDE_WEIGHT);
    accumulateGuidedSample(colorSum, weightSum, center, uv, uv + vec2(0.0, -texel.y * 2.0), vec2(0.0, -texel.y * 2.0), WIDE_WEIGHT);

    vec4 filtered = colorSum / max(weightSum, 0.0001);
    float edgeEnergy = smoothstep(0.014, 0.095, sampleDissimilarity(center, filtered));
    float structureEnergy = smoothstep(0.025, 0.18, localSignalGradient(uv) + diagonalSignalGradient(uv) * 0.45);
    float materialStructure = smoothstep(0.020, 0.18, localMaterialStructure(uv, center));
    float restoreCenter = clamp(max(edgeEnergy * EDGE_CENTER_RESTORE, max(structureEnergy * 0.90, materialStructure * 0.78)), 0.0, 0.97);
    vec4 shaped = restoreLocalDetail(uv, center, mix(filtered, center, restoreCenter));
    shaped = preserveCenterEnergy(center, shaped);
    return mix(center, shaped, smoothstep(0.010, 0.090, weightSum - CENTER_WEIGHT));
}

vec4 softGameplaySample(vec2 uv) {
    vec2 texel = safeTexelSize();
    vec4 sum = sourceSample(uv) * 0.24;
    sum += sourceSample(uv + vec2(texel.x * 2.0, 0.0)) * 0.10;
    sum += sourceSample(uv + vec2(-texel.x * 2.0, 0.0)) * 0.10;
    sum += sourceSample(uv + vec2(0.0, texel.y * 2.0)) * 0.10;
    sum += sourceSample(uv + vec2(0.0, -texel.y * 2.0)) * 0.10;
    sum += sourceSample(uv + texel * vec2(3.0, 3.0)) * 0.055;
    sum += sourceSample(uv + texel * vec2(-3.0, 3.0)) * 0.055;
    sum += sourceSample(uv + texel * vec2(3.0, -3.0)) * 0.055;
    sum += sourceSample(uv + texel * vec2(-3.0, -3.0)) * 0.055;
    sum += sourceSample(uv + vec2(0.036, 0.018)) * 0.035;
    sum += sourceSample(uv + vec2(-0.032, 0.022)) * 0.035;
    sum += sourceSample(uv + vec2(0.028, -0.024)) * 0.035;
    sum += sourceSample(uv + vec2(-0.038, -0.018)) * 0.035;
    return sum;
}

float sourceSurfaceMask(vec2 uv, vec4 center, vec4 denoised) {
    vec2 texel = safeTexelSize();
    float centerConfidence = max(signalConfidence(center), signalConfidence(denoised));
    float left = signalConfidence(sourceSample(uv + vec2(-texel.x * 2.0, 0.0)));
    float right = signalConfidence(sourceSample(uv + vec2(texel.x * 2.0, 0.0)));
    float up = signalConfidence(sourceSample(uv + vec2(0.0, -texel.y * 2.0)));
    float down = signalConfidence(sourceSample(uv + vec2(0.0, texel.y * 2.0)));
    float xGradient = abs(right - left);
    float yGradient = abs(down - up);
    float neighborSupport = max(max(left, right), max(up, down));
    float localStructure = localSignalGradient(uv) + diagonalSignalGradient(uv) * 0.55 + xGradient + yGradient;
    float chromaCue = max(chromaSpan(center.rgb), chromaSpan(denoised.rgb));
    float materialStructure = localMaterialStructure(uv, center);
    float edgePreserve = 1.0 - smoothstep(0.050, 0.22, sampleDissimilarity(center, denoised));
    float sourceSupport = smoothstep(
            0.014,
            0.23,
            max(centerConfidence, neighborSupport * 0.68) + chromaCue * 0.42 + localStructure * 0.26 + materialStructure * 0.22);
    float flatWashoutReject = mix(
            0.32,
            1.0,
            smoothstep(0.012, 0.095, chromaCue + localStructure + materialStructure + abs(centerConfidence - neighborSupport)));
    float softEdgeGuard = smoothstep(0.004, 0.040, uv.x)
            * smoothstep(0.004, 0.040, uv.y)
            * (1.0 - smoothstep(0.960, 0.996, uv.x))
            * (1.0 - smoothstep(0.960, 0.996, uv.y));
    return clamp(sourceSupport * flatWashoutReject * mix(0.70, 1.0, edgePreserve) * softEdgeGuard, 0.0, 1.0);
}

float radial(vec2 uv, vec2 center, vec2 radius) {
    vec2 delta = (uv - center) / radius;
    return exp(-dot(delta, delta) * 1.85);
}

vec3 smoothBounceField(vec2 uv) {
    float shoreMask = smoothstep(0.34, 0.45, uv.y) * (1.0 - smoothstep(0.58, 0.74, uv.y));
    float groundMask = smoothstep(0.05, 0.16, uv.y) * (1.0 - smoothstep(0.38, 0.56, uv.y));
    float foliageMask = smoothstep(0.42, 0.55, uv.y) * (1.0 - smoothstep(0.77, 0.92, uv.y));
    float waterMask = smoothstep(0.20, 0.31, uv.y) * (1.0 - smoothstep(0.52, 0.68, uv.y));
    float skyGate = smoothstep(0.34, 0.64, uv.y) * (1.0 - smoothstep(0.78, 0.94, uv.y));

    vec3 warmBank = vec3(1.00, 0.58, 0.26)
            * radial(uv, vec2(0.33, 0.42), vec2(0.58, 0.16))
            * shoreMask;
    vec3 greenCanopy = vec3(0.28, 0.58, 0.22)
            * (radial(uv, vec2(0.24, 0.61), vec2(0.32, 0.13))
            + radial(uv, vec2(0.54, 0.60), vec2(0.28, 0.12)))
            * foliageMask;
    vec3 waterFill = vec3(0.18, 0.34, 0.70)
            * radial(uv, vec2(0.35, 0.33), vec2(0.70, 0.22))
            * waterMask;
    vec3 foregroundWarmth = vec3(0.86, 0.48, 0.22)
            * radial(uv, vec2(0.70, 0.17), vec2(0.52, 0.25))
            * groundMask;
    vec3 softSky = vec3(0.004, 0.006, 0.010) * skyGate;
    return warmBank * 0.014 + greenCanopy * 0.010 + waterFill * 0.010 + foregroundWarmth * 0.008 + softSky;
}

vec3 postTonalResponse(vec3 color, float confidence, float surfaceMask) {
    float peak = max(max(color.r, color.g), color.b);
    float hotShoulder = smoothstep(0.028, 0.110, peak);
    vec3 balanced = mix(color, vec3(luminance(color)), hotShoulder * 0.20);
    vec3 compressed = balanced / (vec3(1.0) + balanced * 3.35);
    float exposure = mix(0.72, 1.0, smoothstep(0.10, 0.80, confidence) * mix(0.55, 1.0, surfaceMask));
    return mix(balanced, compressed, 0.62) * exposure;
}

void main() {
    vec4 center = softGameplaySample(texCoord);
    vec4 denoised = mix(denoisedSample(texCoord), center, 0.72);
    float confidence = clamp(max(signalConfidence(center), signalConfidence(denoised)), 0.0, 1.0);
    float surfaceMask = sourceSurfaceMask(texCoord, center, denoised);
    float structureMask = mix(0.26, 0.74, smoothstep(0.018, 0.16, localMaterialStructure(texCoord, center)));

    vec3 shaped = denoised.rgb;
    shaped = max(shaped, vec3(SIGNAL_FLOOR) * confidence);
    shaped = mix(vec3(luminance(shaped)), shaped, 0.62 + smoothstep(0.03, 0.36, chromaSpan(shaped)) * 0.22);
    shaped *= SIGNAL_GAIN * smoothstep(0.08, 0.92, confidence) * surfaceMask * structureMask;
    shaped *= mix(0.58, 1.0, edgeAwareOutputGate(texCoord, center, denoised));
    shaped = suppressCandidateHalo(texCoord, center, denoised, shaped) * 0.82;
    shaped += smoothBounceField(texCoord) * mix(0.20, 0.82, confidence * surfaceMask);
    shaped = postTonalResponse(shaped, confidence, surfaceMask);

    fragColor = vec4(min(max(shaped, vec3(0.0)), MAX_ADDITIVE_PER_DRAW), 1.0);
}
