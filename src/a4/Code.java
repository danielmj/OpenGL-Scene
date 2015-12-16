package a4;

import a4.Objects.*;
import com.jogamp.opengl.glu.GLU;
import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.GLSLUtils.*;

import java.awt.*;
import java.nio.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.shape.*;
import com.jogamp.opengl.util.*;

public class Code extends JPanel implements GLEventListener
{
    graphicslib3D.Material thisMaterial;
    private String[] vBlinn1ShaderSource, vBlinn2ShaderSource, fBlinn2ShaderSource;
    private GLCanvas myCanvas;
    private int rendering_program1, rendering_program2, lightbox_program;
    private int tess_rendering_program2, cloud_program, bump_rendering_program1, bump_rendering_program2;
    private int vaoID[] = new int[1];
    private int [] bufferIDs = new int [6];
    private int mv_location, proj_location, vertexLoc, n_location;
    float aspect;
    private GLSLUtils util = new GLSLUtils();

    // location of torus and camera
    graphicslib3D.Point3D torusLoc = new graphicslib3D.Point3D(1.6, 0.0, -0.3);
    graphicslib3D.Point3D pyrLoc = new graphicslib3D.Point3D(-1.0, 0.1, 0.3);
    graphicslib3D.Point3D cameraLoc = new graphicslib3D.Point3D(0.0, 0.2, 6.0);
    graphicslib3D.Point3D lightLoc = new graphicslib3D.Point3D(-3.8f, 2.2f, 5.1f);

    Matrix3D m_matrix = new Matrix3D();
    Matrix3D v_matrix = new Matrix3D();
    Matrix3D mv_matrix = new Matrix3D();
    Matrix3D proj_matrix = new Matrix3D();
    Matrix3D mvp_matrix = new Matrix3D();

    // light stuff
    float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
    PositionalLight currentLight = new PositionalLight();

    // shadow stuff
    int scSizeX, scSizeY;
    int [] shadow_tex = new int [1];
    int [] shadow_buffer = new int [1];
    Matrix3D lightV_matrix = new Matrix3D();
    Matrix3D lightP_matrix = new Matrix3D();
    Matrix3D shadowMVP1 = new Matrix3D();
    Matrix3D shadowMVP2 = new Matrix3D();
    Matrix3D b = new Matrix3D();

    private Camera camera = new Camera();
    ObjectChair myChair = new ObjectChair();
    ObjectGround myGround = new ObjectGround();
    ObjectTV myTV = new ObjectTV();
    ObjectClouds clouds = new ObjectClouds();
    ObjectDonut myDonut = new ObjectDonut();
    ObjectLightBox lightBox = new ObjectLightBox();

    private int[] samplers = new int[1];

    float cloudRotationAmt = 0.0f;

    // model stuff
    int numPyramidVertices, numPyramidIndices;
    ImportedModel pyramid = new ImportedModel("pyr.obj");
    int numTorusVertices;

    int errorCheckCount = 0;

    public Code() {
        Border border = new LineBorder(Color.black, 2);
        setBorder(border);
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        setBackground(Color.BLUE);
        this.setLayout(new BorderLayout());

        //Create and set the GLCanvas
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        add(myCanvas);

        FPSAnimator animator = new FPSAnimator(myCanvas, 60);
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
//        useAxes = !useAxes;
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
//        useDirectionalLight = !useDirectionalLight;
    }

    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();

        errorCheckCount++;
        if(errorCheckCount == 300) {
            errorCheckCount = 0;
            if (checkOpenGLError(drawable)) {
                printProgramLog(drawable, cloud_program);
            }
        }

        aspect = myCanvas.getWidth() / myCanvas.getHeight();
        proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

        FloatBuffer bg = FloatBuffer.allocate(4);
        // vec4(0.7, 0.8, 0.9, 1.0)
        bg.put(0, 0.5f); bg.put(1, 0.5f); bg.put(2, 0.5f); bg.put(3, 1.0f);
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


        //----- CAMERA-----//
        v_matrix.setToIdentity();
        camera.apply(v_matrix);

        double camX = -camera.getPosition().getX()+lightLoc.getX();
        double camY = -camera.getPosition().getY()+lightLoc.getY();
        double camZ = lightLoc.getZ();
        currentLight.setPosition(new Point3D(camX,camY,camZ));//

