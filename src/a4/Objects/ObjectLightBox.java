package a4.Objects;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import graphicslib3D.Matrix3D;
import graphicslib3D.Vertex3D;
import graphicslib3D.shape.Torus;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;

public class ObjectLightBox {

	private int[] vao = new int[1];
	private int[] vbo = new int[2];
	private int texture;

	private int numTorusVertices;

	public void loadTexture(GLAutoDrawable drawable) {
		//texture = (new ModelImporter.TextureReader()).loadTexture(drawable, "leath05.jpg");
	}
	
	public void firstPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
	}

	public void secondPass(GL4 gl, int rendering_program, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
		int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");
		gl.glUniformMatrix4fv(mv_loc, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, proj_mat.getFloatValues(), 0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
	}

	public void setupVertices(GL4 gl)
	{
		float[] vertex_positions =
				{-0.25f,  0.25f, -0.25f, -0.25f, -0.25f, -0.25f, 0.25f, -0.25f, -0.25f,
						0.25f, -0.25f, -0.25f, 0.25f,  0.25f, -0.25f, -0.25f,  0.25f, -0.25f,
						0.25f, -0.25f, -0.25f, 0.25f, -0.25f,  0.25f, 0.25f,  0.25f, -0.25f,
						0.25f, -0.25f,  0.25f, 0.25f,  0.25f,  0.25f, 0.25f,  0.25f, -0.25f,
						0.25f, -0.25f,  0.25f, -0.25f, -0.25f,  0.25f, 0.25f,  0.25f,  0.25f,
						-0.25f, -0.25f,  0.25f, -0.25f,  0.25f,  0.25f, 0.25f,  0.25f,  0.25f,
						-0.25f, -0.25f,  0.25f, -0.25f, -0.25f, -0.25f, -0.25f,  0.25f,  0.25f,
						-0.25f, -0.25f, -0.25f, -0.25f,  0.25f, -0.25f, -0.25f,  0.25f,  0.25f,
						-0.25f, -0.25f,  0.25f,  0.25f, -0.25f,  0.25f,  0.25f, -0.25f, -0.25f,
						0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f, -0.25f,  0.25f,
						-0.25f,  0.25f, -0.25f, 0.25f,  0.25f, -0.25f, 0.25f,  0.25f,  0.25f,
						0.25f,  0.25f,  0.25f, -0.25f,  0.25f,  0.25f, -0.25f,  0.25f, -0.25f
				};

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = FloatBuffer.wrap(vertex_positions);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);
	}
	
}
