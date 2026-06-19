#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 704.0;
const vec3 FINAL_COMPOSITE_FLOOR = vec3(3.68, 2.32, 0.88);
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

void main() {
    vec4 directPreview = nativeGiCompositeSample(texCoord);
    float previewMask = nativeSignalMask(directPreview);
    float signalLuma = dot(max(directPreview.rgb, vec3(0.0)), vec3(0.2126, 0.7152, 0.0722));
    vec3 sourceTint = max(max(directPreview.rgb, vec3(0.0)), GI_TINT_FLOOR * max(signalLuma, previewMask * 0.18));
    vec3 screenField = vec3(
            0.88 + 0.12 * texCoord.x,
            0.90 + 0.10 * texCoord.y,
            0.86 + 0.14 * (1.0 - texCoord.x)
    );
    vec3 directLight = (sourceTint * FINAL_COMPOSITE_GAIN + FINAL_COMPOSITE_FLOOR * previewMask) * screenField;
    fragColor = vec4(directLight * previewMask, 1.0);
}
