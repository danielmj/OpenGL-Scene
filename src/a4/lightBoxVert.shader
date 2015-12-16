#version 430

layout (location=0) in vec3 position;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

out vec4 vColor;

void main(void)
{
	gl_Position = proj_matrix * mv_matrix * vec4(position,1.0);
	
	vColor = vec4(position,1.0)*2.0 + vec4(0.5, 0.5, 0.5, 0.0);
} 
