#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 38.0;
const vec3 FINAL_COMPOSITE_FLOOR = vec3(0.16, 0.10, 0.035);
const vec3 GI_TINT_FLOOR = vec3(0.56, 0.42, 0.22);

vec4 maxDirectSample(vec2 uv) {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec4 result = texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
    result = max(result, texture(InSampler, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))));
    return result;
}

vec4 nativeGiCompositeSample(vec2 uv) {
    vec2 sourceSize = vec2(textureSize(InSampler, 0));
    vec2 texel = 1.0 / max(sourceSize, vec2(1.0));
    vec4 localSignal = maxDirectSample(uv);
    vec4 broadSignal = vec4(0.0);

    broadSignal = max(broadSignal, maxDirectSample(vec2(0.20, 0.24)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.50, 0.24)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.80, 0.24)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.24, 0.50)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.50, 0.50)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.76, 0.50)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.20, 0.76)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.50, 0.76)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.80, 0.76)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.12, 0.12)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.88, 0.12)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.12, 0.88)));
    broadSignal = max(broadSignal, maxDirectSample(vec2(0.88, 0.88)));
    broadSignal = max(broadSignal, texture(InSampler, clamp(uv + texel * vec2(3.0, 2.0), vec2(0.0), vec2(1.0))));
    broadSignal = max(broadSignal, texture(InSampler, clamp(uv + texel * vec2(-3.0, -2.0), vec2(0.0), vec2(1.0))));

    return max(localSignal, broadSignal);
}

float nativeSignalMask(vec4 sampleValue) {
    float rgbSignal = dot(max(sampleValue.rgb, vec3(0.0)), vec3(0.2126, 0.7152, 0.0722));
    return clamp(max(sampleValue.a, rgbSignal * 32.0), 0.0, 1.0);
}

float sourceShapingMask(vec2 uv, vec4 sampleValue) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = nativeSignalMask(sampleValue);
    float xDelta = abs(nativeSignalMask(nativeGiCompositeSample(uv + vec2(texel.x * 2.0, 0.0)))
            - nativeSignalMask(nativeGiCompositeSample(uv + vec2(-texel.x * 2.0, 0.0))));
    float yDelta = abs(nativeSignalMask(nativeGiCompositeSample(uv + vec2(0.0, texel.y * 2.0)))
            - nativeSignalMask(nativeGiCompositeSample(uv + vec2(0.0, -texel.y * 2.0))));
    float localStructure = smoothstep(0.010, 0.16, center + (xDelta + yDelta) * 0.65);
    float softEdgeGuard = smoothstep(0.004, 0.036, uv.x)
            * smoothstep(0.004, 0.036, uv.y)
            * (1.0 - smoothstep(0.964, 0.996, uv.x))
            * (1.0 - smoothstep(0.964, 0.996, uv.y));
    return clamp(localStructure * softEdgeGuard, 0.0, 1.0);
}

void main() {
    vec4 directPreview = nativeGiCompositeSample(texCoord);
    float previewMask = nativeSignalMask(directPreview);
    float surfaceMask = sourceShapingMask(texCoord, directPreview);
    float signalLuma = dot(max(directPreview.rgb, vec3(0.0)), vec3(0.2126, 0.7152, 0.0722));
    vec3 sourceTint = max(max(directPreview.rgb, vec3(0.0)), GI_TINT_FLOOR * max(signalLuma, previewMask * 0.18));
    vec3 directLight = sourceTint * FINAL_COMPOSITE_GAIN + FINAL_COMPOSITE_FLOOR * previewMask;
    fragColor = vec4(directLight * previewMask * surfaceMask, 1.0);
}
