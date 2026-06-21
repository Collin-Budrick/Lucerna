#version 330

in vec2 texCoord;

out vec4 fragColor;

void main() {
    /*
     * The earlier implementation generated fixed screen-space daytime shadow
     * bands from texCoord. Shadow preview visibility now comes from
     * WorldSpaceShadowDecalSubmitter block-face quads instead. Keep this
     * full-screen pass transparent until a real shadow-map/depth target is
     * sampled here.
     */
    fragColor = vec4(0.010, 0.012, 0.016, 0.0);
}
