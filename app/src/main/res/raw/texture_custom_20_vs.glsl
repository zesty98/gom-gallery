varying vec2 vTexCoord;
varying vec2 vTexCoord2;

attribute vec4 aPosition;
attribute vec2 aTexCoord;
attribute vec2 aTexCoord2;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vTexCoord = aTexCoord;
    vTexCoord2 = aTexCoord2;

    gl_Position = pos;
}
