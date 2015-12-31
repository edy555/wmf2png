/*
    WmfView.java
    Copyright (C) 1997,2001 Albrecht Kleine <kleine@ak.sax.de>
 
    version 0.6 
    (bugfix in META_POLYGON token, August 2001,
     tnx to Ivan Markovic for demo files)
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;


public class WmfView extends Frame 
{
    Image image;
   static long startTime;

    WmfViewImageCanvas ic;
    private final int TERMINATE_NOW=123456789;
 
    static public void main(String[] args) 
    {
       boolean batchmode=false;
       if (args.length > 0)
	 { 	 
		 WmfView.startTime = System.currentTimeMillis();
		 new WmfView(args[0],false,false);
	 }
       else
	 System.out.println("Usage: java WmfView wmffile1");
    }

    // constructor
    WmfView(String filename,boolean batchmode,boolean ready)
    {
        super("WmfView");
        Image ii;
        ic = new WmfViewImageCanvas();
        add("North", ic);

        if (filename!=null)
        {
	   ii=CreateWmfDecoder(filename);
	   if (ii!=null)
	     {
		if (!batchmode)
		  {
		     ic.setImage(ii);
		     pack();
		     show();
		  }
	  System.out.println(System.currentTimeMillis()-startTime);		
	     }  
 	   else
 	    System.out.println("error creating WMF decoder class");
        } 
    }


   private Image CreateWmfDecoder(String filename)
   {
        Image image;
        try
	{
	   WmfDecoder WDec=new WmfDecoder(new FileInputStream(filename));
           image = createImage(WDec);
	   WDec=null;
	}
        catch (Exception ex)
	{
           ex.printStackTrace();
	   return null;
	}
        return image;
   }


}


// ----simple mini control for an image display------------------------------
//
class WmfViewImageCanvas extends Canvas 
{
    private Image image;
   

    WmfViewImageCanvas() 
    {
        this.image = null;
    }

    public Dimension preferredSize()
    {
      int w,h;
      if (image!=null)
	 {
	    w = image.getWidth(this);
	    h = image.getHeight(this);
         }
      else
	 {
	    w=200;		// for writing an error message
	    h=100;
	 }
       
     return new Dimension(w,h);
    }

    public Image getImage()
    {
     return image;
    }

    public void saveImage(Image image)
    {
        int w = 640, h = 480;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics big = bi.getGraphics();
        big.drawImage(image, 0, 0, null);

        File saveFile = new File("converted.png");
        try {
            ImageIO.write(bi, "png", saveFile);
        } catch(IOException e) {
            System.out.println(e);
        }
    }
    
    public void setImage(Image image)
    {
        int i;
        MediaTracker tracker;
        System.out.println("reading WMF: waiting for MediaTracker...");
        tracker = new MediaTracker (this);
        tracker.addImage(image,0);
        try 
	{ 
	   tracker.waitForID(0); 
	} 
        catch (Exception ex)
	{
	   System.out.println("error Exception:");
           ex.printStackTrace();
	   return;
	}
        switch (i=tracker.statusID(0,false))
	 {
	  case MediaTracker.COMPLETE:
	    System.out.println("WMF MediaTracker OK.");
        saveImage(image);
	    break;
	  default:
	    System.out.println("WMF MediaTracker status = "+i);
	    image=null;
	    break;
	 }
        this.image=image;
    }

    public void paint(Graphics g) 
    {
      if (image!=null)
	 g.drawImage(image, 0, 0, this);
      else
	 {
	    Font f=new Font("Courier",Font.BOLD,18);
	    g.setFont(f);
	    g.drawString("IMAGE ERROR",30,30);
	 }
    } 
}

