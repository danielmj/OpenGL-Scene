package a4.Objects;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.Vertex3D;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;

public class ObjectClouds {

	private int[] vao = new int[1];
	private int[] vbo = new int[2];

	private HalfSphere mySphere = new HalfSphere(48);
	private int numSphereVertices;

	private int textureID;

	private float d=0.0f; // depth for 3rd dimension of 3D noise texture

	private int noiseHeight= 200;
	private int noiseWidth = 200;
	private int noiseDepth = 200;
	private double[][][] noise = new double[noiseHeight][noiseWidth][noiseDepth];
	private Random random = new Random();


	public void loadTexture(GLAutoDrawable drawable) {
		//texture = (new ModelImporter.TextureReader()).loadTexture(drawable, "leath05.jpg");
		generateNoise();
		textureID = loadNoiseTexture(drawable);
	}
	
//	public void firstPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
//	{
//
//	}

	public void secondPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
		d = d + 0.000025f; if (d>=1.0f) d=0.0f;

		int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		int d_location = gl.glGetUniformLocation(rendering_program, "d");
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_mat.getFloatValues(), 0);
		gl.glUniform1f(d_location, d);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(gl.GL_TEXTURE_3D, textureID);
        gl.glTexParameteri(gl.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glFrontFace(GL_CW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVertices);
	}

	public void setupVertices(GL4 gl)
	{
		Vertex3D[] sphere_vertices = mySphere.getVertices();
		int[] sphere_indices = mySphere.getIndices();
		float[] sphere_fvalues = new float[sphere_indices.length*3];
		float[] sphere_tvalues = new float[sphere_indices.length*2];

		for (int i=0; i<sphere_indices.length; i++)
		{	sphere_fvalues[i*3]   = (float) (sphere_vertices[sphere_indices[i]]).getX();
			sphere_fvalues[i*3+1] = (float) (sphere_vertices[sphere_indices[i]]).getY();
			sphere_fvalues[i*3+2] = (float) (sphere_vertices[sphere_indices[i]]).getZ();

			sphere_tvalues[i*2]   = (float) (sphere_vertices[sphere_indices[i]]).getS();
			sphere_tvalues[i*2+1] = (float) (sphere_vertices[sphere_indices[i]]).getT();
		}
		numSphereVertices = sphere_indices.length;

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		//  put the Sphere vertices into the first buffer,
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(sphere_fvalues);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, gl.GL_STATIC_DRAW);

		//  put the Sphere texture coordinates into the second buffer,
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer sphereTexBuf = FloatBuffer.wrap(sphere_tvalues);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, sphereTexBuf.limit()*4, sphereTexBuf, gl.GL_STATIC_DRAW);
	}


	private void fillDataArray(byte data[])
	{ for (int i=0; i<noiseHeight; i++)
	{ for (int j=0; j<noiseWidth; j++)
	{ for (int k=0; k<noiseDepth; k++)
	{ // clouds (same as above with blue hue)
		float hue = 210/360.0f;
		float sat = (float) turbulence(i,j,k,32) / 256.0f;
		float bri = 60/100.0f;
		int rgb = Color.HSBtoRGB(hue,sat,bri);
		Color c = new Color(rgb);
		data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+0] = (byte) c.getRed();
		data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+1] = (byte) c.getGreen();
		data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+2] = (byte) c.getBlue();
		data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+3] = (byte) 0;
	} } } }

	private int loadNoiseTexture(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();


		byte[] data = new byte[noiseHeight*noiseWidth*noiseDepth*4];

		ByteBuffer bb = ByteBuffer.allocate(noiseHeight*noiseWidth*noiseDepth*4);

		fillDataArray(data);

		bb = ByteBuffer.wrap(data);

		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];

		gl.glBindTexture(gl.GL_TEXTURE_3D, textureID);

		gl.glTexStorage3D(gl.GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
		gl.glTexSubImage3D(gl.GL_TEXTURE_3D, 0, 0, 0, 0,
				noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, bb);

		gl.glTexParameteri(gl.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		return textureID;
	}

	void generateNoise()
	{	for (int x=0; x<noiseHeight; x++)
	{	for (int y=0; y<noiseWidth; y++)
	{	for (int z=0; z<noiseDepth; z++)
	{	noise[x][y][z] = random.nextDouble();
	}	}	}	}

	double smoothNoise(double x1, double y1, double z1)
	{	//get fractional part of x, y, and z
		double fractX = x1 - (int) x1;
		double fractY = y1 - (int) y1;
		double fractZ = z1 - (int) z1;

		//neighbor values
		int x2 = ((int)x1 + noiseWidth - 1) % noiseWidth;
		int y2 = ((int)y1 + noiseHeight- 1) % noiseHeight;
		int z2 = ((int)z1 + noiseDepth - 1) % noiseDepth;

		//smooth the noise by interpolating
		double value = 0.0;
		value += fractX     * fractY     * fractZ     * noise[(int)x1][(int)y1][(int)z1];
		value += fractX     * (1-fractY) * fractZ     * noise[(int)x1][(int)y2][(int)z1];
		value += (1-fractX) * fractY     * fractZ     * noise[(int)x2][(int)y1][(int)z1];
		value += (1-fractX) * (1-fractY) * fractZ     * noise[(int)x2][(int)y2][(int)z1];

		value += fractX     * fractY     * (1-fractZ) * noise[(int)x1][(int)y1][(int)z2];
		value += fractX     * (1-fractY) * (1-fractZ) * noise[(int)x1][(int)y2][(int)z2];
		value += (1-fractX) * fractY     * (1-fractZ) * noise[(int)x2][(int)y1][(int)z2];
		value += (1-fractX) * (1-fractY) * (1-fractZ) * noise[(int)x2][(int)y2][(int)z2];

		return value;
	}

	private double turbulence(double x, double y, double z, double size)
	{	double value = 0.0, initialSize = size;
		while(size >= 0.9)
		{	value = value + smoothNoise(x/size, y/size, z/size) * size;
			size = size / 2.0;
		}
		value = value/initialSize;
		value = 256.0 * logistic(value * 128.0 - 120.0);
		return value;
	}

	private double logistic(double x)
	{	double k = 0.2;
		return (1.0/(1.0+Math.pow(2.718,-k*x)));
	}
}
