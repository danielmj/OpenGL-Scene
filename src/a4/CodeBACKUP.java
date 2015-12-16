package a4;

import a4.Objects.ImportedModel;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import graphicslib3D.*;
import graphicslib3D.light.PositionalLight;
import graphicslib3D.shape.Torus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;

public class CodeBACKUP extends JPanel implements GLEventListener
{
	private GLCanvas myCanvas;

    // Programs
	private int tess_rendering_program2;
    private int rendering_program1;
    private int rendering_program2;
	private int axes_program;

    // Object Stuff
//	private ObjectChair firstObj = new ObjectChair();
//	private ObjectTV secondObj = new ObjectTV();
//	private ObjectGround thirdObj = new ObjectGround();
    private int[] samplers = new int[1];
	private boolean useAxes = false;


    private int mv_location, proj_location, vertexLoc, n_location;
    // model stuff
    int numPyramidVertices, numPyramidIndices;
    ImportedModel pyramid = new ImportedModel("pyr.obj");;
    Torus myTorus = new Torus(0.6f, 0.4f, 48);
    int numTorusVertices;
    // location of torus and camera
    Point3D torusLoc = new Point3D(1.6, 0.0, -0.3);
    Point3D pyrLoc = new Point3D(-1.0, 0.1, 0.3);
    Point3D cameraLoc = new Point3D(0.0, 0.2, 6.0);
    Point3D lightLoc = new Point3D(-3.8f, 2.2f, 1.1f);
    private int vaoID[] = new int[1];
    private int [] bufferIDs = new int [6];





    // View Matrix Stuff
	private Camera camera;
    float aspect;

    // Shadow Stuff
    int scSizeX, scSizeY;
    int [] shadow_tex = new int [1];
    int [] shadow_buffer = new int [1];
    Matrix3D lightV_matrix = new Matrix3D();
    Matrix3D lightP_matrix = new Matrix3D();
    Matrix3D shadowMVP1 = new Matrix3D();
    Matrix3D shadowMVP2 = new Matrix3D();
    Matrix3D b = new Matrix3D();

    //Matrix Model Stuff
	private Matrix3D m_matrix = new Matrix3D();
	private Matrix3D v_matrix = new Matrix3D();
	private Matrix3D mv_matrix = new Matrix3D();
	private Matrix3D proj_matrix = new Matrix3D();
	private Matrix3D mvp_matrix = new Matrix3D();

