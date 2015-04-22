precision mediump float;

varying vec2 vTexCoord;
varying vec2 vTexCoord2;

uniform sampler2D uTexture;
uniform sampler2D uTexture2;

uniform float uAlpha;
uniform vec4 uDefaultColor;
uniform int uUseCheckTexture;

vec4 DEFAULT_COLOR = vec4(0.8, 0.8, 0.8, 1.0);

void main() {
    vec4 dst = texture2D(uTexture, vTexCoord);
    dst = DEFAULT_COLOR * (1.0 - uAlpha) + dst * uAlpha;
    vec4 color;

    if (uUseCheckTexture == 1) {
        vec4 src = texture2D(uTexture2, vTexCoord2);
        color = src * src.a + dst * (1.0 - src.a);
    } else {
        color = dst;
    }

    gl_FragColor = color;
}
