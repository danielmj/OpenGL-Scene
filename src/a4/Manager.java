package a4;

import java.awt.event.*;
import javax.swing.*;
import graphicslib3D.Point3D;

public class Manager extends JFrame implements MouseMotionListener, MouseWheelListener {

    private static final long serialVersionUID = -1150909258012403144L;
    private Code ss;

    public Manager() {
        setTitle("a1");
        setSize(1000, 1000);

        //Create Panels
        layoutCenterPanel();

        setupKeys();

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setVisible(true);
    }

    private void setupKeys()
    {
        ActionMap actionMap = ss.getActionMap();
        InputMap inputMap = ss.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put( KeyStroke.getKeyStroke("UP"), "keyUp" );
        inputMap.put( KeyStroke.getKeyStroke("DOWN"), "keyDown" );
        inputMap.put( KeyStroke.getKeyStroke("LEFT"), "keyLeft" );
        inputMap.put( KeyStroke.getKeyStroke("RIGHT"), "keyRight" );
        inputMap.put( KeyStroke.getKeyStroke("W"), "keyW" );
        inputMap.put( KeyStroke.getKeyStroke("S"), "keyS" );
        inputMap.put( KeyStroke.getKeyStroke("A"), "keyA" );
        inputMap.put( KeyStroke.getKeyStroke("D"), "keyD" );
        inputMap.put( KeyStroke.getKeyStroke("Q"), "keyQ" );
        inputMap.put( KeyStroke.getKeyStroke("E"), "keyE" );
        inputMap.put( KeyStroke.getKeyStroke("SPACE"), "keySpace" );
        inputMap.put( KeyStroke.getKeyStroke("L"), "keyL" );
        actionMap.put("keyUp",keyUp());
        actionMap.put("keyDown",keyDown());
        actionMap.put("keyLeft", keyLeft());
        actionMap.put("keyRight", keyRight());
        actionMap.put("keyW", keyW());
        actionMap.put("keyS", keyS());
        actionMap.put("keyA", keyA());
        actionMap.put("keyD", keyD());
        actionMap.put("keyQ", keyQ());
        actionMap.put("keyE", keyE());
        actionMap.put("keySpace", keySpace());
        actionMap.put("keyL", keyL());
    }

    private Action keyUp() {
       return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.rotateUp();
            }
        };
    }
    private Action keyDown() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.rotateDown();
            }
        };
    }
    private Action keyLeft() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.rotateLeft();
            }
        };
    }
    private Action keyRight() { return new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            ss.rotateRight();
        }
    }; }
    private Action keyW() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveForward();
            }
        };
    }
    private Action keyS() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveBack();
            }
        };
    }
    private Action keyA() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveLeft();
            }
        };
    }
    private Action keyD() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveRight();
            }
        };
    }
    private Action keyQ() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveUp();
            }
        };
    }
    private Action keyE() {

        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.moveDown();
            }
        };
    }
    private Action keySpace() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.toggleAxes();
            }
        };
    }
    private Action keyL() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ss.toggleDirectionalLight();
            }
        };
    }
    /**
     * Create and add a Triangle object to the center panel
     */
    private void layoutCenterPanel() {
        ss = new Code();
       // ss.addKeyListener(this);
        ss.setFocusable(true);

        ss.getCanvas().addMouseWheelListener(this);
        ss.getCanvas().addMouseMotionListener(this);

        getContentPane().add(ss);
    }


    @Override
    public void mouseDragged(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        float x = e.getX();
        float y = e.getY();

        float yPercent = (this.getHeight() != 0) ? x / (float)this.getHeight() : 0;
        float xPercent = (this.getWidth() != 0) ? y / (float)this.getWidth(): 0;

        float maxX = 10.0f;
        float maxY = 10.0f;

        float newY = 5.0f-(float)maxY * yPercent;
        float newX = 5.0f-(float)maxX * xPercent;
        if (newY < -5.0) newY = -5.0f;
        if (newY > 5.0) newY = 5.0f;
        if (newX < -5.0) newX = -5.0f;
        if (newX > 5.0) newX = 5.0f;

        ss.setLightLoc(new Point3D(-newY, newX, ss.getLightLoc().getZ()));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0) {
        // TODO Auto-generated method stub
        System.out.print(arg0.getWheelRotation());
        Point3D lastLoc = ss.getLightLoc();
        float newZ = (float) lastLoc.getZ();
        if(arg0.getWheelRotation() < 0) {
            newZ += 1.0;
            //if (newZ >= 5.0) newZ = 5.0f;
        }
        else {
            newZ -= 1.0;
            //if (newZ <= -5.0) newZ = -5.0f;
        }
        ss.setLightLoc(new Point3D(lastLoc.getX(),lastLoc.getY(),newZ));
    }
}

