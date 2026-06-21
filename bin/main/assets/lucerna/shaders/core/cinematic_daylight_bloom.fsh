#version 330

in vec2 texCoord;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 37.7);
    return fract(p.x * p.y);
}

float noise(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float radial(vec2 uv, vec2 center, vec2 radius, float falloff) {
    vec2 delta = (uv - center) / radius;
    return exp(-dot(delta, delta) * falloff);
}

float ray(vec2 uv, vec2 origin, float angle, float width, float lengthScale) {
    vec2 p = uv - origin;
    vec2 axis = vec2(cos(angle), sin(angle));
    float along = dot(p, axis);
    float across = abs(dot(p, vec2(-axis.y, axis.x)));
    float longitudinal = smoothstep(-0.05, 0.08, along) * exp(-max(along, 0.0) * lengthScale);
    float lateral = exp(-(across * across) / max(width * width, 0.0001));
    return longitudinal * lateral;
}

void main() {
    vec2 uv = texCoord;
    vec2 sun = vec2(0.118, 0.885);

    float sunCore = radial(uv, sun, vec2(0.185, 0.165), 1.55);
    float sunBloom = radial(uv, sun, vec2(0.520, 0.370), 2.00);
    float wideHaze = radial(uv, sun, vec2(1.050, 0.610), 2.20);
    float horizonMist = smoothstep(0.30, 0.56, uv.y) * (1.0 - smoothstep(0.72, 0.96, uv.y));
    float waterBand = smoothstep(0.32, 0.43, uv.y) * (1.0 - smoothstep(0.55, 0.70, uv.y));
    float waterRipple = 0.55 + 0.45 * sin((uv.x + uv.y * 0.42) * 74.0);
    waterRipple *= 0.68 + 0.32 * sin((uv.x * 0.75 - uv.y) * 39.0);
    float waterGlint = waterBand * waterRipple;

    float highCloud = smoothstep(0.70, 0.82, uv.y) * (1.0 - smoothstep(0.95, 1.0, uv.y));
    float cloudShape = noise(uv * vec2(3.6, 2.0) + vec2(4.3, 9.2));
    cloudShape = smoothstep(0.38, 0.82, cloudShape);
    float litCloud = highCloud * cloudShape * (0.30 + 0.70 * radial(uv, sun, vec2(0.78, 0.38), 2.0));

    float rayField = 0.0;
    rayField += ray(uv, sun, 0.35, 0.052, 1.95);
    rayField += ray(uv, sun, 0.56, 0.044, 2.20);
    rayField += ray(uv, sun, 0.80, 0.040, 2.55);
    rayField += ray(uv, sun, 1.08, 0.046, 2.35);
    float cloudBreakup = mix(0.34, 0.88, noise(uv * vec2(8.8, 4.8) + vec2(1.7, 0.4)));
    float mistBreakup = mix(0.60, 1.0, noise(uv * vec2(2.9, 2.1) + vec2(4.4, 7.1)));
    rayField = smoothstep(0.04, 0.68, rayField * cloudBreakup * mistBreakup);

    vec3 gold = vec3(1.00, 0.68, 0.30);
    vec3 cream = vec3(1.00, 0.88, 0.66);
    vec3 skyWarm = vec3(0.34, 0.42, 0.58);
    vec3 waterWarm = vec3(0.72, 0.56, 0.36);
    vec3 cloudWarm = vec3(0.78, 0.70, 0.58);

    vec3 color = vec3(0.0);
    color += cream * sunCore * 0.000;
    color += gold * sunBloom * 0.014;
    color += gold * wideHaze * 0.018;
    color += cream * rayField * 0.030;
    color += skyWarm * horizonMist * 0.014;
    color += cloudWarm * litCloud * 0.026;
    color += waterWarm * waterGlint * radial(uv, vec2(0.500, 0.440), vec2(0.650, 0.190), 1.55) * 0.045;
    color += cream * waterGlint * radial(uv, vec2(0.300, 0.465), vec2(0.460, 0.125), 2.0) * 0.022;

    float hudFade = 1.0 - smoothstep(0.84, 0.97, uv.y);
    float edgeControl = smoothstep(0.0, 0.05, uv.x) * (1.0 - smoothstep(0.96, 1.0, uv.x));
    vec3 shoulder = color / (color + vec3(0.70));
    fragColor = vec4(min(shoulder * hudFade * edgeControl, vec3(0.045)), 1.0);
}
