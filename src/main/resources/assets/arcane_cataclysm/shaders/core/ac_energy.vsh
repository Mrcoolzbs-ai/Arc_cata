#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out vec4 vertexColor;
out vec3 viewNormal;
out vec3 viewDir;
out vec3 worldPos;
void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    texCoord = UV0;
    vertexColor = Color;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    viewDir = normalize(-vp.xyz);
    worldPos = Position;
}
