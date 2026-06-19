#version 450

// Final composite fragment resource for lucerna.composite.final.
// This is distinct from core/direct_light_preview_*.fsh preview plumbing.
// Descriptor bindings are intentionally semantic until the controller/native
// descriptor tables assign concrete slots for the final composite path.

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D LucernaWorldColor;
uniform sampler2D LucernaAlbedoOpacity;
uniform sampler2D LucernaDirectLighting;
uniform sampler2D LucernaDenoisedDiffuse;
uniform sampler2D LucernaDebugOverlay;

uniform float LucernaDirectCompositeGain;
uniform float LucernaDiffuseCompositeGain;
uniform float LucernaDebugOverlayAlpha;
uniform float LucernaFinalCompositeEnabled;

const float LUCERNA_MAX_DIRECT_RADIANCE = 4.0;
const float LUCERNA_MAX_DIFFUSE_RADIANCE = 2.0;

vec3 boundedRadiance(vec3 value, float limit) {
    return clamp(max(value, vec3(0.0)), vec3(0.0), vec3(limit));
}

void main() {
    vec4 worldColor = texture(LucernaWorldColor, texCoord);
    vec4 albedoOpacity = texture(LucernaAlbedoOpacity, texCoord);
    vec4 directLighting = texture(LucernaDirectLighting, texCoord);
    vec4 denoisedDiffuse = texture(LucernaDenoisedDiffuse, texCoord);
    vec4 debugOverlay = texture(LucernaDebugOverlay, texCoord);

    float enabled = clamp(LucernaFinalCompositeEnabled, 0.0, 1.0);
    float surfaceOpacity = clamp(albedoOpacity.a, 0.0, 1.0);
    float directVisibility = clamp(directLighting.a, 0.0, 1.0);

    vec3 baseColor = clamp(worldColor.rgb, vec3(0.0), vec3(1.0));
    vec3 albedo = clamp(albedoOpacity.rgb, vec3(0.0), vec3(1.0));
    vec3 direct = boundedRadiance(directLighting.rgb, LUCERNA_MAX_DIRECT_RADIANCE)
        * max(LucernaDirectCompositeGain, 0.0)
        * directVisibility;
    vec3 diffuse = boundedRadiance(denoisedDiffuse.rgb, LUCERNA_MAX_DIFFUSE_RADIANCE)
        * max(LucernaDiffuseCompositeGain, 0.0);

    vec3 litColor = baseColor + (direct + diffuse) * albedo * surfaceOpacity;
    vec3 finalColor = mix(baseColor, clamp(litColor, vec3(0.0), vec3(1.0)), enabled);

    float overlayAlpha = clamp(debugOverlay.a * LucernaDebugOverlayAlpha, 0.0, 1.0);
    finalColor = mix(finalColor, clamp(debugOverlay.rgb, vec3(0.0), vec3(1.0)), overlayAlpha);

    fragColor = vec4(finalColor, worldColor.a);
}
