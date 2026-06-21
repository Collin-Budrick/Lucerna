#version 450

// Shader-generated diffuse denoise fragment resource for lucerna.denoise.diffuse.
//
// This resource declares the stricter semantic sampler/output shape for a
// shader-owned denoise pass with rejection-mask output. The paired
// runtime-loadable public Mojang resource is
// denoise/shader_generated_diffuse_output.fsh; that .fsh writes denoised
// diffuse RGBA into the owned lucerna.denoise.diffuse color target from the
// public Mojang InSampler bound to lucerna.lighting.diffuseGi as
// raw-diffuse-gi-rgba8 input. The raw GI payload may include traced lighting
// evidence from upstream GI producers, but this shader must consume it as
// raw-diffuse-gi-rgba8 and keep that source identity. It intentionally does not
// add extra Mojang samplers yet. Direct-light validation RGBA,
// lucerna.lighting.direct, and direct-light preview payloads are not accepted
// raw-GI substitutes for this path. The current scheduler still uses
// denoise/noop.glsl until Java or native code binds these semantic inputs and
// output attachments. If the
// depth/normal/material/history bindings are unavailable, the pass may fall
// back to raw-GI signal edge preservation, but telemetry must report the
// missing bindings and must not claim the full geometry/history-guided denoise
// contract.
//
// Required readable inputs:
// - RawDiffuseGiSampler: lucerna.lighting.diffuseGi raw-diffuse-gi-rgba8 only,
//   not lucerna.lighting.direct or a native direct-light validation payload.
//   Traced raw-GI evidence may be present in this payload, but physical GI
//   quality still requires controller evidence that traced lighting was
//   produced and consumed; this resource declaration alone is not proof.
// - RawGiConfidenceSampler: lucerna.lighting.cacheConfidence.
// - RawGiVarianceSampler: lucerna.lighting.variance.
// - CurrentDepthSampler: lucerna.gbuffer.depth.
// - CurrentNormalRoughnessSampler: lucerna.gbuffer.normalRoughness.
// - CurrentMaterialIdSampler: lucerna.gbuffer.materialId.
// - CurrentMotionHistorySampler: lucerna.gbuffer.motionHistory.
// - PreviousDepthSampler, PreviousNormalRoughnessSampler, PreviousLightingSampler.
//
// Outputs:
// - DenoisedDiffuseOutput: lucerna.denoise.diffuse.
// - RejectionMaskOutput: lucerna.denoise.rejectionMask in red, with green/blue
//   carrying variance and confidence diagnostics for debug use.

in vec2 texCoord;

layout(location = 0) out vec4 DenoisedDiffuseOutput;
layout(location = 1) out vec4 RejectionMaskOutput;

uniform sampler2D RawDiffuseGiSampler;
uniform sampler2D RawGiConfidenceSampler;
uniform sampler2D RawGiVarianceSampler;
uniform sampler2D CurrentDepthSampler;
uniform sampler2D CurrentNormalRoughnessSampler;
uniform sampler2D CurrentMaterialIdSampler;
uniform sampler2D CurrentMotionHistorySampler;
uniform sampler2D PreviousDepthSampler;
uniform sampler2D PreviousNormalRoughnessSampler;
uniform sampler2D PreviousLightingSampler;

uniform float LucernaShaderDenoiseEnabled;
uniform float LucernaDepthBindingReady;
uniform float LucernaNormalBindingReady;
uniform float LucernaMaterialBindingReady;
uniform float LucernaHistoryBindingReady;
uniform float LucernaVarianceClamp;
uniform float LucernaHistoryBlend;

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float CHECKER_ARTIFACT_REJECT = 0.54;
const float EDGE_AWARE_NEIGHBORHOOD_SLACK = 0.16;

vec3 localRawMean(vec2 uv, vec4 centerRaw);

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

