#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 112.0;
const vec3 FINAL_COMPOSITE_FLOOR = vec3(0.82, 0.50, 0.18);
const vec3 GI_TINT_FLOOR = vec3(0.24, 0.18, 0.09);

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
    broadSignal = max(broadSignal, texture(InSampler, clamp(uv + texel * vec2(3.0, 2.0), vec2(0.0), vec2(1.0))));
    broadSignal = max(broadSignal, texture(InSampler, clamp(uv + texel * vec2(-3.0, -2.0), vec2(0.0), vec2(1.0))));

    return max(localSignal, broadSignal);
}

void main() {
    vec4 directPreview = nativeGiCompositeSample(texCoord);
    float previewMask = clamp(directPreview.a, 0.0, 1.0);
    vec3 directLight = max(directPreview.rgb, vec3(0.0)) * FINAL_COMPOSITE_GAIN;
    directLight += (FINAL_COMPOSITE_FLOOR + GI_TINT_FLOOR * max(directPreview.rgb, vec3(0.0))) * previewMask;
    fragColor = vec4(directLight * previewMask, 1.0);
}
