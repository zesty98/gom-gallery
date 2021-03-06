varying vec4 vColor;

attribute vec4 aPosition;
attribute vec4 aColor;

uniform highp mat4 uPMatrix;
uniform highp mat4 uMMatrix;
uniform highp mat4 uVMatrix;

void main() {
    vec4 pos = uPMatrix * uVMatrix * uMMatrix * aPosition;

    vColor = aColor;

    gl_Position = pos;
}
