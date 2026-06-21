#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 0.92;
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float chromaSpan(vec3 color) {
    vec3 positiveColor = max(color, vec3(0.0));
    return max(max(positiveColor.r, positiveColor.g), positiveColor.b)
            - min(min(positiveColor.r, positiveColor.g), positiveColor.b);
}

vec4 maxDirectSample(vec2 uv) {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 clampedUv = clamp(uv, vec2(0.0), vec2(1.0));
    vec4 result = texture(InSampler, vec2(clampedUv.x, 1.0 - clampedUv.y));
    clampedUv = clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0));
    result = max(result, texture(InSampler, vec2(clampedUv.x, 1.0 - clampedUv.y)));
    clampedUv = clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0));
    result = max(result, texture(InSampler, vec2(clampedUv.x, 1.0 - clampedUv.y)));
    clampedUv = clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0));
    result = max(result, texture(InSampler, vec2(clampedUv.x, 1.0 - clampedUv.y)));
    clampedUv = clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0));
    result = max(result, texture(InSampler, vec2(clampedUv.x, 1.0 - clampedUv.y)));
    return result;
}

vec4 localCompositeSample(vec2 uv) {
    vec2 sourceSize = vec2(textureSize(InSampler, 0));
    vec2 texel = 1.0 / max(sourceSize, vec2(1.0));
    vec4 localSignal = maxDirectSample(uv) * 0.56;
    vec4 nearSignal = (
            maxDirectSample(uv + texel * vec2(3.0, 0.0))
            + maxDirectSample(uv + texel * vec2(-3.0, 0.0))
            + maxDirectSample(uv + texel * vec2(0.0, 3.0))
            + maxDirectSample(uv + texel * vec2(0.0, -3.0))) * 0.090;
    vec4 diagonalSignal = (
            maxDirectSample(uv + texel * vec2(2.0, 2.0))
            + maxDirectSample(uv + texel * vec2(-2.0, 2.0))
            + maxDirectSample(uv + texel * vec2(2.0, -2.0))
            + maxDirectSample(uv + texel * vec2(-2.0, -2.0))) * 0.035;

    return localSignal + nearSignal + diagonalSignal;
}

float nativeSignalMask(vec4 sampleValue) {
    float rgbSignal = luminance(sampleValue.rgb);
    float signal = max(sampleValue.a, rgbSignal * 8.0);
    return smoothstep(0.26, 0.74, signal);
}

float sourceShapingMask(vec2 uv, vec4 sampleValue) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = nativeSignalMask(sampleValue);
    float xDelta = abs(nativeSignalMask(localCompositeSample(uv + vec2(texel.x * 2.0, 0.0)))
            - nativeSignalMask(localCompositeSample(uv + vec2(-texel.x * 2.0, 0.0))));
    float yDelta = abs(nativeSignalMask(localCompositeSample(uv + vec2(0.0, texel.y * 2.0)))
            - nativeSignalMask(localCompositeSample(uv + vec2(0.0, -texel.y * 2.0))));
    float materialCue = chromaSpan(sampleValue.rgb) + luminance(sampleValue.rgb) * 0.18;
    float localStructure = smoothstep(0.20, 0.66, center + (xDelta + yDelta) * 0.86 + materialCue * 0.28);
    float antiWashout = smoothstep(0.120, 0.45, xDelta + yDelta + materialCue);
    float softEdgeGuard = smoothstep(0.004, 0.036, uv.x)
            * smoothstep(0.004, 0.036, uv.y)
            * (1.0 - smoothstep(0.964, 0.996, uv.x))
            * (1.0 - smoothstep(0.964, 0.996, uv.y));
    return clamp(localStructure * antiWashout * softEdgeGuard, 0.0, 1.0);
}

void main() {
    vec4 directPreview = localCompositeSample(texCoord);
    float previewMask = nativeSignalMask(directPreview);
    float surfaceMask = sourceShapingMask(texCoord, directPreview);
    float signalLuma = luminance(directPreview.rgb);
    vec3 sourceTint = max(directPreview.rgb - vec3(0.018), vec3(0.0));
    float nonUniformSignal = smoothstep(0.050, 0.240, signalLuma + chromaSpan(directPreview.rgb) * 0.70);
    float contribution = clamp(
            previewMask * surfaceMask * nonUniformSignal * max(directPreview.a, signalLuma * 1.35),
            0.0,
            0.72);
    vec3 directLight = sourceTint * FINAL_COMPOSITE_GAIN * contribution;
    fragColor = vec4(min(directLight, vec3(0.12)), contribution);
}