        //----- CLOUDS-----//
        gl.glUseProgram(cloud_program);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(-(float)camera.getPosition().getX(),-(float)camera.getPosition().getY()-0.5f,-(float)camera.getPosition().getZ());//0,-0.5f,0);
        cloudRotationAmt = cloudRotationAmt + 0.015f;
        m_matrix.rotateY(cloudRotationAmt);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        clouds.secondPass(gl,cloud_program,mv_matrix,proj_matrix,null);
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        //----- END CLOUDS-----//


        gl.glDrawBuffer(GL.GL_FRONT);

        secondPass(drawable);
    }

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public void firstPass(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();

        gl.glUseProgram(rendering_program1);

        Point3D origin = new Point3D(0.0, 0.0, 0.0);
        Vector3D up = new Vector3D(0.0, 1.0, 0.0);
        lightV_matrix.setToIdentity();
        lightP_matrix.setToIdentity();

        lightV_matrix = lookAt(currentLight.getPosition(), origin, up);	// vector from light to origin
        lightP_matrix = perspective(90.0f, aspect, 0.1f, 10000.0f);

        gl.glClear(GL_DEPTH_BUFFER_BIT);

//        // draw the torus
//
//        m_matrix.setToIdentity();
//        m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
//        m_matrix.rotateX(25.0);
//
//        shadowMVP1.setToIdentity();
//        shadowMVP1.concatenate(lightP_matrix);
//        shadowMVP1.concatenate(lightV_matrix);
//        shadowMVP1.concatenate(m_matrix);
//        int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
//        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
//
//        // set up torus vertices buffer
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIDs[0]);
//        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
//        gl.glEnableVertexAttribArray(0);
//
//        gl.glClear(GL_DEPTH_BUFFER_BIT);
//        gl.glEnable(GL_CULL_FACE);
//        gl.glFrontFace(GL_CCW);
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthFunc(GL_LEQUAL);
//
//        gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

        // ---- draw the pyramid
//        gl.glClear(GL_DEPTH_BUFFER_BIT);


        gl.glUseProgram(rendering_program1);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(-2.5f,1.7f,0.5f);
        m_matrix.rotateY(245.0f);
        m_matrix.scale(0.01,0.01,0.01);

        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);

        myChair.firstPass(gl,rendering_program1,mv_matrix,proj_matrix,shadowMVP1);



        gl.glUseProgram(rendering_program1);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(2.5f,1.7f,-0.0f);
        m_matrix.rotateY(200.0f);
        m_matrix.scale(0.01,0.01,0.01);

        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);

        myChair.firstPass(gl,rendering_program1,mv_matrix,proj_matrix,shadowMVP1);




        gl.glUseProgram(rendering_program1);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(0.05f,1.01f,4.5f);
        m_matrix.scale(2.5,2.5,2.5);
        m_matrix.rotate(0.0,91.5,0.0);
        shadowMVP1.setToIdentity();
        shadowMVP1.concatenate(lightP_matrix);
        shadowMVP1.concatenate(lightV_matrix);
        shadowMVP1.concatenate(m_matrix);
        myTV.firstPass(gl,rendering_program1,mv_matrix,proj_matrix,shadowMVP1);



