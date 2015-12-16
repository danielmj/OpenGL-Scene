#version 430

layout (vertices = 4) out;


in vec2 tc[];
out vec2 tcs_out[];

uniform mat4 mvp;
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;


/*--- light stuff----*/
struct PositionalLight
{	vec4 ambient; vec4 diffuse; vec4 specular; vec3 position; };
struct Material
{	vec4 ambient; vec4 diffuse; vec4 specular; float shininess; };
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 normalMat;
/*-----------------*/

void main(void)
{	if (gl_InvocationID == 0)
	{	vec4 p0 = normalize(mvp * gl_in[0].gl_Position);
		vec4 p1 = normalize(mvp * gl_in[1].gl_Position);
		vec4 p2 = normalize(mvp * gl_in[2].gl_Position);
		vec4 p3 = normalize(mvp * gl_in[3].gl_Position);
		float l0 = length(p2.xy - p0.xy) * 16.0 + 1.0;
		float l1 = length(p3.xy - p2.xy) * 16.0 + 1.0;
		float l2 = length(p3.xy - p1.xy) * 16.0 + 1.0;
		float l3 = length(p1.xy - p0.xy) * 16.0 + 1.0;
		gl_TessLevelOuter[0] = l0;
		gl_TessLevelOuter[1] = l1;
		gl_TessLevelOuter[2] = l2;
		gl_TessLevelOuter[3] = l3;
		gl_TessLevelInner[0] = min(l1,l3);
		gl_TessLevelInner[1] = min(l0,l2);
	}
	
	tcs_out[gl_InvocationID] = tc[gl_InvocationID];
	gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
}