#version 450

// Depth-aware shadow-mask composite resource for lucerna.composite.final.
//
// This is the shader-resource form of the native conservative shadow-mask
// consumer. It is not wired by the current public Mojang single InSampler path.
// The scheduler must bind all semantic samplers below before this resource can
// be runtime evidence. If CurrentDepthSampler is not bound, this shader must
// contribute transparent output rather than falling back to screen-space paint.
//
// Required bindings:
// - ShadowMaskSampler: lucerna.lighting.nativeConservativeShadowMask.
// - CurrentDepthSampler: lucerna.gbuffer.depth for current receiver depth.
// - CurrentNormalRoughnessSampler: lucerna.gbuffer.normalRoughness.
// - SourceLightingSampler: lucerna.lighting.direct or resolved lighting support.
//
// Output:
// - Black source-over alpha for receiver-supported conservative darkening only.
//   This resource does not draw fixed blobs, fog, bloom, debug colors, proof
//   markers, or shadows derived from UV coordinates alone.
//
// Runtime evidence must come from a ready DirectionalShadowMapOutputPayload
// plus bound same-frame depth. The payload is budget metadata and RGBA8 mask
// data from the native conservative CPU path; this shader must not be reported
// as GPU shadow-map generation, hardware RT, or physically complete lighting.

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D ShadowMaskSampler;
uniform sampler2D CurrentDepthSampler;
uniform sampler2D CurrentNormalRoughnessSampler;
uniform sampler2D SourceLightingSampler;

uniform float LucernaShadowCompositeEnabled;
uniform float LucernaDepthBindingReady;
uniform float LucernaNormalBindingReady;

const float MAX_SHADOW_ALPHA = 0.62;
const float SOFT_SHADOW_RADIUS = 1.35;
const float MIN_DEPTH_AWARE_PAYLOAD_COVERAGE = 0.030;
const float MIN_DEPTH_AWARE_RECEIVER_STRUCTURE = 0.018;
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec2 texelSize(sampler2D source) {
    return 1.0 / max(vec2(textureSize(source, 0)), vec2(1.0));
}

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float borderGuard(vec2 uv) {
    return smoothstep(0.002, 0.018, uv.x)
            * smoothstep(0.002, 0.018, uv.y)
            * (1.0 - smoothstep(0.982, 0.998, uv.x))
            * (1.0 - smoothstep(0.982, 0.998, uv.y));
}

float depthAt(vec2 uv) {
    return clamp(texture(CurrentDepthSampler, clampUv(uv)).r, 0.0, 1.0);
}

float depthReceiverGate(vec2 uv, float centerDepth) {
    vec2 texel = texelSize(CurrentDepthSampler);
    float left = depthAt(uv - vec2(texel.x, 0.0));
    float right = depthAt(uv + vec2(texel.x, 0.0));
    float up = depthAt(uv - vec2(0.0, texel.y));
    float down = depthAt(uv + vec2(0.0, texel.y));

    float localSpan = max(max(abs(centerDepth - left), abs(centerDepth - right)),
            max(abs(centerDepth - up), abs(centerDepth - down)));
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, centerDepth);
    float edgeSafe = 1.0 - smoothstep(0.004, 0.045, localSpan);
    return clamp(finiteSurface * edgeSafe, 0.0, 1.0);
}

vec3 decodedNormal(vec2 uv) {
    vec3 packed = texture(CurrentNormalRoughnessSampler, clampUv(uv)).xyz;
    vec3 normal = packed * 2.0 - vec3(1.0);
    return normalize(mix(vec3(0.0, 0.0, 1.0), normal, step(0.001, dot(normal, normal))));
}

float normalReceiverGate(vec2 uv) {
    if (LucernaNormalBindingReady < 0.5) {
        return 1.0;
    }

    vec2 texel = texelSize(CurrentNormalRoughnessSampler);
    vec3 center = decodedNormal(uv);
    vec3 left = decodedNormal(uv - vec2(texel.x, 0.0));
    vec3 right = decodedNormal(uv + vec2(texel.x, 0.0));
    vec3 up = decodedNormal(uv - vec2(0.0, texel.y));
    vec3 down = decodedNormal(uv + vec2(0.0, texel.y));

    float agreement = min(min(dot(center, left), dot(center, right)),
            min(dot(center, up), dot(center, down)));
    return smoothstep(0.42, 0.96, clamp(agreement, 0.0, 1.0));
}

float receiverSupportOf(vec4 payload) {
    return clamp(max(payload.g, payload.b), 0.0, 1.0);
}