//        gl.glUseProgram(bump_rendering_program1);
//        //  build the MODEL matrix
//        m_matrix.setToIdentity();
//        m_matrix.translate(3.05f,0.01f,1.5f);
//        m_matrix.scale(2.5,2.5,2.5);
//        m_matrix.rotate(0.0,91.5,0.0);
//
//        shadowMVP1.setToIdentity();
//        shadowMVP1.concatenate(lightP_matrix);
//        shadowMVP1.concatenate(lightV_matrix);
//        shadowMVP1.concatenate(m_matrix);
//
//        myDonut.firstPass(gl,bump_rendering_program1,mv_matrix,proj_matrix,shadowMVP1);
    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    public void secondPass(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();

        //  build the VIEW matrix

        gl.glUseProgram(rendering_program2);

        thisMaterial = Material.SILVER;
        installLights(rendering_program2, v_matrix, drawable);

        mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
        proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
        n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
        int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");

        // draw the torus

        //  put the MV and PROJ matrices into the corresponding uniforms
        gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
        gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

        // draw the pyramid

        thisMaterial = Material.SILVER;
        installLights(rendering_program2, v_matrix, drawable);

        gl.glUseProgram(rendering_program2);

        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(-2.5f,1.7f,0.5f);
        m_matrix.rotateY(245.0f);
        m_matrix.scale(0.01,0.01,0.01);

        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);

        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);

        myChair.secondPass(gl,rendering_program2,mv_matrix,proj_matrix,shadowMVP2);


        // draw the pyramid

        thisMaterial = Material.SILVER;
        installLights(rendering_program2, v_matrix, drawable);
        gl.glUseProgram(rendering_program2);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(2.5f,1.7f,-0f);
        m_matrix.rotateY(200.0f);
        m_matrix.scale(0.01,0.01,0.01);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        //Shadow
        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);
        myChair.secondPass(gl,rendering_program2,mv_matrix,proj_matrix,shadowMVP2);



        //------- TV ------//
        thisMaterial = Material.SILVER;
        installLights(rendering_program2, v_matrix, drawable);
        gl.glUseProgram(rendering_program2);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(0.05f,1.01f,4.5f);
        m_matrix.scale(2.5,2.5,2.5);
        m_matrix.rotate(0.0,91.5,0.0);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);
        myTV.secondPass(gl,rendering_program2,mv_matrix,proj_matrix,shadowMVP2);




        //----- DONUT ------//
        thisMaterial = Material.GOLD;
        installLights(bump_rendering_program2, v_matrix, drawable);
        gl.glUseProgram(bump_rendering_program2);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(0.05f,2.9f,4.5f);
        m_matrix.scale(2.5,2.5,2.5);
        m_matrix.rotate(0.0,91.5,0.0);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);
        myDonut.secondPass(gl,bump_rendering_program2,mv_matrix,proj_matrix,shadowMVP2);



        //----- LIGHTBOX-----//
        gl.glUseProgram(lightbox_program);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(-camera.getPosition().getX()+lightLoc.getX(),
                -camera.getPosition().getY()+lightLoc.getY(),lightLoc.getZ());
        m_matrix.scale(0.5,0.5,0.5);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        lightBox.secondPass(gl,lightbox_program,mv_matrix,proj_matrix,null);
        //----- END LIGHTBOX-----//





        gl.glUseProgram(tess_rendering_program2);
        thisMaterial = graphicslib3D.Material.SILVER;
        installLights(tess_rendering_program2, v_matrix, drawable);
        //  build the MODEL matrix
        m_matrix.setToIdentity();
        m_matrix.translate(0f,-0.5f,0f);
        //  build the MODEL-VIEW matrix
        mv_matrix.setToIdentity();
        mv_matrix.concatenate(v_matrix);
        mv_matrix.concatenate(m_matrix);
        //MVP
        mvp_matrix.setToIdentity();
        mvp_matrix.concatenate(proj_matrix);
        mvp_matrix.concatenate(v_matrix);
        mvp_matrix.concatenate(m_matrix);
        //Shadow
        shadowMVP2.setToIdentity();
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightP_matrix);
        shadowMVP2.concatenate(lightV_matrix);
        shadowMVP2.concatenate(m_matrix);
        myGround.secondPass(gl,tess_rendering_program2,mvp_matrix,mv_matrix,proj_matrix,shadowMVP2);
    }

    public void init(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();
        createShaderPrograms(drawable);
        setupVertices(gl, drawable);
        setupShadowBuffers(drawable);

        b.setElementAt(0,0,0.5);b.setElementAt(0,1,0.0);b.setElementAt(0,2,0.0);b.setElementAt(0,3,0.5f);
        b.setElementAt(1,0,0.0);b.setElementAt(1,1,0.5);b.setElementAt(1,2,0.0);b.setElementAt(1,3,0.5f);
        b.setElementAt(2,0,0.0);b.setElementAt(2,1,0.0);b.setElementAt(2,2,0.5);b.setElementAt(2,3,0.5f);
        b.setElementAt(3,0,0.0);b.setElementAt(3,1,0.0);b.setElementAt(3,2,0.0);b.setElementAt(3,3,1.0f);

//         may reduce shadow border artifacts
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

//        int tx_loc = gl.glGetUniformLocation(rendering_program2, "s");
//        gl.glGenSamplers(1, samplers,0);
//        gl.glBindSampler(1, tx_loc);

        myChair.loadTexture(drawable);
        myGround.loadTexture(drawable);
        myTV.loadTexture(drawable);
        clouds.loadTexture(drawable);
        myDonut.loadTexture(drawable);
        lightBox.loadTexture(drawable);
    }

    public void setupShadowBuffers(GLAutoDrawable drawable)
    {
        GL4 gl = (GL4) drawable.getGL();

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

    // -----------------------------
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) //{}
    {
        setupShadowBuffers(drawable);
    }

    private void setupVertices(GL4 gl, GLAutoDrawable drawable)
    {
        myChair.setupVertices(gl);
        myGround.setupVertices(gl);
        myTV.setupVertices(gl);
        clouds.setupVertices(gl);
        myDonut.setupVertices(gl);
        lightBox.setupVertices(gl);


        // pyramid definition
        Vertex3D[] pyramid_vertices = pyramid.getVertices();
        int[] pyramid_indices = pyramid.getIndices();
        numPyramidVertices = pyramid.getNumVertices();
        numPyramidIndices = pyramid.getNumIndices();
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

    public static void main(String[] args) { new Code(); }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {	GL4 gl = (GL4) drawable.getGL();
        gl.glDeleteVertexArrays(1, vaoID, 0);
    }

    //-----------------
    private void createShaderPrograms(GLAutoDrawable drawable)
    {
        String[] objShaders = new String[] {"blinnVert1.shader"};
        int[] shaderTypes = new int[] {GL4.GL_VERTEX_SHADER};
        rendering_program1 = createProgram(drawable, objShaders,shaderTypes);

        String[] objShaders2 = new String[] {"blinnVert2.shader", "blinnFrag2.shader"};
        shaderTypes = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        rendering_program2 = createProgram(drawable, objShaders2, shaderTypes);


        //Tessellation
        String tvertShader = "tessVert.shader";
        String tfragShader = "tessFrag.shader";
        String teShader = "tessE.shader";
        String tcShader = "tessC.shader";
        String[] tessShaders = new String[] {tvertShader,tcShader,teShader,tfragShader};
        shaderTypes = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_TESS_CONTROL_SHADER, GL4.GL_TESS_EVALUATION_SHADER, GL4.GL_FRAGMENT_SHADER};
        tess_rendering_program2 = createProgram(drawable, tessShaders, shaderTypes);

        String[] objShaders3 = new String[] {"cloudVert.shader", "cloudFrag.shader"};
        int[] shaderTypes3 = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        cloud_program = createProgram(drawable, objShaders3, shaderTypes3);

        objShaders3 = new String[] {"bumpVert.shader"};
        shaderTypes3 = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        bump_rendering_program1 = createProgram(drawable, objShaders3, shaderTypes3);

        objShaders3 = new String[] {"bumpVert2.shader", "bumpFrag2.shader"};
        shaderTypes3 = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        bump_rendering_program2 = createProgram(drawable, objShaders3, shaderTypes3);

        objShaders3 = new String[] {"lightBoxVert.shader","lightBoxFrag.shader"};
        shaderTypes3 = new int[] {GL4.GL_VERTEX_SHADER, GL4.GL_FRAGMENT_SHADER};
        lightbox_program = createProgram(drawable, objShaders3, shaderTypes3);
    }

    //------------------
    private Matrix3D perspective(float fovy, float aspect, float n, float f) {	float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
        float A = q / aspect;
        float B = (n + f) / (n - f);
        float C = (2.0f * n * f) / (n - f);
        Matrix3D r = new Matrix3D();
        r.setElementAt(0,0,A);
        r.setElementAt(1,1,q);
        r.setElementAt(2,2,B);
        r.setElementAt(3,2,-1.0f);
        r.setElementAt(2,3,C);
        r.setElementAt(3,3,0.0f);
        return r;
    }

    private Matrix3D lookAt(graphicslib3D.Point3D eyeP, graphicslib3D.Point3D centerP, Vector3D upV) {	Vector3D eyeV = new Vector3D(eyeP);
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