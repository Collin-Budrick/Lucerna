#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float PREVIEW_GAIN = 24.0;
const vec3 MIN_PREVIEW_RADIANCE = vec3(0.45, 0.28, 0.08);

void main() {
    vec4 directPreview = texture(InSampler, texCoord);
    float previewMask = clamp(directPreview.a, 0.0, 1.0);
    vec3 directLight = max(directPreview.rgb, vec3(0.0)) * PREVIEW_GAIN;
    directLight += MIN_PREVIEW_RADIANCE * previewMask;
    fragColor = vec4(directLight, 1.0);
}