    //Light & Materials
	Material thisMaterial = Material.SILVER;
    private boolean useDirectionalLight = true;
	private PositionalLight currentLight = new PositionalLight();
//	private Point3D lightLoc = new Point3D(5.0f, 2.0f, 2.0f);
	float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };

	public CodeBACKUP()
	{
		Border border = new LineBorder(Color.black,2);
		setBorder(border);
		GridBagLayout layout = new GridBagLayout();
		setLayout (layout);
		setBackground(Color.BLUE);
		this.setLayout(new BorderLayout());

		//Create and set the GLCanvas
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		add(myCanvas);

		camera = new Camera();

		FPSAnimator animator = new FPSAnimator(myCanvas,60);
		animator.start();
	}

	public GLCanvas getCanvas() {
		return myCanvas;
	}
	public void moveForward() {
		camera.moveZ(0.25f);
	}
	public void moveLeft() {
		camera.moveX(0.25f);
	}
	public void moveRight() {
		camera.moveX(-0.25f);
	}
	public void moveBack()
	{
		camera.moveZ(-0.25f);
	}
	public void moveUp() {
//		cameraY += 0.25;
		camera.moveY(0.25f);
	}
	public void moveDown() {
//		cameraY -= 0.25;
		camera.moveY(-0.25f);
	}
	public void rotateLeft()
	{
		camera.rotateY(2f);
	}
	public void rotateRight()
	{
		camera.rotateY(-2f);
	}
	public void rotateUp()
	{
		camera.rotateX(-2);
	}
	public void rotateDown()
	{
		camera.rotateX(2);
	}
	public void toggleAxes()
	{
		useAxes = !useAxes;
	}
	public Point3D getLightLoc() {
		return lightLoc;
	}
	public void setLightLoc(Point3D newLightLoc)
	{
		lightLoc = newLightLoc;
	}
	public void toggleDirectionalLight()
	{
		useDirectionalLight = !useDirectionalLight;
	}

	public void display(GLAutoDrawable drawable)
	{
        GL4 gl = (GL4) drawable.getGL();

        currentLight.setPosition(lightLoc);
        aspect = myCanvas.getWidth() / myCanvas.getHeight();
        proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

        FloatBuffer bg = FloatBuffer.allocate(4);
        bg.put(0, 0.0f); bg.put(1, 0.0f); bg.put(2, 0.2f); bg.put(3, 1.0f);
        gl.glClearBufferfv(GL_COLOR,0,bg);

        float depthClearVal[] = new float[1]; depthClearVal[0] = 1.0f;
        gl.glClearBufferfv(GL_DEPTH,0,depthClearVal,0);

        gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);

        gl.glDrawBuffer(GL.GL_NONE);
        gl.glEnable(GL_DEPTH_TEST);

        gl.glEnable(GL_POLYGON_OFFSET_FILL);	// for reducing
        gl.glPolygonOffset(2.0f, 4.0f);			//  shadow artifacts

        firstPass(drawable);

        gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued

        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glActiveTexture(gl.GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);

        gl.glDrawBuffer(GL.GL_FRONT);

        secondPass(drawable);
	}


    public void firstPass(GLAutoDrawable drawable)
    {	GL4 gl = (GL4) drawable.getGL();

        gl.glUseProgram(rendering_program1);

        Point3D origin = new Point3D(0.0, 0.0, 0.0);
        Vector3D up = new Vector3D(0.0, 1.0, 0.0);
        lightV_matrix.setToIdentity();
        lightP_matrix.setToIdentity();

        lightV_matrix = lookAt(currentLight.getPosition(), origin, up);	// vector from light to origin
        lightP_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

        // draw the torus

        m_matrix.setToIdentity();
        m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
        m_matrix.rotateX(25.0);

        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);
        int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

        // set up torus vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

        // ---- draw the pyramid

        gl.glUseProgram(rendering_program1);
        mv_location = gl.glGetUniformLocation(rendering_program1, "mv_matrix");
        proj_location = gl.glGetUniformLocation(rendering_program1, "proj_matrix");

        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
        m_matrix.rotateX(30.0);
        m_matrix.rotateY(40.0);

        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);

        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

        // set up vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    public void secondPass(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();

        gl.glUseProgram(rendering_program2);

        // draw the torus

        thisMaterial = Material.BRONZE;
        installLights(rendering_program2, v_matrix, drawable);

        mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
        proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
        n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
        int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");

        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
        double amt = (double)(System.currentTimeMillis()%36000)/100.0;
        m_matrix.rotateX(25.0);

        //  build the VIEW matrix
        v_matrix.setToIdentity();
        v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());

        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);

        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);

        //  put the MV and PROJ matrices into the corresponding uniforms
        gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

        // set up torus vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // set up torus normals buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[2]);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

        // draw the pyramid

        thisMaterial = Material.GOLD;
        installLights(rendering_program2, v_matrix, drawable);

        gl.glUseProgram(rendering_program2);
        mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
        proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
        n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");

        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
        m_matrix.rotateX(30.0);
        m_matrix.rotateY(40.0);

        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);

        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

        //  put the MV and PROJ matrices into the corresponding uniforms
        gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);

        // set up vertices buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // set up normals buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[3]);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
    }