float spatialWeight(int x, int y) {
    int distance = abs(x) + abs(y);
    if (distance == 0) {
        return 1.00;
    }
    if (distance == 1) {
        return 0.70;
    }
    return 0.44;
}

vec4 depthAwareSoftMask(vec2 uv, vec4 centerMask, float centerDepth, vec3 centerNormal) {
    vec2 maskTexel = texelSize(ShadowMaskSampler);
    vec2 depthTexel = texelSize(CurrentDepthSampler);
    float centerReceiver = receiverSupportOf(centerMask);
    float centerCoverage = clamp(centerMask.r, 0.0, 1.0);

    float weightedCoverage = 0.0;
    float weightedReceiver = 0.0;
    float weightedEdge = 0.0;
    float weightedValidity = 0.0;
    float totalWeight = 0.0;

    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 sampleUv = uv + vec2(float(x), float(y)) * maskTexel * SOFT_SHADOW_RADIUS;
            vec2 depthUv = uv + vec2(float(x), float(y)) * depthTexel * SOFT_SHADOW_RADIUS;
            vec4 sampleMask = texture(ShadowMaskSampler, clampUv(sampleUv));
            float sampleCoverage = clamp(sampleMask.r, 0.0, 1.0);
            float sampleReceiver = receiverSupportOf(sampleMask);
            float sampleValidity = clamp(max(sampleMask.a, sampleReceiver), 0.0, 1.0);
            float sampleDepth = depthAt(depthUv);

            float receiverSimilarity = 1.0 - smoothstep(0.10, 0.55,
                    abs(sampleReceiver - centerReceiver));
            float coverageSimilarity = 1.0 - smoothstep(0.20, 0.80,
                    abs(sampleCoverage - centerCoverage));
            float depthSimilarity = 1.0 - smoothstep(0.0025, 0.030,
                    abs(sampleDepth - centerDepth));
            float normalSimilarity = 1.0;
            if (LucernaNormalBindingReady >= 0.5) {
                normalSimilarity = smoothstep(0.36, 0.96,
                        clamp(dot(centerNormal, decodedNormal(sampleUv)), 0.0, 1.0));
            }

            float supportGate = max(sampleReceiver, centerReceiver);
            float coveragePresence = smoothstep(MIN_DEPTH_AWARE_PAYLOAD_COVERAGE, 0.35,
                    max(sampleCoverage, centerCoverage)) * 0.35;
            float weight = spatialWeight(x, y)
                    * mix(0.35, 1.0, receiverSimilarity)
                    * mix(0.64, 1.0, coverageSimilarity)
                    * depthSimilarity
                    * normalSimilarity
                    * smoothstep(0.010, 0.20,
                            max(max(supportGate, sampleValidity), coveragePresence));

            weightedCoverage += sampleCoverage * sampleValidity * weight;
            weightedReceiver += sampleReceiver * weight;
            weightedEdge += clamp(sampleMask.b, 0.0, 1.0) * weight;
            weightedValidity += sampleValidity * weight;
            totalWeight += weight;
        }
    }

    if (totalWeight <= 0.0001) {
        return vec4(0.0);
    }

    return vec4(weightedCoverage / totalWeight,
            weightedReceiver / totalWeight,
            weightedEdge / totalWeight,
            weightedValidity / totalWeight);
}

float maskStructure(vec2 uv, vec4 centerMask) {
    vec2 texel = texelSize(ShadowMaskSampler);
    vec4 left = texture(ShadowMaskSampler, clampUv(uv - vec2(texel.x, 0.0)));
    vec4 right = texture(ShadowMaskSampler, clampUv(uv + vec2(texel.x, 0.0)));
    vec4 up = texture(ShadowMaskSampler, clampUv(uv - vec2(0.0, texel.y)));
    vec4 down = texture(ShadowMaskSampler, clampUv(uv + vec2(0.0, texel.y)));

    float coverageEdge = abs(right.r - left.r) + abs(down.r - up.r);
    float receiverEdge = abs(max(right.g, right.b) - max(left.g, left.b))
            + abs(max(down.g, down.b) - max(up.g, up.b));
    float centerSupport = max(centerMask.g, centerMask.b);
    return clamp(centerSupport * 0.60 + coverageEdge * 0.45 + receiverEdge * 0.90, 0.0, 1.0);
}

