#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const vec2 FOCUS_CENTER = vec2(0.50, 0.52);
const vec2 FOCUS_HALF_EXTENT = vec2(0.34, 0.27);
const float FINAL_COMPOSITE_GAIN = 56.0;
const vec3 FINAL_COMPOSITE_FLOOR = vec3(0.58, 0.34, 0.10);

vec4 maxDirectSample(vec2 uv) {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec4 result = texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
    result = max(result, texture(InSampler, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(-texel.x, 0.0), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))));
    result = max(result, texture(InSampler, clamp(uv + vec2(0.0, -texel.y), vec2(0.0), vec2(1.0))));
    return result;
}

void main() {
    vec2 focusDelta = abs(texCoord - FOCUS_CENTER);
    if (focusDelta.x > FOCUS_HALF_EXTENT.x || focusDelta.y > FOCUS_HALF_EXTENT.y) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 focusedUv = ((texCoord - FOCUS_CENTER) / (FOCUS_HALF_EXTENT * 2.0)) + 0.5;
    vec4 directPreview = maxDirectSample(focusedUv);
    float previewMask = clamp(directPreview.a, 0.0, 1.0);
    float edgeFadeX = 1.0 - smoothstep(FOCUS_HALF_EXTENT.x * 0.78, FOCUS_HALF_EXTENT.x, focusDelta.x);
    float edgeFadeY = 1.0 - smoothstep(FOCUS_HALF_EXTENT.y * 0.78, FOCUS_HALF_EXTENT.y, focusDelta.y);
    float focusMask = edgeFadeX * edgeFadeY;
    vec3 directLight = max(directPreview.rgb, vec3(0.0)) * FINAL_COMPOSITE_GAIN;
    directLight += FINAL_COMPOSITE_FLOOR * previewMask;
    fragColor = vec4(directLight * previewMask * focusMask, 1.0);
}