vec2 texelSize(sampler2D source) {
    return 1.0 / max(vec2(textureSize(source, 0)), vec2(1.0));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float depthAt(vec2 uv) {
    return clamp(texture(CurrentDepthSampler, clampUv(uv)).r, 0.0, 1.0);
}

vec3 decodedNormal(sampler2D source, vec2 uv) {
    vec3 packed = texture(source, clampUv(uv)).xyz;
    vec3 normal = packed * 2.0 - vec3(1.0);
    return normalize(mix(vec3(0.0, 0.0, 1.0), normal, step(0.001, dot(normal, normal))));
}

float materialIdAt(vec2 uv) {
    return texture(CurrentMaterialIdSampler, clampUv(uv)).r;
}

float confidenceAt(vec2 uv) {
    return clamp(texture(RawGiConfidenceSampler, clampUv(uv)).r, 0.0, 1.0);
}

float varianceAt(vec2 uv) {
    return max(texture(RawGiVarianceSampler, clampUv(uv)).r, 0.0);
}

float depthPreservationWeight(float centerDepth, float sampleDepth) {
    if (LucernaDepthBindingReady < 0.5) {
        return 1.0;
    }
    return exp(-abs(centerDepth - sampleDepth) * 96.0);
}

float normalPreservationWeight(vec3 centerNormal, vec3 sampleNormal) {
    if (LucernaNormalBindingReady < 0.5) {
        return 1.0;
    }
    return smoothstep(0.35, 0.98, dot(centerNormal, sampleNormal));
}

float materialPreservationWeight(float centerMaterial, float sampleMaterial) {
    if (LucernaMaterialBindingReady < 0.5) {
        return 1.0;
    }
    return 1.0 - smoothstep(0.0005, 0.004, abs(centerMaterial - sampleMaterial));
}

float sampleWeight(vec2 centerUv, vec2 sampleUv, vec4 centerRaw, float centerDepth, vec3 centerNormal, float centerMaterial) {
    vec4 raw = texture(RawDiffuseGiSampler, clampUv(sampleUv));
    float sampleDepth = centerDepth;
    if (LucernaDepthBindingReady >= 0.5) {
        sampleDepth = depthAt(sampleUv);
    }
    vec3 sampleNormal = centerNormal;
    if (LucernaNormalBindingReady >= 0.5) {
        sampleNormal = decodedNormal(CurrentNormalRoughnessSampler, sampleUv);
    }
    float sampleMaterial = centerMaterial;
    if (LucernaMaterialBindingReady >= 0.5) {
        sampleMaterial = materialIdAt(sampleUv);
    }

    float depthWeight = depthPreservationWeight(centerDepth, sampleDepth);
    float normalWeight = normalPreservationWeight(centerNormal, sampleNormal);
    float materialWeight = materialPreservationWeight(centerMaterial, sampleMaterial);
    float lumaWeight = exp(-abs(luminance(centerRaw.rgb) - luminance(raw.rgb)) * 9.0);
    float confidenceWeight = mix(0.35, 1.0, confidenceAt(sampleUv));
    float varianceWeight = 1.0 / (1.0 + varianceAt(sampleUv) * max(LucernaVarianceClamp, 0.25));

    return depthWeight * normalWeight * materialWeight * lumaWeight * confidenceWeight * varianceWeight;
}

float localConfidenceRange(vec2 uv) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    float center = confidenceAt(uv);
    float left = confidenceAt(uv - vec2(texel.x, 0.0));
    float right = confidenceAt(uv + vec2(texel.x, 0.0));
    float up = confidenceAt(uv - vec2(0.0, texel.y));
    float down = confidenceAt(uv + vec2(0.0, texel.y));
    return max(max(abs(center - left), abs(center - right)),
            max(abs(center - up), abs(center - down)));
}

float localRawChromaRange(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    vec3 center = max(centerRaw.rgb, vec3(0.0));
    vec3 left = max(texture(RawDiffuseGiSampler, clampUv(uv - vec2(texel.x, 0.0))).rgb, vec3(0.0));
    vec3 right = max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x, 0.0))).rgb, vec3(0.0));
    vec3 up = max(texture(RawDiffuseGiSampler, clampUv(uv - vec2(0.0, texel.y))).rgb, vec3(0.0));
    vec3 down = max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y))).rgb, vec3(0.0));
    return max(max(length(center - left), length(center - right)),
            max(length(center - up), length(center - down)));
}