//    public void firstPassTess(GLAutoDrawable drawable) {
//        GL4 gl = (GL4) drawable.getGL();
//
//        gl.glUseProgram(tess_rendering_program2);
//
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//
//        mvp_matrix.setToIdentity();
//        mvp_matrix.concatenate(proj_matrix);
//        mvp_matrix.concatenate(v_matrix);
//        mvp_matrix.concatenate(m_matrix);
//
//        thirdObj.firstPass(gl,tess_rendering_program2, mvp_matrix,mv_matrix,proj_matrix,shadowMVP1);
//    }
//
//    public void firstPass(GLAutoDrawable drawable) {
//
//    m_matrix.setToIdentity();
//    m_matrix.translate(0.0f, 0.0f, -3.0f);
//    m_matrix.rotateX(20.0f);
//
//        GL4 gl = (GL4) drawable.getGL();
//
//        gl.glUseProgram(rendering_program1);
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(-2.5f,1.7f,-3.5f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,245.0,0.0);
//
//        firstObj.firstPass(gl,rendering_program1, mv_matrix, proj_matrix, shadowMVP1);
//
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(2.5f,1.7f,-3.5f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,200.0,0.0);
//
//        firstObj.firstPass(gl,rendering_program1, mv_matrix, proj_matrix, shadowMVP1);
//
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(0.5f,2.3f,-1.0f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,200.0,0.0);
//
//        firstObj.firstPass(gl,rendering_program1, mv_matrix, proj_matrix, shadowMVP1);
//
//
//        //Position and draw - TV
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(0.05f,0.01f,0.0f);
//        mv_matrix.scale(2.5,2.5,2.5);
//        mv_matrix.rotate(0.0,91.5,0.0);
//
//        secondObj.firstPass(gl,rendering_program1, mv_matrix, proj_matrix, shadowMVP1);
//    }

//    public void secondPassTess(GLAutoDrawable drawable) {
//
//        GL4 gl = (GL4) drawable.getGL();
//        gl.glUseProgram(tess_rendering_program2);
//
//        //FOG - Send Camera Location
//        int cam_loc = gl.glGetUniformLocation(tess_rendering_program2, "camera_loc");
//        float camCoord[] = new float[] {
//                (float)camera.getPosition().getX(),
//                (float)camera.getPosition().getY(),
//                (float)camera.getPosition().getZ()
//        };
//        gl.glProgramUniform3fv(tess_rendering_program2, cam_loc, 1, camCoord, 0);
//
//        thisMaterial = graphicslib3D.Material.SILVER;
//        currentLight.setPosition(lightLoc);
//        installLights(tess_rendering_program2, v_matrix, drawable);
//
//        shadowMVP2.setToIdentity();
//        shadowMVP2.concatenate(b);
//        shadowMVP2.concatenate(lightP_matrix);
//        shadowMVP2.concatenate(lightV_matrix);
//        shadowMVP2.concatenate(m_matrix);
//
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//
//        mvp_matrix.setToIdentity();
//        mvp_matrix.concatenate(proj_matrix);
//        mvp_matrix.concatenate(v_matrix);
//        mvp_matrix.concatenate(m_matrix);
//
//        thirdObj.secondPass(gl,tess_rendering_program2, mvp_matrix,mv_matrix,proj_matrix,shadowMVP2);
//    }
//
//    public void secondPass(GLAutoDrawable drawable) {
//
//        GL4 gl = (GL4) drawable.getGL();
//        gl.glUseProgram(rendering_program2);
//
//        //FOG - Send Camera Location
//        int cam_loc = gl.glGetUniformLocation(rendering_program2, "camera_loc");
//        float camCoord[] = new float[] {
//                (float)camera.getPosition().getX(),
//                (float)camera.getPosition().getY(),
//                (float)camera.getPosition().getZ()
//        };
//        gl.glProgramUniform3fv(rendering_program2, cam_loc, 1, camCoord, 0);
//
//        thisMaterial = graphicslib3D.Material.SILVER;
//        currentLight.setPosition(lightLoc);
//        installLights(rendering_program2, v_matrix, drawable);
//
//        shadowMVP2.setToIdentity();
//        shadowMVP2.concatenate(b);
//        shadowMVP2.concatenate(lightP_matrix);
//        shadowMVP2.concatenate(lightV_matrix);
//        shadowMVP2.concatenate(m_matrix);
//
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(-2.5f,1.7f,-3.5f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,245.0,0.0);
//
//        firstObj.secondPass(gl,rendering_program2, mv_matrix, proj_matrix, shadowMVP2);
//
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(2.5f,1.7f,-3.5f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,200.0,0.0);
//
//        firstObj.secondPass(gl,rendering_program2, mv_matrix, proj_matrix, shadowMVP2);
//
//
//        //Position and draw CHAIR2
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(0.5f,2.0f,-1.0f);
//        mv_matrix.scale(0.01,0.01,0.01);
//        mv_matrix.rotate(0.0,200.0,0.0);
//
//        firstObj.secondPass(gl,rendering_program2, mv_matrix, proj_matrix, shadowMVP2);
//
//
//        //Position and draw - TV
//        mv_matrix.setToIdentity();
//        mv_matrix.concatenate(v_matrix);
//        mv_matrix.concatenate(m_matrix);
//        mv_matrix.translate(0.05f,0.01f,0.0f);
//        mv_matrix.scale(2.5,2.5,2.5);
//        mv_matrix.rotate(0.0,91.5,0.0);
//
//        secondObj.secondPass(gl,rendering_program2, mv_matrix, proj_matrix, shadowMVP2);
//    }

	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) drawable.getGL();
        createShaderPrograms(drawable);
		setupVertices(gl, drawable);
        setupShadowBuffers(drawable);

