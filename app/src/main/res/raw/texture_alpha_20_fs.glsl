precision mediump float;

varying vec2 vTexCoord;

uniform float uAlpha;
uniform sampler2D uTexture;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    color.a *= uAlpha;
    gl_FragColor = color;
}
