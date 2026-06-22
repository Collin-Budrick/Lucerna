#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D InSampler;

float luma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    if (texCoord.x < 0.5) {
        discard;
    }

    vec2 uv = clamp(texCoord, vec2(0.0), vec2(1.0));
    vec2 dims = vec2(textureSize(InSampler, 0));
    vec2 texel = 1.0 / max(dims, vec2(1.0));

    vec4 source = texture(InSampler, uv);
    vec3 bounded = clamp(source.rgb, vec3(0.0), vec3(1.0));

    float center = luma(bounded);
    float north = luma(clamp(texture(InSampler, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb, vec3(0.0), vec3(1.0)));
    float south = luma(clamp(texture(InSampler, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb, vec3(0.0), vec3(1.0)));
    float east = luma(clamp(texture(InSampler, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb, vec3(0.0), vec3(1.0)));
    float west = luma(clamp(texture(InSampler, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb, vec3(0.0), vec3(1.0)));

    float localMean = (north + south + east + west) * 0.25;
    float localRange = max(max(abs(center - north), abs(center - south)), max(abs(center - east), abs(center - west)));
    float gradient = length(vec2(east - west, north - south));
    float localDelta = max(center - localMean, 0.0);
    float localActivity = localRange + localDelta * 0.85 + gradient * 0.28;
    vec3 chroma = bounded - vec3(center);
    float colorfulness = clamp(length(chroma) * 4.70, 0.0, 1.0);
    float localizedStructure = smoothstep(0.016, 0.110, localActivity);
    float receiverDetail = smoothstep(0.010, 0.080, localRange + gradient * 0.42 + localDelta * 0.55);
    float smoothReceiverReject = 1.0 - smoothstep(0.42, 0.78, center)
            * (1.0 - receiverDetail)
            * (1.0 - smoothstep(0.070, 0.190, colorfulness));
    float contourSignal = smoothstep(0.026, 0.105, localRange + gradient * 0.45) * smoothstep(0.22, 0.78, center);
    float shaderHalfX = clamp((uv.x - 0.5) * 2.0, 0.0, 1.0);
    float splitEdgeFade = 1.0 - smoothstep(0.82, 0.985, shaderHalfX);
    float contourKeep = 1.0 - contourSignal * mix(0.72, 1.0, smoothstep(0.62, 0.95, shaderHalfX));
    float artifactKeep = clamp(splitEdgeFade * contourKeep, 0.0, 1.0);

    float colorGate = smoothstep(0.002, 0.052, colorfulness);
    float sourceEnergy = smoothstep(0.026, 0.420, center);
    float detailSupport = smoothstep(0.010, 0.085, localRange + gradient * 0.35 + colorfulness * 0.020);
    float smoothHotBlobReject = min(smoothReceiverReject,
            1.0 - smoothstep(0.50, 0.82, center)
            * (1.0 - smoothstep(0.028, 0.125, localActivity)));
    float payloadLocality = clamp(mix(0.45, 1.0, receiverDetail) * smoothHotBlobReject * artifactKeep, 0.0, 1.0);
    float broadSurface = smoothstep(0.035, 0.340, center)
            * colorGate
            * (0.18 + colorfulness * 0.42)
            * detailSupport
            * smoothHotBlobReject
            * artifactKeep;
    float activeSignal = max(
            localizedStructure * colorGate * sourceEnergy * payloadLocality,
            broadSurface * (0.14 + smoothstep(0.025, 0.120, localActivity) * 0.22));

    vec3 saturated = clamp(mix(vec3(center), bounded, 3.55 + colorfulness * 1.90), vec3(0.0), vec3(1.0));
    float warmBias = smoothstep(-0.05, 0.24, saturated.r - max(saturated.g, saturated.b) * 0.78);
    float coolBias = smoothstep(0.02, 0.26, saturated.b - saturated.r * 0.82);
    float greenBias = smoothstep(0.02, 0.24, saturated.g - max(saturated.r, saturated.b) * 0.86);
    float strongestBias = clamp(max(max(warmBias, coolBias), greenBias), 0.0, 1.0);
    float localPulse = smoothstep(0.032, 0.145, localActivity)
            * smoothstep(0.012, 0.070, localDelta + gradient * 0.38);
    float localizedColorPulse = localPulse * payloadLocality * (0.75 + receiverDetail * 0.55);
    float neutralFloor = min(saturated.r, min(saturated.g, saturated.b));
    vec3 chromaLift = clamp(saturated - vec3(neutralFloor), vec3(0.0), vec3(1.0));
    vec3 bounceTint = vec3(0.0);
    bounceTint += vec3(1.46, 0.72, 0.26) * warmBias;
    bounceTint += vec3(0.30, 0.70, 1.55) * coolBias;
    bounceTint += vec3(0.32, 1.36, 0.36) * greenBias;
    bounceTint += clamp(chromaLift * 2.65 + saturated * 0.22, vec3(0.0), vec3(1.0)) * (1.0 - strongestBias);
    vec3 colorBounce = max(saturated * bounceTint + chromaLift * (0.50 + colorfulness * 0.72), vec3(0.0));
    float visibilityBoost = mix(1.00, 2.25, localizedColorPulse) * (0.88 + colorfulness * 0.56);
    vec3 shapedGi = colorBounce * (0.24 + center * 0.86) * activeSignal * visibilityBoost * smoothHotBlobReject;

    vec3 result = clamp(shapedGi, vec3(0.0), vec3(0.34 + receiverDetail * 0.06));
    float resultLuma = luma(result);
    float contributionAlpha = smoothstep(0.008, 0.155, resultLuma)
            * activeSignal
            * mix(0.88, 1.18, localizedColorPulse)
            * (1.0 - contourSignal * 0.78)
            * smoothHotBlobReject;
    fragColor = vec4(result, clamp(contributionAlpha, 0.0, 0.50));
}
