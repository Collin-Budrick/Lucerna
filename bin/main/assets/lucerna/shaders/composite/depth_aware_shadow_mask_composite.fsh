#version 330

// Runtime public-Mojang shader for the depth-backed shadow-mask slice.
// Sampler0 = native conservative shadow-mask RGBA8 payload.
// Sampler1 = current Minecraft depth texture view from the live render target.
// The caller must use a SAMPLER0_SAMPLER1 bind layout and only report this as
// depth-sampling evidence when the draw is actually submitted.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;

out vec4 fragColor;

const float MAX_SHADOW_ALPHA = 0.62;
const float SOFT_SHADOW_RADIUS = 1.25;
const float MIN_PAYLOAD_COVERAGE = 0.030;

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

vec4 maskSample(vec2 uv) {
    return texture(Sampler0, clampUv(uv));
}

float receiverSupportOf(vec4 payload) {
    return clamp(max(payload.g, payload.b), 0.0, 1.0);
}

float depthAt(vec2 uv) {
    return clamp(texture(Sampler1, clampUv(uv)).r, 0.0, 1.0);
}

float depthSurfaceGate(vec2 uv, float centerDepth) {
    vec2 texel = 1.0 / max(vec2(textureSize(Sampler1, 0)), vec2(1.0));
    float left = depthAt(uv - vec2(texel.x, 0.0));
    float right = depthAt(uv + vec2(texel.x, 0.0));
    float up = depthAt(uv - vec2(0.0, texel.y));
    float down = depthAt(uv + vec2(0.0, texel.y));

    float localSpan = max(max(abs(centerDepth - left), abs(centerDepth - right)),
            max(abs(centerDepth - up), abs(centerDepth - down)));
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, centerDepth);
    float edgeSafe = 1.0 - smoothstep(0.006, 0.050, localSpan);
    return clamp(max(finiteSurface * edgeSafe, finiteSurface * 0.35), 0.0, 1.0);
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

