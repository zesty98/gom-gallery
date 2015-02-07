precision mediump float;

varying vec4 vColor;

uniform float uAlpha;

void main() {
    gl_FragColor = vColor * uAlpha;
}
