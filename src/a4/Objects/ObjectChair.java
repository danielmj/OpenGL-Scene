package a4.Objects;

import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL4.*;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import graphicslib3D.Matrix3D;
import graphicslib3D.Vertex3D;

public class ObjectChair {

	private int[] vao = new int[1];
	private int[] vbo = new int[3];
	private int texture;

	private int numObjVertices, numObjIndices;
	private ImportedModel myObj = new ImportedModel("Armchair.obj");
	
	private boolean useNormals = true;
	
	public void loadTexture(GLAutoDrawable drawable) {
		texture = (new ModelImporter.TextureReader()).loadTexture(drawable, "leath05.jpg");
	}
	
	public void firstPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
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

//        gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		int numVerts = myObj.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}

	public void secondPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
		int shadow_location = gl.glGetUniformLocation(rendering_program, "shadowMVP");

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
		gl.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		//if(useNormals) {
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
		//}

        gl.glActiveTexture(gl.GL_TEXTURE1);
        gl.glBindTexture(gl.GL_TEXTURE_2D, texture);

//        gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP.getFloatValues(), 0);

		int numVerts = myObj.getIndices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}
	
	public void setupVertices(GL4 gl)
	{
		Vertex3D[] vertices = myObj.getVertices();
		int[] indices = myObj.getIndices();
		numObjVertices = myObj.getNumVertices();
		numObjIndices = myObj.getNumIndices();

		float[] fvalues = new float[numObjIndices*3];
		float[] tvalues = new float[numObjIndices*2];
		float[] nvalues = new float[numObjIndices*3];

		for (int i=0; i<numObjIndices; i++)
		{	fvalues[i*3]   = (float) (vertices[indices[i]]).getX();
			fvalues[i*3+1] = (float) (vertices[indices[i]]).getY();
			fvalues[i*3+2] = (float) (vertices[indices[i]]).getZ();
			tvalues[i*2]   = (float) (vertices[indices[i]]).getS();
			tvalues[i*2+1] = (float) (vertices[indices[i]]).getT();
			nvalues[i*3]   = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i*3+1] = (float) (vertices[indices[i]]).getNormalY();
			nvalues[i*3+2] = (float) (vertices[indices[i]]).getNormalZ();
		}

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(fvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer texBuf = FloatBuffer.wrap(tvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL.GL_STATIC_DRAW);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer norBuf = FloatBuffer.wrap(nvalues);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL.GL_STATIC_DRAW);
	}
}