vec4 depthConsistentSoftMask(vec2 uv, vec4 centerMask, float centerDepth) {
    vec2 maskTexel = 1.0 / max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec2 depthTexel = 1.0 / max(vec2(textureSize(Sampler1, 0)), vec2(1.0));
    float centerReceiver = receiverSupportOf(centerMask);
    float centerCoverage = clamp(centerMask.r, 0.0, 1.0);

    float weightedCoverage = 0.0;
    float weightedReceiver = 0.0;
    float weightedEdge = 0.0;
    float weightedValidity = 0.0;
    float totalWeight = 0.0;

    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 maskUv = uv + vec2(float(x), float(y)) * maskTexel * SOFT_SHADOW_RADIUS;
            vec2 depthUv = uv + vec2(float(x), float(y)) * depthTexel * SOFT_SHADOW_RADIUS;
            vec4 sampleMask = maskSample(maskUv);
            float sampleCoverage = clamp(sampleMask.r, 0.0, 1.0);
            float sampleReceiver = receiverSupportOf(sampleMask);
            float sampleValidity = clamp(max(sampleMask.a, sampleReceiver), 0.0, 1.0);
            float depthSimilarity = 1.0 - smoothstep(0.0025, 0.035,
                    abs(depthAt(depthUv) - centerDepth));
            float receiverSimilarity = 1.0 - smoothstep(0.10, 0.55,
                    abs(sampleReceiver - centerReceiver));
            float coverageSimilarity = 1.0 - smoothstep(0.20, 0.80,
                    abs(sampleCoverage - centerCoverage));
            float supportGate = smoothstep(0.010, 0.18,
                    max(max(sampleReceiver, centerReceiver), max(sampleValidity, sampleCoverage)));
            float weight = spatialWeight(x, y)
                    * depthSimilarity
                    * mix(0.36, 1.0, receiverSimilarity)
                    * mix(0.62, 1.0, coverageSimilarity)
                    * supportGate;

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

float localMaskStructure(vec2 uv, vec4 centerMask) {
    vec2 texel = 1.0 / max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec4 left = maskSample(uv - vec2(texel.x, 0.0));
    vec4 right = maskSample(uv + vec2(texel.x, 0.0));
    vec4 up = maskSample(uv - vec2(0.0, texel.y));
    vec4 down = maskSample(uv + vec2(0.0, texel.y));
    float coverageEdge = abs(right.r - left.r) + abs(down.r - up.r);
    float receiverEdge = abs(receiverSupportOf(right) - receiverSupportOf(left))
            + abs(receiverSupportOf(down) - receiverSupportOf(up));
    return clamp(receiverSupportOf(centerMask) * 0.55 + coverageEdge * 0.45 + receiverEdge * 0.90, 0.0, 1.0);
}

float receiverLocalityGate(vec2 uv, vec4 centerMask, float centerDepth) {
    vec2 maskTexel = 1.0 / max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec2 depthTexel = 1.0 / max(vec2(textureSize(Sampler1, 0)), vec2(1.0));
    float centerCoverage = clamp(centerMask.r, 0.0, 1.0);
    float coverageSpan = max(max(
            abs(centerCoverage - maskSample(uv - vec2(maskTexel.x * 2.0, 0.0)).r),
            abs(centerCoverage - maskSample(uv + vec2(maskTexel.x * 2.0, 0.0)).r)),
            max(abs(centerCoverage - maskSample(uv - vec2(0.0, maskTexel.y * 2.0)).r),
                    abs(centerCoverage - maskSample(uv + vec2(0.0, maskTexel.y * 2.0)).r)));
    float depthSpan = max(max(
            abs(centerDepth - depthAt(uv - vec2(depthTexel.x * 2.0, 0.0))),
            abs(centerDepth - depthAt(uv + vec2(depthTexel.x * 2.0, 0.0)))),
            max(abs(centerDepth - depthAt(uv - vec2(0.0, depthTexel.y * 2.0))),
                    abs(centerDepth - depthAt(uv + vec2(0.0, depthTexel.y * 2.0)))));
    float structure = localMaskStructure(uv, centerMask);
    float receiverAnchor = smoothstep(0.018, 0.22, max(receiverSupportOf(centerMask), structure));
    float geometryAnchor = smoothstep(0.002, 0.030, depthSpan);
    float broadFlatMask = smoothstep(0.10, 0.48, centerCoverage)
            * (1.0 - smoothstep(0.010, 0.080, coverageSpan + structure))
            * (1.0 - max(receiverAnchor, geometryAnchor * 0.65));
    return clamp(1.0 - broadFlatMask * 0.82, 0.0, 1.0);
}

float borderGuard(vec2 uv) {
    return smoothstep(0.003, 0.020, uv.x)
            * smoothstep(0.003, 0.020, uv.y)
            * (1.0 - smoothstep(0.980, 0.997, uv.x))
            * (1.0 - smoothstep(0.980, 0.997, uv.y));
}

void main() {
    vec4 mask = maskSample(texCoord);
    float centerDepth = depthAt(texCoord);
    vec4 softMask = depthConsistentSoftMask(texCoord, mask, centerDepth);
    float coverage = clamp(softMask.r, 0.0, 1.0);
    float receiver = receiverSupportOf(softMask);
    float validity = clamp(max(softMask.a, receiver), 0.0, 1.0);
    float edgeConfidence = clamp(max(softMask.b, mask.b), 0.0, 1.0);
    float depthGate = depthSurfaceGate(texCoord, centerDepth);
    float structureGate = smoothstep(0.016, 0.34, localMaskStructure(texCoord, mask));
    float receiverGate = smoothstep(0.015, 0.35,
            max(receiver, max(edgeConfidence * 0.55, structureGate * 0.70)));
    float coverageGate = smoothstep(MIN_PAYLOAD_COVERAGE, 0.72, coverage * validity);
    float receiverLocality = receiverLocalityGate(texCoord, mask, centerDepth);

    float alpha = coverageGate
            * receiverGate
            * depthGate
            * receiverLocality
            * borderGuard(texCoord)
            * MAX_SHADOW_ALPHA;

    fragColor = vec4(0.0, 0.0, 0.0, clamp(alpha, 0.0, MAX_SHADOW_ALPHA));
}
