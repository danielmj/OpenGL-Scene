package a4.Objects;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.jogamp.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;

public class ModelImporter
{
  private ArrayList<Float> verts = new ArrayList<Float>();
  private ArrayList<Float> tris = new ArrayList<Float>(); 
  private ArrayList<Float> tcs = new ArrayList<Float>();
  private ArrayList<Float> UVs = new ArrayList<Float>();
  private ArrayList<Float> normals = new ArrayList<Float>();
  private ArrayList<Float> normVals = new ArrayList<Float>();

  public void parseOBJ(String filename) throws IOException
  { 
	System.out.println(filename);
	InputStream input = ModelImporter.class.getResourceAsStream(filename);
    BufferedReader br = new BufferedReader(new InputStreamReader(input));
    String line;
    while ((line = br.readLine()) != null)
    { if(line.startsWith("v "))
      { for(String s : line.split(" "))
        { if(s.matches("^[^a-z]+$") && !s.matches("\\s*"))
          { verts.add(Float.valueOf(s));
      } } }
      else if(line.startsWith("vt"))
      { for(String s : line.split(" "))
        { if(s.matches("^[^a-z]+$") && !s.matches("\\s*"))
          { UVs.add(Float.valueOf(s));
      } } }
      else if(line.startsWith("vn"))
      { for(String s : line.split(" "))
        { if(s.matches("^[^a-z]+$") && !s.matches("\\s*"))
          { normVals.add(Float.valueOf(s));
      } } }
      else if(line.startsWith("f"))
      {	for(String s : line.split(" "))
        { if(s.matches("^[^a-z]+$") && !s.matches("\\s*"))
          { 
        	String[] splitStr = s.split("/");
        	
        	String v = splitStr[0];
        	int vertNum = (Integer.valueOf(v)-1)*3;
			tris.add(verts.get(vertNum));
            tris.add(verts.get((vertNum)+1));
            tris.add(verts.get((vertNum)+2));
            tris.add(1f);
          
        	if (splitStr.length >= 2)
        	{
        		String vt = splitStr[1];
        		int tcNum   = (Integer.valueOf(vt));
        		tcs.add(UVs.get((tcNum-1)*2));
                tcs.add(UVs.get((tcNum-1)*2+1));
        	}
        	
            if (splitStr.length >= 3)
            {
            	String vn = splitStr[2];
            	int normNum = (Integer.valueOf(vn)-1)*3;
            	normals.add(normVals.get(normNum));
                normals.add(normVals.get((normNum)+1));
                normals.add(normVals.get((normNum)+2));
                normals.add(1f);
            }
    } } } }
    input.close();
  }
	
  public float[] getVertices()
  { float[] f = new float[tris.size()];
    for(int i = 0; i < tris.size(); i++)
    { f[i] = tris.get(i);
    }
    return f;
  }
	
  public int getNumVertices() { return (tris.size()/4); }

  public ArrayList<Float> getTris() { return tris; }
  public ArrayList<Float> getTcs() { return tcs; }

  public float[] getTextureCoordinates()
  { float[] f = new float[(tcs.size())];
    for(int i = 0; i < tcs.size(); i++)
    { f[i] = tcs.get(i);
    }
    return f;
  }
	
  public float[] getNormals()
  { float[] f = new float[(normals.size())];
    for(int i = 0; i < normals.size(); i++)
    { f[i] = normals.get(i);
    }
    return f;
  }

  public static class TextureReader
  {
      public TextureReader() {}

      public int loadTexture(GLAutoDrawable drawable, String fileName)
      {	GL gl = drawable.getGL();

          String textureFileName = TextureReader.class.getResource(fileName).getPath();

          BufferedImage textureImage = getBufferedImage(textureFileName);
          byte[] imgRGBA = getRGBAPixelData(textureImage);
          ByteBuffer wrappedRGBA = ByteBuffer.wrap(imgRGBA);

          int[] textureIDs = new int[1];
          gl.glGenTextures(1, textureIDs, 0);
          int textureID = textureIDs[0];

          // make the textureID the "current texture"
          gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);

          // attach the image texture to the currently active OpenGL texture ID
          gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, 		// MIPMAP Level
                  GL.GL_RGBA, 			// number of color components
                  textureImage.getWidth(), 	// image size
                  textureImage.getHeight(), 0, 	// border size in pixels
                  GL.GL_RGBA, 			// pixel format
                  GL.GL_UNSIGNED_BYTE, 		// pixel data type
                  wrappedRGBA 			// buffer holding texture data
          );

          // if no mipmapping
          // gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

          // if mipmapping (tri-linear)
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
          gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

          // if also anisotropic filtering
          if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
          {	float aniso[] = new float[1];
              gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, aniso, 0);
              gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso[0]);
          }

          return textureID;
      }

      /**
       * Read the specified file and return a BufferedImage containing the file's
       * content. The file is assumed to be in a format understood by ImageIO.read
       * (GIF, JPG, PNG, or BMP).
       */
      public BufferedImage getBufferedImage(String fileName)
      {	BufferedImage img;
          try {
              img = ImageIO.read(new File(fileName));
          } catch (IOException e) {
              System.err.println("Error reading '" + fileName + '"');
              throw new RuntimeException(e);
          }
          return img;
      }

      /**
       * Extract pixel data from the specified BufferedImage and return it as an
       * an array of bytes in OpenGL-compatible format for use by glDrawPixels().
       * To provide glDrawPixels() with a suitable array, we need to do the
       * following: -Convert to a color model that OpenGL understands. -Use a byte
       * array to keep the color values properly arranged. -Flip the image
       * vertically so that the row order puts the bottom of the image on the
       * first row.
       */
      public byte[] getRGBAPixelData(BufferedImage img)
      {	byte[] imgRGBA;
          int height = img.getHeight(null);
          int width = img.getWidth(null);

          // create an (empty) BufferedImage with a suitable Raster and ColorModel
          WritableRaster raster = Raster.createInterleavedRaster(
                  DataBuffer.TYPE_BYTE, width, height, 4, null);
          ComponentColorModel colorModel = new ComponentColorModel(
                  ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 }, // bits
                  true,  				// hasAlpha
                  false, 				// isAlphaPreMultiplied
                  ComponentColorModel.TRANSLUCENT,// transparency
                  DataBuffer.TYPE_BYTE); 		// data transfertype
          BufferedImage newImage = new BufferedImage(colorModel,
                  raster, false, 			// isRasterPremultiplied
                  null); 				// properties

          // Since Java expects images to have their origin at the upper left
          // while OpenGL expects the origin at the lower left,
          // we must flip the image upside down prior to extracting the RGBA image data.
          // We create an AffineTransform to perform the flipping,
          // and use the Graphics object for the new image to draw the old image
          // into the new one, applying the AffineTransform as it draws
          // (i.e. "upside down" in the Java sense, which will make it rightside up in the OpenGL sense).

          AffineTransform gt = new AffineTransform();
          gt.translate(0, height);
          gt.scale(1, -1d);

          Graphics2D g = newImage.createGraphics();
          g.transform(gt);

          g.drawImage(img, null, null); // draw original image into new image
          g.dispose();

          // now retrieve the underlying byte array from the raster data buffer
          DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
          imgRGBA = dataBuf.getData();

          return imgRGBA;
      }
  }
}
