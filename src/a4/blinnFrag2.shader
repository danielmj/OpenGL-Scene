#version 430

in vec3 vNormal, vLightDir, vVertPos, vHalfVec;
in vec4 shadow_coord;
out vec4 fragColor;

in vec2 tc;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};

struct Material
{	vec4 ambient, diffuse, specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix; 
uniform mat4 proj_matrix;
uniform mat4 normalMat;
uniform mat4 shadowMVP;
layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D s;

in vec3 coord;
uniform vec3 camera_loc;

void main(void)
{
    vec4 fog = vec4(0.5, 0.6, 0.7, 1.0);
    float fogStart = 10.0;
    float fogEnd = 50.0;
    float fogDensity = 0.025;
    float dist = length(-vVertPos.xyz);
    float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.0, 1.0);

	vec3 L = normalize(vLightDir);
	vec3 N = normalize(vNormal);
	vec3 V = normalize(-vVertPos);
	vec3 H = normalize(vHalfVec);
	
	float inShadow = textureProj(shadowTex, shadow_coord);
	
	vec4 tmpfragColor = globalAmbient * material.ambient
				+ light.ambient * material.ambient;
	
	if (inShadow != 0.0)
	{	
		tmpfragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
				+ light.specular * material.specular
				* pow(max(dot(H,N),0.0),material.shininess*3.0);
	}

	tmpfragColor =(0.5 * tmpfragColor )+(0.5 * texture2D(s,tc));

	fragColor = mix(fog,tmpfragColor,fogFactor);
}
