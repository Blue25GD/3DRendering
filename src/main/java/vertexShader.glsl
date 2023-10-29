
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 InColor;
layout (location = 2) in vec3 rotationAngles;
layout (location = 3) in vec3 rotationCenter;
layout (location = 4) in vec2 texCoordIn;

out vec3 pos3D;
out vec4 color;

out vec2 texCoord;

uniform vec2 resolution;

vec2 rotateOnPlane(vec2 point, float angle, vec2 center)
{
    // rotate around center
    float a = point.y - center.x;
    float b = point.x - center.y;

    float PI = 3.141592653589793;


    float r = sqrt(a * a + b * b);
    float theta;
    if (b == 0) {
        if (a > 0) {
            theta = PI / 2;
        } else {
            theta = -PI / 2;
        }
    } else {
        theta = atan(a / b);
        if (b < 0) {
            theta = PI + theta;
        }
    }
    float rotatedTheta = theta + angle;

    float ap = r * sin(rotatedTheta);
    float bp = r * cos(rotatedTheta);

    float rotatedX = bp + center.x;
    float rotatedY = ap + center.y;

    return vec2(rotatedX, rotatedY);
}

vec3 rotatePoint(vec3 point, vec3 rotationAngles, vec3 center)
{
    vec2 rotatedZ = rotateOnPlane(vec2(point.x, point.y), rotationAngles.z, vec2(center.x, center.y));
    vec3 rotatedPoint = vec3(rotatedZ.x, rotatedZ.y, point.z);

    vec2 rotatedY = rotateOnPlane(vec2(rotatedPoint.x, rotatedPoint.z), rotationAngles.y, vec2(center.x, center.z));
    rotatedPoint = vec3(rotatedY.x, rotatedPoint.y, rotatedY.y);

    vec2 rotatedX = rotateOnPlane(vec2(rotatedPoint.y, rotatedPoint.z), rotationAngles.x, vec2(center.y, center.z));
    rotatedPoint = vec3(rotatedPoint.x, rotatedX.x, rotatedX.y);
    return rotatedPoint;
}

void main()
{
    // rotate point
    vec3 rotatedPoint = rotatePoint(aPos, rotationAngles, rotationCenter);

    // fix aspect ratio thing
    float aspectRatio = resolution.x / resolution.y;
    rotatedPoint.y *= aspectRatio;

    // 3D projection
    float focalLength = 1.0;
    float xProjected = (focalLength * rotatedPoint.x) / (focalLength + rotatedPoint.z);
    float yProjected = (focalLength * rotatedPoint.y) / (focalLength + rotatedPoint.z);

    gl_Position = vec4(xProjected, yProjected, rotatedPoint.z, 1.0);
    pos3D = rotatedPoint;

    color = InColor;
    texCoord = texCoordIn;
}