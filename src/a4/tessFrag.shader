#version 430


in vec2 tes_out;
out vec4 color;
in vec4 shadow_coord;
uniform mat4 mvp;
uniform vec3 camera_loc;

layout (binding=0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

uniform mat4 shadowMVP;
layout (binding=3) uniform sampler2DShadow shadowTex;

/* ---- for lighting ---- */
in vec3 varyingVertPos;
in vec3 varyingLightDir;
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
/* ---------------------- */

void main(void)
{
    //vec4 P = vec4(varyingVertPos,1.0) + vec4((tex_normal*((texture2D(tex_height,tes_out).r)/5.0f)),1.0f);
    vec3 coord = -varyingVertPos;
    vec4 fog = vec4(0.5, 0.6, 0.7, 1.0);
    float fogStart = 5.0;
    float fogEnd = 30.0;
    float fogDensity = 0.025;
    float dist = length(coord.xyz - camera_loc);
    float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.0, 1.0);

	vec3 L = normalize(varyingLightDir);

    // get the normal from the normal map
    vec3 N = texture2D(tex_normal,tes_out).rgb * 2.0 - 1.0;

    vec3 V = normalize(-varyingVertPos);
    vec3 R = normalize(reflect(-L, N));
    float cosTheta = dot(L,N);
    float cosPhi = dot(V,R);

    float inShadow = textureProj(shadowTex, shadow_coord);

    vec4 colorAmount = globalAmbient * material.ambient
                        +  light.ambient * material.ambient;

    if (inShadow != 0.0)
    {
        colorAmount += light.diffuse * material.diffuse * max(cosTheta,0.0)
                    + light.specular * material.specular
                    * pow(max(cosPhi,0.0), material.shininess);
    }

    vec4 tmpcolor = 0.5 * colorAmount
        + 0.5 *  texture2D(tex_color, tes_out);

    color = mix(fog,( tmpcolor ),fogFactor);
}