precision mediump float;

varying vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float uAlpha;
uniform vec4 uDefaultColor;

vec4 DEFAULT_COLOR = vec4(0.8, 0.8, 0.8, 1.0);

void main() {
    vec4 texColor = texture2D(uTexture, vTexCoord);
//    gl_FragColor = uDefaultColor * (1.0 - uAlpha) + texColor * uAlpha;
    gl_FragColor = DEFAULT_COLOR * (1.0 - uAlpha) + texColor * uAlpha;
}