//		int tx_loc = gl.glGetUniformLocation(tess_rendering_program2, "s");
//		gl.glGenSamplers(1, samplers,0);
//		gl.glBindSampler(0, tx_loc);
//
//        tx_loc = gl.glGetUniformLocation(rendering_program2, "s");
//        gl.glGenSamplers(1, samplers,0);
//        gl.glBindSampler(0, tx_loc);

        b.setElementAt(0,0,0.5);b.setElementAt(0,1,0.0);b.setElementAt(0,2,0.0);b.setElementAt(0,3,0.5f);
        b.setElementAt(1,0,0.0);b.setElementAt(1,1,0.5);b.setElementAt(1,2,0.0);b.setElementAt(1,3,0.5f);
        b.setElementAt(2,0,0.0);b.setElementAt(2,1,0.0);b.setElementAt(2,2,0.5);b.setElementAt(2,3,0.5f);
        b.setElementAt(3,0,0.0);b.setElementAt(3,1,0.0);b.setElementAt(3,2,0.0);b.setElementAt(3,3,1.0f);

        // may reduce shadow border artifacts
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

//		firstObj.loadTexture(drawable);
//		secondObj.loadTexture(drawable);
//		thirdObj.loadTexture(drawable);
	}

    public void setupShadowBuffers(GLAutoDrawable drawable)
    {	GL4 gl = (GL4) drawable.getGL();

        scSizeX = myCanvas.getWidth();
        scSizeY = myCanvas.getHeight();

        gl.glGenFramebuffers(1, shadow_buffer, 0);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);

        gl.glGenTextures(1, shadow_tex, 0);
        gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
                scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
    }

    private void installLights(int rendering_program, Matrix3D v_matrix, GLAutoDrawable drawable)
    {	GL4 gl = (GL4) drawable.getGL();

        Material currentMaterial = new Material(); currentMaterial = thisMaterial;

        Point3D lightP = currentLight.getPosition();
        Point3D lightPv = lightP.mult(v_matrix);

        float [] currLightPos = new float[] { (float) lightPv.getX(),
                (float) lightPv.getY(),
                (float) lightPv.getZ() };

        // get the location of the global ambient light field in the shader
        int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");

        // set the current globalAmbient settings
        gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);

        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
        int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

        int MambLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
        int MdiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
        int MspecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
        int MshiLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");

        // set the uniform light and material values in the shader
        gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
        gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
        gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
        gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);

        gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
        gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
        gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
        gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
    }