float checkerArtifactProxy(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    float center = confidenceAt(uv);
    float axis = (
            confidenceAt(uv + vec2(texel.x, 0.0))
            + confidenceAt(uv + vec2(-texel.x, 0.0))
            + confidenceAt(uv + vec2(0.0, texel.y))
            + confidenceAt(uv + vec2(0.0, -texel.y))) * 0.25;
    float diagonal = (
            confidenceAt(uv + texel * vec2(1.0, 1.0))
            + confidenceAt(uv + texel * vec2(-1.0, 1.0))
            + confidenceAt(uv + texel * vec2(1.0, -1.0))
            + confidenceAt(uv + texel * vec2(-1.0, -1.0))) * 0.25;
    float alternating = abs(center - axis) + max(diagonal - axis, 0.0) * 0.70;
    float geometryOrRawEdge = localConfidenceRange(uv) + localRawChromaRange(uv, centerRaw) * 0.60;
    return smoothstep(0.020, 0.16, alternating) * (1.0 - smoothstep(0.10, 0.32, geometryOrRawEdge));
}

vec3 localRawMin(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    vec3 minRgb = max(centerRaw.rgb, vec3(0.0));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x, 0.0))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(-texel.x, 0.0))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, -texel.y))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(1.0, 1.0))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-1.0, 1.0))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(1.0, -1.0))).rgb, vec3(0.0)));
    minRgb = min(minRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-1.0, -1.0))).rgb, vec3(0.0)));
    return minRgb;
}

vec3 localRawMax(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    vec3 maxRgb = max(centerRaw.rgb, vec3(0.0));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x, 0.0))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(-texel.x, 0.0))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, -texel.y))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(1.0, 1.0))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-1.0, 1.0))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(1.0, -1.0))).rgb, vec3(0.0)));
    maxRgb = max(maxRgb, max(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-1.0, -1.0))).rgb, vec3(0.0)));
    return maxRgb;
}

vec3 clampToRawNeighborhood(vec2 uv, vec4 centerRaw, vec3 candidate, float slack) {
    vec3 minRgb = localRawMin(uv, centerRaw);
    vec3 maxRgb = localRawMax(uv, centerRaw);
    vec3 range = max(maxRgb - minRgb, vec3(0.001));
    return clamp(candidate, max(minRgb - range * slack, vec3(0.0)), maxRgb + range * slack + vec3(0.001));
}

vec4 spatialDenoise(vec2 uv, vec4 centerRaw, float centerDepth, vec3 centerNormal, float centerMaterial, out float rejectedWeight) {
    // Source-gated denoise marker: smoothing is accepted only when the raw GI
    // signal and available depth/normal/material guidance agree. This preserves
    // payload edges and keeps checker suppression separate from geometry edges.
    vec2 texel = texelSize(RawDiffuseGiSampler);
    vec2 offsets[8] = vec2[](
        vec2(1.0, 0.0),
        vec2(-1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(0.0, -1.0),
        vec2(1.0, 1.0),
        vec2(-1.0, 1.0),
        vec2(1.0, -1.0),
        vec2(-1.0, -1.0)
    );

    vec3 sumColor = max(centerRaw.rgb, vec3(0.0)) * 1.35;
    float sumAlpha = clamp(centerRaw.a, 0.0, 1.0) * 1.35;
    float sumWeight = 1.35;
    float rejected = 0.0;

    for (int i = 0; i < 8; ++i) {
        vec2 sampleUv = uv + offsets[i] * texel;
        vec4 raw = texture(RawDiffuseGiSampler, clampUv(sampleUv));
        float weight = sampleWeight(uv, sampleUv, centerRaw, centerDepth, centerNormal, centerMaterial);
        sumColor += max(raw.rgb, vec3(0.0)) * weight;
        sumAlpha += clamp(raw.a, 0.0, 1.0) * weight;
        sumWeight += weight;
        rejected += 1.0 - clamp(weight, 0.0, 1.0);
    }

    rejectedWeight = clamp(rejected / 8.0, 0.0, 1.0);
    vec3 filtered = sumColor / max(sumWeight, 0.0001);
    float checkerReject = checkerArtifactProxy(uv, centerRaw) * CHECKER_ARTIFACT_REJECT;
    filtered = clampToRawNeighborhood(uv, centerRaw, filtered, EDGE_AWARE_NEIGHBORHOOD_SLACK);
    filtered = mix(filtered, localRawMean(uv, centerRaw), checkerReject);
    return vec4(filtered, clamp(sumAlpha / max(sumWeight, 0.0001), 0.0, 1.0));
}

vec3 localRawMean(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    vec3 sum = max(centerRaw.rgb, vec3(0.0)) * 2.0;
    sum += max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x, 0.0))).rgb, vec3(0.0));
    sum += max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(-texel.x, 0.0))).rgb, vec3(0.0));
    sum += max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y))).rgb, vec3(0.0));
    sum += max(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, -texel.y))).rgb, vec3(0.0));
    return sum / 6.0;
}

