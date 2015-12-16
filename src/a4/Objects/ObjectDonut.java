package a4.Objects;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.Vertex3D;
import graphicslib3D.shape.Torus;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;

public class ObjectDonut {

	private int[] vao = new int[1];
	private int[] vbo = new int[2];
	private int texture;

	private Torus myTorus = new Torus(0.125f, 0.05f, 48);
	private int numTorusVertices;

	public void loadTexture(GLAutoDrawable drawable) {
		//texture = (new ModelImporter.TextureReader()).loadTexture(drawable, "leath05.jpg");
	}
	
	public void firstPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
//		int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
//		int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
//		int n_location = gl.glGetUniformLocation(rendering_program, "normalMat");
//		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
//		gl.glUniformMatrix4fv(proj_location, 1, false, proj_mat.getFloatValues(), 0);
//		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(),0);
//
//		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
//		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//		gl.glEnableVertexAttribArray(0);
//
////		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
////		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
////		gl.glEnableVertexAttribArray(1);
//
//		gl.glClear(GL_DEPTH_BUFFER_BIT);
//		gl.glEnable(GL_CULL_FACE);
//		gl.glFrontFace(GL_CCW);
//		gl.glEnable(GL_DEPTH_TEST);
//		gl.glDepthFunc(GL_LEQUAL);

//		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);
	}

	public void secondPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
		int shadow_location = gl.glGetUniformLocation(rendering_program, "shadowMVP");
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP.getFloatValues(), 0);

		int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		int n_location = gl.glGetUniformLocation(rendering_program, "normalMat");
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_mat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(),0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);
	}

	public void setupVertices(GL4 gl)
	{
		Vertex3D[] vertices = myTorus.getVertices();
		int[] indices = myTorus.getIndices();

		float[] fvalues = new float[indices.length*3];
		float[] tvalues = new float[indices.length*2];
		float[] nvalues = new float[indices.length*3];

		for (int i=0; i<indices.length; i++)
		{	fvalues[i*3] = (float) (vertices[indices[i]]).getX();
			fvalues[i*3+1] = (float) (vertices[indices[i]]).getY();
			fvalues[i*3+2] = (float) (vertices[indices[i]]).getZ();
			tvalues[i*2] = (float) (vertices[indices[i]]).getS();
			tvalues[i*2+1] = (float) (vertices[indices[i]]).getT();
			nvalues[i*3] = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i*3+1]= (float)(vertices[indices[i]]).getNormalY();
			nvalues[i*3+2]=(float) (vertices[indices[i]]).getNormalZ();
		}

		numTorusVertices = indices.length;

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL.GL_STATIC_DRAW);
	}
	
}
