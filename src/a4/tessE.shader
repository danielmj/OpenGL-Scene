#version 430

//layout (quads, equal_spacing,ccw) in;
layout (quads, fractional_even_spacing,ccw) in;

uniform mat4 mvp;
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normsssal;

layout (location = 3) uniform vec3 camera_loc;

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
out vec3 varyingVertPos;
out vec3 varyingLightDir;
/*-----------------*/

in vec2 tcs_out[];
out vec2 tes_out;

uniform mat4 shadowMVP;
out vec4 shadow_coord;

void main (void)
{
    vec2 tc1 = mix(tcs_out[0], tcs_out[1], gl_TessCoord.x);
	vec2 tc2 = mix(tcs_out[2], tcs_out[3], gl_TessCoord.x);
	vec2 tc = mix(tc2, tc1, gl_TessCoord.y);

	// map the tessellated grid onto the texture rectangle:
	vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);
	vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);
	vec4 p = mix(p2, p1, gl_TessCoord.y);
	
	// add the height from the height map to the vertex:
	p.y = p.y + (texture(tex_height, tc).r)*8.0;
	
	gl_Position = mvp * p;
	tes_out = tc;

	/*--- light stuff----*/
    varyingVertPos = (mv_matrix * p).xyz;
    varyingLightDir = light.position - varyingVertPos;

    shadow_coord = shadowMVP * p;
}