float localRawLumaRange(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    float center = luminance(centerRaw.rgb);
    float minLuma = center;
    float maxLuma = center;
    float left = luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(-texel.x, 0.0))).rgb);
    float right = luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x, 0.0))).rgb);
    float up = luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, -texel.y))).rgb);
    float down = luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y))).rgb);
    minLuma = min(minLuma, min(min(left, right), min(up, down)));
    maxLuma = max(maxLuma, max(max(left, right), max(up, down)));
    return max(maxLuma - minLuma, 0.0);
}

float lowResolutionPlateauReject(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize(RawDiffuseGiSampler);
    float centerConfidence = max(confidenceAt(uv), luminance(centerRaw.rgb) * 18.0);
    float axisConfidence = (
            max(confidenceAt(uv + vec2(texel.x * 2.0, 0.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(texel.x * 2.0, 0.0))).rgb) * 18.0)
            + max(confidenceAt(uv + vec2(-texel.x * 2.0, 0.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(-texel.x * 2.0, 0.0))).rgb) * 18.0)
            + max(confidenceAt(uv + vec2(0.0, texel.y * 2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, texel.y * 2.0))).rgb) * 18.0)
            + max(confidenceAt(uv + vec2(0.0, -texel.y * 2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + vec2(0.0, -texel.y * 2.0))).rgb) * 18.0)) * 0.25;
    float diagonalConfidence = (
            max(confidenceAt(uv + texel * vec2(2.0, 2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(2.0, 2.0))).rgb) * 18.0)
            + max(confidenceAt(uv + texel * vec2(-2.0, 2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-2.0, 2.0))).rgb) * 18.0)
            + max(confidenceAt(uv + texel * vec2(2.0, -2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(2.0, -2.0))).rgb) * 18.0)
            + max(confidenceAt(uv + texel * vec2(-2.0, -2.0)),
                    luminance(texture(RawDiffuseGiSampler, clampUv(uv + texel * vec2(-2.0, -2.0))).rgb) * 18.0)) * 0.25;
    float broadSignal = max(centerConfidence, max(axisConfidence, diagonalConfidence));
    float plateau = 1.0 - smoothstep(0.010, 0.070,
            abs(centerConfidence - axisConfidence) + abs(axisConfidence - diagonalConfidence));
    float structure = localRawChromaRange(uv, centerRaw) * 0.72
            + localRawLumaRange(uv, centerRaw) * 1.20
            + localConfidenceRange(uv) * 0.72;
    return smoothstep(0.050, 0.34, broadSignal) * plateau
            * (1.0 - smoothstep(0.030, 0.18, structure));
}

vec3 preserveRawLocalContrast(vec2 uv, vec4 centerRaw, vec3 denoisedRgb) {
    vec3 centerRgb = max(centerRaw.rgb, vec3(0.0));
    vec3 rawDetail = centerRgb - localRawMean(uv, centerRaw);
    float structure = smoothstep(0.002, 0.055, localRawLumaRange(uv, centerRaw));
    float support = smoothstep(0.001, 0.030, max(confidenceAt(uv), luminance(centerRgb)));
    return max(denoisedRgb + rawDetail * (0.32 * structure * support), vec3(0.0));
}

