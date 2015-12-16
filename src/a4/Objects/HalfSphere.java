package a4.Objects;

import graphicslib3D.*;
import static java.lang.Math.*;

// this sphere is modified to be a "half" sphere for use as a skydome.

public class HalfSphere
{
	private int numVertices, numIndices, prec=48;
	private int[] indices;
	private Vertex3D[] vertices;
	
	public HalfSphere(int p)
	{	prec = p;
		InitSphere();
	}
	
	public void InitSphere()
	{	numVertices = (prec+1) * (prec+1);
		numIndices = (prec+1) * (prec) * 6;
		vertices = new Vertex3D[numVertices];
		indices = new int[numIndices];

		for (int i=0; i<numVertices; i++) { vertices[i] = new Vertex3D(); }

		// calculate triangle vertices
		for (int i=0; i<=prec; i++)
		{	for (int j=0; j<=prec; j++)
			{	float y =  (float)cos(toRadians(90-i*90/(float)(prec)));
				float x = -(float)cos(toRadians(j*360.0f/(float)(prec)))*(float)abs(cos(asin(y)));
				float z =  (float)sin(toRadians(j*360.0f/(float)(prec)))*(float)abs(cos(asin(y)));
				vertices[i*(prec+1)+j].setLocation(new Point3D(x,y,z));
				vertices[i*(prec+1)+j].setS((float)j/(float)(prec)); //(1.0f-(float)j/prec);
				vertices[i*(prec+1)+j].setT((float)i/(float)(prec)); //2.0f*(float)i/(float)prec); // for skydome map
				vertices[i*(prec+1)+j].setNormal(new Vector3D(vertices[i*(prec+1)+j].getLocation()));
		}	}
		// calculate triangle indices
		for(int i=0; i<prec; i++)
		{	for(int j=0; j<prec; j++)
			{	indices[6*(i*(prec+1)+j)+0] = i*(prec+1)+j;
				indices[6*(i*(prec+1)+j)+1] = i*(prec+1)+j+1;
				indices[6*(i*(prec+1)+j)+2] = (i+1)*(prec+1)+j;
				indices[6*(i*(prec+1)+j)+3] = i*(prec+1)+j+1;
				indices[6*(i*(prec+1)+j)+4] = (i+1)*(prec+1)+j+1;
				indices[6*(i*(prec+1)+j)+5] = (i+1)*(prec+1)+j;
	}	}	}

	public int[] getIndices()
	{	return indices;
	}

	public Vertex3D[] getVertices()
	{	return vertices;
	}
}