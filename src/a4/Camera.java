package a4;

import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;
import graphicslib3D.Point3D;
import graphicslib3D.Vector3D;

public class Camera {
	
	private Vector3D position;
	private Vector3D u;
	private Vector3D v;
	private Vector3D n;
	
	Camera() {
		position = new Vector3D(0,-4,-11,1);
		u = new Vector3D(1,0,0);
		v = new Vector3D(0,1,0);
		n = new Vector3D(0,0,1);
	}

	public Vector3D getPosition() {
		return position;
	}

	public void apply(Matrix3D stack)
	{
		Matrix3D mat = new Matrix3D();
		mat.setToIdentity();
		mat.setRow(0, u);
		mat.setRow(1, v);
		mat.setRow(2, n);

		Matrix3D tMat = new Matrix3D();
		tMat.setToIdentity();
		tMat.setCol(3, position);

		mat.concatenate(tMat);
		stack.concatenate(mat);
	}

	public void moveZ(float amt)
	{	
		Vector3D newN = (Vector3D)n.clone();
		newN.scale(amt);
		position = position.add(newN);
		position.setW(1.0f);
	}
	
	public void moveX(float amt) 
	{
		Vector3D newU = (Vector3D)u.clone();
		newU.scale(amt);
		position = position.add(newU);
		position.setW(1.0f);
	}
	
	public void moveY(float amt)
	{
		Vector3D newV = (Vector3D)v.clone();
		newV.scale(amt);
		position = position.add(newV);
		position.setW(1.0f);
	}

	public void rotateY(float amt)
	{		
		Matrix3D mat = new Matrix3D();
		mat.rotate(amt, v);
		
		u = u.mult(mat).normalize();
		n = n.mult(mat).normalize();
	}
	
	public void rotateX(float amt) 
	{
		Matrix3D mat = new Matrix3D();
		mat.rotate(amt, u);
		
		v = v.mult(mat).normalize();
		n = n.mult(mat).normalize();
	}
}