float depthReceiverLocalityGate(vec2 uv, vec4 centerMask, float centerDepth) {
    vec2 maskTexel = texelSize(ShadowMaskSampler);
    vec2 depthTexel = texelSize(CurrentDepthSampler);
    float centerCoverage = clamp(centerMask.r, 0.0, 1.0);
    float centerReceiver = receiverSupportOf(centerMask);
    float leftCoverage = clamp(texture(ShadowMaskSampler, clampUv(uv - vec2(maskTexel.x * 2.0, 0.0))).r, 0.0, 1.0);
    float rightCoverage = clamp(texture(ShadowMaskSampler, clampUv(uv + vec2(maskTexel.x * 2.0, 0.0))).r, 0.0, 1.0);
    float upCoverage = clamp(texture(ShadowMaskSampler, clampUv(uv - vec2(0.0, maskTexel.y * 2.0))).r, 0.0, 1.0);
    float downCoverage = clamp(texture(ShadowMaskSampler, clampUv(uv + vec2(0.0, maskTexel.y * 2.0))).r, 0.0, 1.0);
    float coverageSpan = max(max(abs(centerCoverage - leftCoverage), abs(centerCoverage - rightCoverage)),
            max(abs(centerCoverage - upCoverage), abs(centerCoverage - downCoverage)));

    float leftDepth = depthAt(uv - vec2(depthTexel.x * 2.0, 0.0));
    float rightDepth = depthAt(uv + vec2(depthTexel.x * 2.0, 0.0));
    float upDepth = depthAt(uv - vec2(0.0, depthTexel.y * 2.0));
    float downDepth = depthAt(uv + vec2(0.0, depthTexel.y * 2.0));
    float depthSpan = max(max(abs(centerDepth - leftDepth), abs(centerDepth - rightDepth)),
            max(abs(centerDepth - upDepth), abs(centerDepth - downDepth)));

    float structure = maskStructure(uv, centerMask);
    float geometryAnchor = smoothstep(0.002, 0.030, depthSpan);
    float receiverAnchor = smoothstep(0.018, 0.22, max(centerReceiver, structure));
    float broadFlatMask = smoothstep(0.10, 0.48, centerCoverage)
            * (1.0 - smoothstep(0.010, 0.080, coverageSpan + structure))
            * (1.0 - max(receiverAnchor, geometryAnchor * 0.65));
    return clamp(1.0 - broadFlatMask * 0.82, 0.0, 1.0);
}

float lightingReceiverGate(vec2 uv) {
    vec4 lighting = texture(SourceLightingSampler, clampUv(uv));
    float signal = luminance(lighting.rgb) + lighting.a * 0.25;
    return smoothstep(0.010, 0.18, signal);
}

void main() {
    float enabled = clamp(LucernaShadowCompositeEnabled, 0.0, 1.0);

    if (enabled <= 0.0 || LucernaDepthBindingReady < 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 mask = texture(ShadowMaskSampler, clampUv(texCoord));
    float centerDepth = depthAt(texCoord);
    vec3 centerNormal = decodedNormal(texCoord);
    vec4 softMask = depthAwareSoftMask(texCoord, mask, centerDepth, centerNormal);
    float coverage = clamp(softMask.r, 0.0, 1.0);
    float receiverSupport = receiverSupportOf(softMask);
    float validity = clamp(max(softMask.a, receiverSupport), 0.0, 1.0);
    float edgeConfidence = clamp(max(softMask.b, mask.b), 0.0, 1.0);

    float depthGate = depthReceiverGate(texCoord, centerDepth);
    float normalGate = normalReceiverGate(texCoord);
    float structureGate = smoothstep(MIN_DEPTH_AWARE_RECEIVER_STRUCTURE, 0.42,
            maskStructure(texCoord, mask));
    float lightingGate = lightingReceiverGate(texCoord);
    float receiverGate = max(receiverSupport, max(structureGate * 0.72, edgeConfidence * 0.52));
    float coverageGate = smoothstep(MIN_DEPTH_AWARE_PAYLOAD_COVERAGE, 0.72,
            coverage * validity);
    float softFalloffShape = mix(0.84, 1.08, smoothstep(0.04, 0.48,
            max(receiverSupport, edgeConfidence)));
    float receiverLocality = depthReceiverLocalityGate(texCoord, mask, centerDepth);

    float alpha = coverageGate
            * receiverGate
            * depthGate
            * normalGate
            * lightingGate
            * softFalloffShape
            * receiverLocality
            * borderGuard(texCoord)
            * MAX_SHADOW_ALPHA;

    fragColor = vec4(0.0, 0.0, 0.0, clamp(alpha, 0.0, MAX_SHADOW_ALPHA));
}
