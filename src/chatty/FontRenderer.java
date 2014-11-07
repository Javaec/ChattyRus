package chatty;

import java.awt.AlphaComposite;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class FontRenderer {


	//private BufferedImage image = createImage("ÏûùÏûù");
	public BufferedImage image;
	
	public BufferedImage imagesource;
	
	public static String str = "";

	public FontRenderer() 
	{
		
		try 
		{
			imagesource =  ImageIO.read(new File("font2.png"));
		} 
		catch (IOException e) 
		{
		}

	}

	public BufferedImage createImage(String label)
	{
		
		BufferedImage bi = new BufferedImage( (int)((imagesource.getWidth()/12)*str.length()* 0.58)+30, imagesource.getHeight()/4, BufferedImage.TYPE_INT_ARGB);
		

		Graphics2D g;
		
		g = (Graphics2D) bi.getGraphics();
		//g = bi.createGraphics();
		
		int cou = 0;
		
		for (char ch: str.toCharArray()) 
		{
			int y = getX(ch);
			int x = getY(ch);
			
			//System.out.println("toCharArray " + ch + " " + x + " " + y);
			
			//g.drawImage(imagesource, (int)Math.round(x*87.5), (int)Math.round(y*87.5), (int)Math.round(87.5), (int)Math.round(87.5), null);
			
			g.drawImage(imagesource, 
					(int)Math.round( (cou * 0.58 )*87.5), 0 , (int)Math.round(( (cou* 0.58+1))*87.5), (int)Math.round((1)*87.5), 
					(int)Math.round( (x)*87.5), (int)Math.round(y*87.25), (int)Math.round((x+1)*87.5), (int)Math.round((y+1)*87.25),
					null);
			
			cou++;
			
			//g.dispose();
		}
		
		g.dispose();
		
		
		//return bi;
		return createResizedCopy(bi, (int)(bi.getWidth()/2),(int)(bi.getHeight()/2),false);
	}
	
	
	BufferedImage createResizedCopy(Image originalImage, 
    		int scaledWidth, int scaledHeight, 
    		boolean preserveAlpha)
    {
    	//System.out.println("resizing...");
    	int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
    	BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
    	Graphics2D g = scaledBI.createGraphics();
    	if (preserveAlpha) {
    		g.setComposite(AlphaComposite.Src);
    	}
    	g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null); 
    	g.dispose();
    	return scaledBI;
    }

	
	protected int getX(char c)
	{
		int code = (int)c;
		
		//System.out.println("code " + c + " " + code);
		
		if ( (code >= 48) && (code <= 57))
		{
			return 0;
		}
		
		if ( (code >= 65) && (code <= 76))
		{
			return 1;
		}
		
		if ( (code >= 77) && (code <= 88))
		{
			return 2;
		}
		
		if ( (code >= 89) && (code <= 90))
		{
			return 3;
		}

		
		return 3;
	}
	
	
	protected int getY(char c)
	{
		int code = (int)c;
		
		if ( (code >= 48) && (code <= 57))
		{
			return code - 48;
		}
		
		if ( (code >= 65) && (code <= 76))
		{
			return code - 65;
		}
		
		if ( (code >= 77) && (code <= 88))
		{
			return code - 77;
		}
		
		if ( (code >= 89) && (code <= 90))
		{
			return code - 89;
		}
		
		
		if (code == 33)
		{
			return 2;
		}
		
		if (code == 63)
		{
			return 3;
		}
		
		if (code == 43)
		{
			return 4;
		}
		
		if (code == 45)
		{
			return 5;
		}
		
		if (code == 46)
		{
			return 6;
		}
		
		if (code == 44)
		{
			return 7;
		}
		
		if (code == 58)
		{
			return 8;
		}
		
		if (code == 32)
		{
			return 9;
		}

		
		return 9;
	}
	
	public void display(String s, String file) 
	{
		str = s.toUpperCase();
		image = createImage(str);
		
		File outputfile = new File(file);
		
		try {
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) 
	{
		EventQueue.invokeLater(new Runnable() 
		{

			@Override
			public void run() 
			{
				new FontRenderer().display("Sub today:224","image.png");
			}
		});
	}
}