#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 directLight = max(texture(InSampler, texCoord).rgb, vec3(0.0));
    fragColor = vec4(directLight, 0.0);
}
