package dev.libreglass

/** Independently authored AGSL P0 edge-lensing program. */
internal const val LIBRE_GLASS_SHADER: String = """
uniform shader content;
uniform shader backdrop;
uniform float2 resolution;
uniform float2 sceneOrigin;
uniform float2 sceneSize;
uniform float cornerRadius;
uniform float blurRadius;
uniform float horizontalEdgeWidth;
uniform float verticalEdgeWidth;
uniform float horizontalCurvature;
uniform float verticalCurvature;
uniform float lensStrength;
uniform float refractionWidth;
uniform float distortion;
uniform float dispersion;
uniform float saturation;
uniform float brightness;
uniform float4 tint;
uniform float highlightAlpha;
uniform float rimAlpha;
uniform float shadowAlpha;

float sdRoundRect(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float2 scenePoint(float2 local) {
    return sceneOrigin + local;
}

half3 saturateColor(half3 color, float amount) {
    half luminance = dot(color, half3(0.2126, 0.7152, 0.0722));
    return mix(half3(luminance), color, amount);
}

half4 main(float2 p) {
    half4 source = content.eval(p);
    float2 halfSize = resolution * 0.5;
    float radius = min(cornerRadius, min(halfSize.x, halfSize.y));
    float sd = sdRoundRect(p - halfSize, halfSize, radius);

    float eps = 1.0;
    float dx = sdRoundRect(p + float2(eps, 0.0) - halfSize, halfSize, radius) -
        sdRoundRect(p - float2(eps, 0.0) - halfSize, halfSize, radius);
    float dy = sdRoundRect(p + float2(0.0, eps) - halfSize, halfSize, radius) -
        sdRoundRect(p - float2(0.0, eps) - halfSize, halfSize, radius);
    float2 normal = normalize(float2(dx, dy) + float2(0.0001, 0.0001));
    float axis = abs(normal.x);
    float edgeWidth = mix(verticalEdgeWidth, horizontalEdgeWidth, axis);
    float curvature = mix(verticalCurvature, horizontalCurvature, axis);
    float band = clamp((edgeWidth + sd) / max(edgeWidth, 0.5), 0.0, 1.0);
    // A Snell-inspired convex edge: a flat centre transitions continuously into a curved rim.
    float lens = (1.0 - sqrt(max(0.0, 1.0 - band * band))) * curvature;
    float2 displacement = -normal * (lens * lensStrength + band * distortion * curvature);
    float2 samplePoint = scenePoint(p + displacement);
    float2 chroma = normal * (lens * dispersion);

    // Five taps are a deliberately modest P0 prefilter. A later tier may use
    // platform blur over a recorded GraphicsLayer instead of these samples.
    float blur = blurRadius * 0.35;
    half3 blurred = (backdrop.eval(samplePoint).rgb +
        backdrop.eval(samplePoint + float2(blur, 0.0)).rgb +
        backdrop.eval(samplePoint - float2(blur, 0.0)).rgb +
        backdrop.eval(samplePoint + float2(0.0, blur)).rgb +
        backdrop.eval(samplePoint - float2(0.0, blur)).rgb) * 0.2;
    half3 sampled;
    sampled.r = mix(blurred.r, backdrop.eval(samplePoint + chroma).r, 0.35);
    sampled.g = blurred.g;
    sampled.b = mix(blurred.b, backdrop.eval(samplePoint - chroma).b, 0.35);
    sampled = saturateColor(sampled, saturation) + half3(brightness);
    sampled = mix(sampled, tint.rgb, tint.a);

    float upperLight = smoothstep(0.15, 0.95, -normal.y) * band;
    float rim = smoothstep(0.55, 1.0, band);
    float lowerShadow = smoothstep(0.15, 0.95, normal.y) * band * shadowAlpha;
    sampled += half3(upperLight * highlightAlpha + rim * rimAlpha - lowerShadow);
    return half4(sampled, source.a);
}
"""

/** Live layers use the RenderEffect input shader as their coordinated backdrop source. */
internal val LIBRE_GLASS_LIVE_SHADER: String = LIBRE_GLASS_SHADER
    .replace("uniform shader backdrop;", "")
    .replace("backdrop.eval(", "content.eval(")
