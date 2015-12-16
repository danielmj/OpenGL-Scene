#version 430

in vec4 shadow_coord;

layout (binding = 0) uniform sampler2DShadow shadow_tex;
 
layout (location = 0) out vec4 fragColor;

void main(void)
{	//fragColor = vec4(1.0,0.0,0.0,0.0);
}