//	private void setupVertices(GL4 gl) {
//		firstObj.setupVertices(gl);
//		secondObj.setupVertices(gl);
//		thirdObj.setupVertices(gl);
//	}

    private void setupVertices(GL4 gl, GLAutoDrawable drawable)
    {
        // pyramid definition
        Vertex3D[] pyramid_vertices = pyramid.getVertices();
        int[] pyramid_indices = pyramid.getIndices();
        numPyramidVertices = pyramid.getNumVertices();
        numPyramidIndices = pyramid.getNumIndices();

        float[] pyramid_vertex_positions = new float[numPyramidIndices*3];
        float[] pyramid_texture_coordinates = new float[numPyramidIndices*2];
        float[] pyramid_normals = new float[numPyramidIndices*3];
        float[] pyramid_cvalues = new float[numPyramidIndices*3];

        for (int i=0; i<numPyramidIndices; i++)
        {	pyramid_vertex_positions[i*3]   = (float) (pyramid_vertices[pyramid_indices[i]]).getX();
            pyramid_vertex_positions[i*3+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getY();
            pyramid_vertex_positions[i*3+2] = (float) (pyramid_vertices[pyramid_indices[i]]).getZ();

            pyramid_texture_coordinates[i*2]   = (float) (pyramid_vertices[pyramid_indices[i]]).getS();
            pyramid_texture_coordinates[i*2+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getT();

            pyramid_normals[i*3]   = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalX();
            pyramid_normals[i*3+1] = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalY();
            pyramid_normals[i*3+2] = (float) (pyramid_vertices[pyramid_indices[i]]).getNormalZ();
        }

        for (int i=0; i<numPyramidIndices/2; i++)
        {	pyramid_cvalues[i*3] = 1.0f; pyramid_cvalues[i*3+1] = 0.0f; pyramid_cvalues[i*3+2] = 0.0f;
        }
        for (int i=numPyramidIndices/2; i<numPyramidIndices; i++)
        {	pyramid_cvalues[i*3] = 0.0f; pyramid_cvalues[i*3+1] = 1.0f; pyramid_cvalues[i*3+2] = 0.0f;
        }

        Vertex3D[] torus_vertices = myTorus.getVertices();

        int[] torus_indices = myTorus.getIndices();
        float[] torus_fvalues = new float[torus_indices.length*3];
        float[] torus_tvalues = new float[torus_indices.length*2];
        float[] torus_nvalues = new float[torus_indices.length*3];
        float[] torus_cvalues = new float[torus_indices.length*3];

        for (int i=0; i<torus_indices.length; i++)
        {	torus_fvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getX();
            torus_fvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getY();
            torus_fvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getZ();

            torus_tvalues[i*2]   = (float) (torus_vertices[torus_indices[i]]).getS();
            torus_tvalues[i*2+1] = (float) (torus_vertices[torus_indices[i]]).getT();

            torus_nvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getNormalX();
            torus_nvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getNormalY();
            torus_nvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getNormalZ();
        }
        for (int i=0; i<torus_indices.length/2; i++)
        {	torus_cvalues[i*3] = 1.0f; torus_cvalues[i*3+1] = 0.0f; torus_cvalues[i*3+2] = 0.0f;
        }
        for (int i=torus_indices.length/2; i<torus_indices.length; i++)
        {	torus_cvalues[i*3] = 0.0f; torus_cvalues[i*3+1] = 1.0f; torus_cvalues[i*3+2] = 0.0f;
        }

        numTorusVertices = torus_indices.length;

        gl.glGenVertexArrays(vaoID.length, vaoID, 0);
        gl.glBindVertexArray(vaoID[0]);

        gl.glGenBuffers(6, bufferIDs, 0);

        //  put the Torus vertices into the first buffer,
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
        FloatBuffer vertBuf = FloatBuffer.wrap(torus_fvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL.GL_STATIC_DRAW);

        //  load the pyramid vertices into the second buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[1]);
        FloatBuffer pyrVertBuf = FloatBuffer.wrap(pyramid_vertex_positions);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, pyrVertBuf.limit()*4, pyrVertBuf, GL.GL_STATIC_DRAW);

        // load the torus normal coordinates into the third buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[2]);
        FloatBuffer torusNorBuf = FloatBuffer.wrap(torus_nvalues);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL.GL_STATIC_DRAW);

        // load the pyramid normal coordinates into the fourth buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[3]);
        FloatBuffer pyrNorBuf = FloatBuffer.wrap(pyramid_normals);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL.GL_STATIC_DRAW);
    }

	private Matrix3D perspective(float fovy, float aspect, float n, float f) {	float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		Matrix3D rt = new Matrix3D();
		r.setElementAt(0,0,A);
		r.setElementAt(1,1,q);
		r.setElementAt(2,2,B);
		r.setElementAt(2,3,-1.0f);
		r.setElementAt(3, 3, 0.0f);
		r.setElementAt(3,2,C);
		rt = r.transpose();
		return rt;
	}

    private Matrix3D lookAt(Point3D eyeP, Point3D centerP, Vector3D upV)
    {	Vector3D eyeV = new Vector3D(eyeP);
        Vector3D cenV = new Vector3D(centerP);
        Vector3D f = (cenV.minus(eyeV)).normalize();
        Vector3D sV = (f.cross(upV)).normalize();
        Vector3D nU = (sV.cross(f)).normalize();

        Matrix3D l = new Matrix3D();
        l.setElementAt(0,0,sV.getX());l.setElementAt(0,1,nU.getX());l.setElementAt(0,2,-f.getX());l.setElementAt(0,3,0.0f);
        l.setElementAt(1,0,sV.getY());l.setElementAt(1,1,nU.getY());l.setElementAt(1,2,-f.getY());l.setElementAt(1,3,0.0f);
        l.setElementAt(2,0,sV.getZ());l.setElementAt(2,1,nU.getZ());l.setElementAt(2,2,-f.getZ());l.setElementAt(2,3,0.0f);
        l.setElementAt(3,0,sV.dot(eyeV.mult(-1)));
        l.setElementAt(3,1,nU.dot(eyeV.mult(-1)));
        l.setElementAt(3,2,(f.mult(-1)).dot(eyeV.mult(-1)));
        l.setElementAt(3,3,1.0f);
        return(l.transpose());
    }

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        setupShadowBuffers(drawable);
    }
	public void dispose(GLAutoDrawable drawable) {
        GL4 gl = (GL4) drawable.getGL();
        gl.glDeleteVertexArrays(1, vaoID, 0);

    }

    private void createShaderPrograms(GLAutoDrawable drawable) {
        String axesVertShader = "axes_vert.shader";
        String axesFragShader = "axes_frag.shader";

        String tvertShader = "tessVert.shader";
        String tfragShader = "tessFrag.shader";
        String teShader = "tessE.shader";
        String tcShader = "tessC.shader";


        //Tessellation
//        String[] tessShaders = new String[] {tvertShader,tcShader,teShader,tfragShader};
//        int[] shaderTypes = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_TESS_CONTROL_SHADER, GL4.GL_TESS_EVALUATION_SHADER, GL4.GL_FRAGMENT_SHADER};
//        tess_rendering_program2 = createProgram(drawable, tessShaders, shaderTypes);

        String[] objShaders = new String[] {"blinnVert1.shader"};
        int[] shaderTypes = new int[] {GL4.GL_VERTEX_SHADER};
        rendering_program1 = createProgram(drawable, objShaders,shaderTypes);

        String[] objShaders2 = new String[] {"blinnVert2.shader", "blinnFrag1.shader"};
        shaderTypes = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        rendering_program2 = createProgram(drawable, objShaders2, shaderTypes);

//        String[] axesShaders = new String[] {axesVertShader, axesFragShader};
//        shaderTypes = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
//        axes_program = createProgram(drawable, axesShaders, shaderTypes);
    }

    private int createProgram(GLAutoDrawable drawable, String[] shaders, int[] shaderTypes) {
        GL4 gl = (GL4) drawable.getGL();

        int program = gl.glCreateProgram();
        int shaderList[] = new int[shaders.length];

        for(int shaderIndex = 0; shaderIndex < shaders.length; shaderIndex++)
        {
            int[] compiled = new int[1];

            int lengths[];
            String shaderFile = shaders[shaderIndex];
            String path = CodeBACKUP.class.getResource(shaderFile).getPath();
            String shaderSource[] = GLSLUtils.readShaderSource(path);
            //Record length of shader file
            lengths = new int[shaderSource.length];
            for (int i = 0; i < lengths.length; i++) {
                lengths[i] = shaderSource[i].length();
            }

            //Create & compile the shader
            int shader = gl.glCreateShader(shaderTypes[shaderIndex]);
            gl.glShaderSource(shader, shaderSource.length, shaderSource, lengths, 0);
            gl.glCompileShader(shader);

            //Check for vShader compiler errors
            checkOpenGLError(drawable);  // can use returned boolean
            gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 1) {
                System.out.println(shaderFile + " shader compilation success");
            } else {
                System.out.println(shaderFile + " shader compilation failed");
                printShaderLog(drawable, shader);
            }

            shaderList[shaderIndex] = shader;
        }

        //------------ linking ----------//
        int[] linked = new int[1];
        //Attach & Link the shaders
        for(int i = 0; i < shaderList.length; i++) {
            gl.glAttachShader(program, shaderList[i]);
        }
        gl.glLinkProgram(program);
        //Check for linker errors
        checkOpenGLError(drawable);
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 1) {
            System.out.println("linking succeeded");
        } else {
            System.out.println("linking failed");
            printProgramLog(drawable, program);
        }

        return program;
    }

	/**
	 * Check open GL errors
//	 * @param drawable
//	 * @return
	 */
	boolean checkOpenGLError(GLAutoDrawable drawable) {
		GL4 gl = (GL4) drawable.getGL();
		boolean foundError = false;
		GLU glu = new GLU();
		int glErr = gl.glGetError();
		while (glErr != GL.GL_NO_ERROR)
		{ 
			System.err.println("glError: " + glu.gluErrorString(glErr));
			foundError = true;
			glErr = gl.glGetError();
		}
		return foundError;
	}
	
	/**
	 * Print shader log
	 * @param drawable
	 * @param shader
	 */
	private void printShaderLog(GLAutoDrawable drawable, int shader) {
		GL4 gl = (GL4) drawable.getGL();
		int[] len = new int[1];
		int[] chWrittn = new int[1];
		byte[] log = null;
		// determine the length of the shader compilation log
		gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, len, 0);
		if (len[0] > 0)
		{ 
			log = new byte[len[0]];
			gl.glGetShaderInfoLog(shader, len[0], chWrittn, 0, log, 0);
			System.out.println("Shader Info Log: ");
			for (int i = 0; i < log.length; i++)
			{ 
				System.out.print((char) log[i]);
			}
		}
	}
	
	/**
	 * Print program log
	 * @param drawable
	 * @param prog
	 */
	void printProgramLog(GLAutoDrawable drawable, int prog) {
		GL4 gl = (GL4) drawable.getGL();
		int[] len = new int[1];
		int[] chWrittn = new int[1];
		byte[] log = null;
		// determine the length of the program compilation log
		gl.glGetProgramiv(prog,GL4.GL_INFO_LOG_LENGTH,len, 0);
		if (len[0] > 0)
		{ 
			log = new byte[len[0]];
			gl.glGetProgramInfoLog(prog, len[0], chWrittn, 0,log, 0);
			System.out.println("Program Info Log: ");
			for (int i = 0; i < log.length; i++)
			{ 
				System.out.print((char) log[i]);
			}
		}
	}
}