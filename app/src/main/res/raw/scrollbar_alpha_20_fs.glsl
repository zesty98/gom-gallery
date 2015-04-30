precision mediump float;

uniform float uAlpha;
uniform vec4 uColor;

void main() {
    gl_FragColor = uColor * uAlpha;
}
