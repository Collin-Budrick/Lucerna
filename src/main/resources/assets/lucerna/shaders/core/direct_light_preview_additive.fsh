#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 directPreview = texture(InSampler, texCoord);
    vec3 directLight = max(directPreview.rgb, vec3(0.0)) * clamp(directPreview.a, 0.0, 1.0);
    fragColor = vec4(directLight, 0.0);
}
