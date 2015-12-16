package a4.Objects;
import a4.Camera;
import graphicslib3D.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;

import java.nio.FloatBuffer;

public class ObjectGround {

	private int textureID0, textureID1, textureID2;

	public void loadTexture(GLAutoDrawable drawable) {
		ModelImporter.TextureReader tr = new ModelImporter.TextureReader();
		textureID0 = tr.loadTexture(drawable, "moon2.png");
		textureID1 = tr.loadTexture(drawable, "moon_height.jpg");
		textureID2 = tr.loadTexture(drawable, "moon-normal.png");
	}
	
	public void firstPass(GL4 gl, int rendering_program, Matrix3D mvp_matrix, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
	{
        //secondPass(gl,rendering_program,mvp_matrix,mv_matrix,proj_mat,shadowMVP);
	}

    public void secondPass(GL4 gl, int rendering_program, Matrix3D mvp_matrix, Matrix3D mv_matrix, Matrix3D proj_mat, Matrix3D shadowMVP)
    {
        int shadow_location = gl.glGetUniformLocation(rendering_program, "shadowMVP");
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP.getFloatValues(), 0);

        int mvp_location = gl.glGetUniformLocation(rendering_program, "mvp");
        int mv_location = gl.glGetUniformLocation(rendering_program, "mv_matrix");
        int proj_location = gl.glGetUniformLocation(rendering_program, "proj_matrix");
        int n_location = gl.glGetUniformLocation(rendering_program, "normalMat");
        gl.glUniformMatrix4fv(mvp_location, 1, false, mvp_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_location, 1, false, proj_mat.getFloatValues(), 0);
        gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, textureID0);
        gl.glActiveTexture(gl.GL_TEXTURE1);
        gl.glBindTexture(gl.GL_TEXTURE_2D, textureID1);
        gl.glActiveTexture(gl.GL_TEXTURE2);
        gl.glBindTexture(gl.GL_TEXTURE_2D, textureID2);

        //gl.glClear(GL_DEPTH_BUFFER_BIT);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glFrontFace(GL_CCW);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//GL_PATCHES
        gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64*64);
    }

	public void setupVertices(GL4 gl)
	{
		//Not Used. Vertices created in shaders.
	}
}