float historyAcceptance(vec2 uv, float centerDepth, vec3 centerNormal) {
    if (LucernaHistoryBindingReady < 0.5) {
        return 0.0;
    }

    vec2 motion = texture(CurrentMotionHistorySampler, clampUv(uv)).xy;
    vec2 historyUv = clampUv(uv - motion);
    float previousDepth = clamp(texture(PreviousDepthSampler, historyUv).r, 0.0, 1.0);
    vec3 previousNormal = decodedNormal(PreviousNormalRoughnessSampler, historyUv);

    float depthOk = 1.0 - smoothstep(0.0015, 0.025, abs(centerDepth - previousDepth));
    float normalOk = smoothstep(0.45, 0.96, dot(centerNormal, previousNormal));
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, centerDepth);
    return clamp(depthOk * normalOk * finiteSurface, 0.0, 1.0);
}

void main() {
    vec4 centerRaw = texture(RawDiffuseGiSampler, clampUv(texCoord));

    if (LucernaShaderDenoiseEnabled <= 0.0) {
        DenoisedDiffuseOutput = vec4(0.0);
        RejectionMaskOutput = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }

    float centerDepth = 1.0;
    if (LucernaDepthBindingReady >= 0.5) {
        centerDepth = depthAt(texCoord);
    }
    vec3 centerNormal = vec3(0.0, 0.0, 1.0);
    if (LucernaNormalBindingReady >= 0.5) {
        centerNormal = decodedNormal(CurrentNormalRoughnessSampler, texCoord);
    }
    float centerMaterial = 0.0;
    if (LucernaMaterialBindingReady >= 0.5) {
        centerMaterial = materialIdAt(texCoord);
    }
    float rejectedWeight = 0.0;
    vec4 filtered = spatialDenoise(texCoord, centerRaw, centerDepth, centerNormal, centerMaterial, rejectedWeight);

    float historyGate = historyAcceptance(texCoord, centerDepth, centerNormal);
    vec4 previousLighting = vec4(0.0);
    if (LucernaHistoryBindingReady >= 0.5) {
        vec2 historyUv = clampUv(texCoord - texture(CurrentMotionHistorySampler, clampUv(texCoord)).xy);
        previousLighting = texture(PreviousLightingSampler, historyUv);
    }
    float historyBlend = clamp(LucernaHistoryBlend, 0.0, 0.92) * historyGate;

    vec3 denoisedRgb = mix(filtered.rgb, max(previousLighting.rgb, vec3(0.0)), historyBlend);
    denoisedRgb = preserveRawLocalContrast(texCoord, centerRaw, denoisedRgb);
    denoisedRgb = clampToRawNeighborhood(texCoord, centerRaw, denoisedRgb,
            EDGE_AWARE_NEIGHBORHOOD_SLACK + (1.0 - historyGate) * 0.08);
    float plateauReject = lowResolutionPlateauReject(texCoord, centerRaw);
    denoisedRgb = mix(denoisedRgb, max(centerRaw.rgb, vec3(0.0)),
            plateauReject * (1.0 - historyGate * 0.45) * 0.50);
    float confidence = confidenceAt(texCoord);
    float variance = varianceAt(texCoord);
    float varianceReject = smoothstep(0.025, 0.42, variance);
    float geometryGuidance = min(min(clamp(LucernaDepthBindingReady, 0.0, 1.0),
            clamp(LucernaNormalBindingReady, 0.0, 1.0)), clamp(LucernaMaterialBindingReady, 0.0, 1.0));
    float missingGuidanceReject = (1.0 - geometryGuidance) * 0.18;
    float rejection = clamp(max(rejectedWeight, 1.0 - historyGate) * 0.62
            + varianceReject * 0.30
            + missingGuidanceReject
            + plateauReject * 0.26, 0.0, 1.0);

    // Raw-GI input preservation and non-compute boundary marker: this resource
    // may write color attachments when scheduled, but it does not claim Vulkan
    // compute, storage-image denoise, or hardware denoise by existing alone.
    DenoisedDiffuseOutput = vec4(max(denoisedRgb, vec3(0.0)), clamp(max(filtered.a, confidence), 0.0, 1.0));
    RejectionMaskOutput = vec4(rejection, clamp(variance, 0.0, 1.0), confidence, 1.0);
}
