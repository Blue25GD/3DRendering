#version 330 core
out vec4 FragColor;

in vec3 pos3D;
in vec4 color;

in vec2 texCoord;

uniform sampler2D textureData;

void main()
{
    FragColor = texture(textureData, texCoord);
//    FragColor = vec4(1, 1, 1, 1.0f);
//    FragColor = vec4(pos3D.x, pos3D.y, pos3D.z, 1.0f);
}