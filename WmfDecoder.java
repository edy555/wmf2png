/*
    Copyright (C) 
    1997-2005 Albrecht Kleine <kleine@ak.sax.de>
    2005 Mike Flis <mflis@xteric.com>
 
    version 0.8

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

// ----------- the WMF file interpreter ----------------------------------
//
class WmfDecoder implements ImageProducer
{
     boolean debug=false;
//     boolean debug=true;
     boolean drawCross_if_error=true;
     private int minsize=8;
     private int top,left,siz,obj,max,res,inch;
     private WmfDecObj gdiObj[];
     private Stack DCstack;
     private int rgbPixels[]=null;
     private short params[];
     private Frame fr;
     private int width = -1, height = -1;
     private InputStream in;
     private ColorModel cmodel=ColorModel.getRGBdefault();
     private boolean err = false;
     private boolean producing = false;
     private Vector consumers = new Vector();

	 static private float[] dashpen = new float[] {18.0f},
							dotpen = new float[] {3.0f},
							dashdotpen = new float[] {9.0f,6.0f,3.0f,6.0f},
							dashdotdotpen = new float[] {9.0f,3.0f,3.0f,3.0f,3.0f,3.0f};

     // constructor
     public WmfDecoder(InputStream is)
     {
	in=is;
     }

    // -------- methods that implement ImageProducer -----------------------
    public void addConsumer(ImageConsumer ic)
	{
	   if (debug)
	     System.out.println("addConsumer:"+ic);
	   if (ic != null && ! isConsumer(ic))
	     consumers.addElement(ic);
	}

    public void startProduction(ImageConsumer ic)
	{
	   if (debug)
	     System.out.println("startProduction:"+ic);
	   addConsumer(ic);
	   if (rgbPixels==null)
	     {
	    try
	       {  
	         readWmf();
	       }
	       catch (Exception ex)
		 {
	          err = true;
	          width = height = -1;
       	          System.out.println(ex);
		 }
	     }

	if (!producing)
	    {
	       producing = true;
	       sendImage();
	    }
	}

    public boolean isConsumer(ImageConsumer ic)
	{
	   return consumers.contains(ic);
	}

    public void removeConsumer(ImageConsumer ic)
	{
	  if (debug)
            System.out.println("Remove:"+ic);
	  consumers.removeElement(ic);
	}

    public void requestTopDownLeftRightResend(ImageConsumer ic)
	{
	}

    // ---- method connects the Wmf interpreter with ImageProducer methods
    private void sendImage()
	{
	   Vector xconsumers =(Vector)consumers.clone();
	   // consumers will decrease while calling ic.imageComplete()
	   // so in xconsumers we can each all elements
 	   for(Enumeration e = xconsumers.elements() ; e.hasMoreElements(); )
	   {
	    if (debug)
	      System.out.println("consumers.size:"+consumers.size());
	    ImageConsumer ic = (ImageConsumer)e.nextElement();
	    if (isConsumer(ic))
	    {
	      if (debug)	       
      	        System.out.println("setPixels:"+ic);
	      if (!err)
	      {	 
	       ic.setDimensions(width, height);
	       ic.setColorModel(cmodel);
	       ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES |
			   ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
	       for ( int row = 0; row < height; row++ )
                 ic.setPixels(0, row, width, 1, cmodel, rgbPixels, row*width, width);
	       ic.imageComplete( ImageConsumer.STATICIMAGEDONE);
	      }
	      else
		 {
		    if (debug)
		      System.out.println("IMAGEERROR");
		    // ic.imageComplete(ImageConsumer.IMAGEERROR);
		    ic.imageComplete(ImageConsumer.IMAGEABORTED);
		 }
	    }
	   }
	producing = false;
	}

     private static void PrintObserverStatus(String text,int status)
     {
   	System.out.print(text);
	if ((status & 0x80)>0 )  System.out.print("ABORT ");
	if ((status & 0x40)>0 )  System.out.print("ERROR ");
	if ((status & 0x20)>0 )  System.out.print("ALLBITS ");
	if ((status & 0x10)>0 )  System.out.print("FRAMEBITS ");
	if ((status & 0x08)>0 )  System.out.print("SOMEBITS ");
	if ((status & 0x04)>0 )  System.out.print("PROPERTIES ");
	if ((status & 0x02)>0 )  System.out.print("HEIGHT ");
	if ((status & 0x01)>0 )  System.out.print("WIDTH ");
	System.out.println("");
    }


     //---- main method for reading Wmf into a pixel array -------------------
     private void readWmf() throws IOException,InterruptedException
     {
         Dimension d=new Dimension(320,240); // std window size, only if no first header present
         Image offscreen;
         Graphics g;

         if (chkHeader(in,d))
           throw new IOException( "WMF file format not supported" );
	
	 DCstack=new Stack();
	 gdiObj = new WmfDecObj[obj];
	 width=d.width;
         height=d.height;
	 if (debug)        
           System.out.println(d);
	 fr= new Frame();
         fr.addNotify();
	 offscreen = fr.createImage(d.width,d.height);
	 g = offscreen.getGraphics();
 	 params=new short[max];  		// max space for a metafile record
	
	 WmfDecDC DC=new WmfDecDC(width,height,left,top);
	 DC.gr=g;
	 DCstack.push(DC);
	 while (readRecord(in))
	     ;
	 rgbPixels = new int[d.width * d.height];
	 PixelGrabber pg=new PixelGrabber(offscreen.getSource(),0,0,
	    d.width,d.height,rgbPixels,0,d.width);
         pg.grabPixels();
	 if (debug)
            PrintObserverStatus("PixelGrabber status: ",pg.status());
      System.out.println("PixelGrabber status:"+pg.status());
      System.out.println("fr="+fr);
	 fr.dispose();
	}


    private boolean chkHeader(InputStream in,Dimension d) throws IOException
    {
       int i,j,wid=0,hig=0,sum=0;
       int hdr[] = {-12841,-25914,0,  0,0,0,0,0,0,0,0,  1,9,0x300 };

       for (i=0;i<14;i++)
	{
	  j=readInt16(in);
	  sum^=j;
	  if ( (i<3 || i>7))
	  {   
	    if (j != hdr[i])
	       if ((i+=11)==11 && j==hdr[11])	// no first header present
		  continue;
	       else
		 return true;			// error
	  }
          else
	   switch (i)
	   {
	   case 3:left=j;break;
	   case 4:top=j;break;
	   case 5:wid=j;break;
	   case 6:hig=j;break;
	   case 7:hdr[10]=sum;			// store checksum
                  res = Toolkit.getDefaultToolkit().getScreenResolution();
	          inch = j;
	          if (debug)
	 	  {
		    System.out.println("inch:  "+inch);
		    System.out.println("sres:  "+res);
		  }
                  d.width  = ((wid-left)*res)/inch;
                  d.height = ((hig-top)*res)/inch;
	          break;
	   }
	}
       if (debug)
         System.out.println("dimension: "+d);
       siz=readInt32(in);
       obj=readInt16(in);
       max=readInt32(in);
       readInt16(in);				// unused
       if (debug)
	 {
          System.out.println("filesize(16): "+siz);
          System.out.println("GDI-Objects : "+obj);
          System.out.println("max rec size: "+max);
	 }
       return false;
       }

      private boolean readRecord(InputStream in)
	 {
	    int i,j,rdSize,rdFunc;
	    int a,b,c,d,e,f,k,l,m,n;
	    Color crco;
	    Font fo;
	    Image im;
	    WmfDecDC DC=(WmfDecDC)DCstack.peek();
	    Graphics g=DC.gr;
	    
	    boolean error;
	    int xpoints[],ypoints[];
	    byte text[];
	    String s;
	    Object ob;
	    Graphics g2;

	    try
	      {
		 rdSize = readInt32(in);
		 rdFunc = readInt16(in);
		 for (i=0;i<rdSize-3;i++)
		   params[i]=readInt16(in);
	      } 
	    catch (IOException ex)
	      {
		 return false;
	      }
	     if (debug)   
	        System.out.println("RFunc: "+Integer.toString(rdFunc,16));
		 switch (rdFunc)
		 {
			 case META_LINETO:
			 {
		      g.setColor(DC.aktpen.getColor());

		      a=DC.ytransfer(params[0]);
		      b=DC.xtransfer(params[1]);

		      if (debug) 
				  System.out.println("MetaLineTo color="+DC.aktpen.getColor()+" x="+b+" y="+a+" curx="+DC.aktXpos+" cury="+DC.aktYpos);

			  Stroke str = ((Graphics2D)g).getStroke();
			  ((Graphics2D)g).setStroke(makeStroke(DC));
			  g.drawLine(DC.aktXpos,DC.aktYpos,b,a);
			  ((Graphics2D)g).setStroke(str);

		      DC.aktXpos=b;
		      DC.aktYpos=a;
		      break;
			 }

		    case META_MOVETO:
		      DC.aktYpos=DC.ytransfer(params[0]);
		      DC.aktXpos=DC.xtransfer(params[1]);
		      if (debug)
				System.out.println("MetaMoveTo x="+DC.aktXpos+"("+params[1]+") y="+DC.aktYpos+"("+params[0]+")");
		      break;
		      
		 case META_ROUNDRECT:
		 {
		      if (debug)
					System.out.println("MetaRoundRect");
		      e=transform(params[0],minsize);
		      f=transform(params[1],minsize);
		      a=DC.ytransfer(params[2]);
		      b=DC.xtransfer(params[3]);
		      c=DC.ytransfer(params[4]);
		      d=DC.xtransfer(params[5]);
		      if (a<c && b<d)
			  {
				i=a;a=c;c=i;
				i=b;b=d;d=i;
			  }
		      
              // FIXME: draw_round_rect_Pattern if needed
			  if (DC.aktbrush.getColor() != null)
			  {
				  g.setColor(DC.aktbrush.getColor());
				  g.fillRoundRect(d,c,b-d-1,a-c-1,f,e);
			  }
		      g.setColor(DC.aktpen.getColor());

			  Stroke str = ((Graphics2D)g).getStroke();
			  ((Graphics2D)g).setStroke(makeStroke(DC));
      	      g.drawRoundRect(d,c,b-d-1,a-c-1,f,e);
			  ((Graphics2D)g).setStroke(str);

		      break;
		 }

		 case META_RECTANGLE:
		 {
		      if (debug)
				System.out.println("MetaRectangle");
		      a=DC.ytransfer(params[0]);
		      b=DC.xtransfer(params[1]);
		      c=DC.ytransfer(params[2]);
		      d=DC.xtransfer(params[3]);
		      if (a<c && b<d)
			  {
				i=a;a=c;c=i;
				i=b;b=d;d=i;
			  }
		      if (DC.aktbrush.getImage(DC.aktbkmode==1)!=null)
				  drawOpaqePattern(g,DC.aktbrush.getImage(DC.aktbkmode==1),d,c,b,a,fr);
		      else if (DC.aktbrush.getColor() != null)
			  {
			     g.setColor(DC.aktbrush.getColor());
		      	 g.fillRect(d,c,b-d-1,a-c-1);
			  }
		      g.setColor(DC.aktpen.getColor());

			  Stroke str = ((Graphics2D)g).getStroke();
			  ((Graphics2D)g).setStroke(makeStroke(DC));
		      g.drawRect(d,c,b-d-1,a-c-1);
			  ((Graphics2D)g).setStroke(str);

		      break;
		 }
		      
		    case META_SETPIXEL:
		      if (debug)
			System.out.println("MetaSetpixel");
		      crco = new Color(getLoByteVal(params[0]),
				       getHiByteVal(params[0]),
				       getLoByteVal(params[1]));
		      g.setColor(crco);
		      crco=null;
		      a=DC.xtransfer(params[3]);
		      b=DC.ytransfer(params[2]);
		      g.drawLine(a,b,a,b);
		      break;
		      
		 case META_POLYLINE:
		 case META_POLYGON:
		 {
		      if (debug)
			System.out.println(((rdFunc==META_POLYGON)? "MetaPolygon: ":
					    "MetaPolyLine: ")+params[0]);
           	      xpoints=new int[params[0]];
		      ypoints=new int[params[0]];
		      for (i=0;i<params[0];i++)
			{
			   xpoints[i]=DC.xtransfer(params[i*2+1]);
			   ypoints[i]=DC.ytransfer(params[i*2+2]);
			   if (debug) 
			     System.out.println(Integer.toString(xpoints[i],16)+" "+Integer.toString(ypoints[i],16));
			}
		    if (rdFunc==META_POLYGON)
			{
			   if (DC.aktbrush.getColor() != null) 
			   {
				   g.setColor(DC.aktbrush.getColor());
				   g.fillPolygon((int[])xpoints,(int[])ypoints,params[0]);
			   }
			   g.setColor(DC.aktpen.getColor());
			   Stroke str = ((Graphics2D)g).getStroke();
			   ((Graphics2D)g).setStroke(makeStroke(DC));
			   g.drawPolygon((int[])xpoints,(int[])ypoints,params[0]);
			   ((Graphics2D)g).setStroke(str);
			}
		      else
		      {
			   g.setColor(DC.aktpen.getColor());
			   Stroke str = ((Graphics2D)g).getStroke();
			   ((Graphics2D)g).setStroke(makeStroke(DC));
			   g.drawPolyline((int[])xpoints,(int[])ypoints,params[0]);
			   ((Graphics2D)g).setStroke(str);
		      }
		      xpoints=null;
		      ypoints=null;
		      break;
		 }

		 case META_POLYPOLYGON:
		 {
		      if (debug)
			  {
				System.out.println("MetaPolyPolygon: "+params[0]);
				showparams(params,rdSize,rdFunc);
			  }

		      for (i=0;i<params[0];i++)
			  {
			   xpoints=new int[params[i+1]];
			   ypoints=new int[params[i+1]];
			   if (debug)
			     System.out.println("Polygon #"+i+ " Pts="+params[i+1]);
			   
			   b=params[0]+1;  		// first point of first polygon
			   for (c=0;c<i;c++)
				b+=params[c+1]*2; 	// add size of polygons before

			   for (a=0;a<params[i+1];a++)
			   {
				 xpoints[a]=DC.xtransfer(params[b + a*2]);
				 ypoints[a]=DC.ytransfer(params[b + a*2+1]);

				 if (debug)
				    System.out.println(Integer.toString(xpoints[a],16)+" "+
						     Integer.toString(ypoints[a],16));
			   }
			   if (DC.aktbrush.getColor() != null) 
			   {
//				   g.setXORMode(DC.aktbackgnd);			// ak Sun Oct 30 09:48:00 CET 2005
				   g.setColor(DC.aktbrush.getColor());
				   g.fillPolygon((int[])xpoints,(int[])ypoints,params[i+1]);
//				   g.setPaintMode();				// ak Sun Oct 30 09:48:00 CET 2005
			   }
			   g.setColor(DC.aktpen.getColor());
			   Stroke str = ((Graphics2D)g).getStroke();
			   ((Graphics2D)g).setStroke(makeStroke(DC));
			   g.drawPolygon((int[])xpoints,(int[])ypoints,params[i+1]);
			   ((Graphics2D)g).setStroke(str);
			}
		    break;
		 }

		 case META_ELLIPSE:
		 {
		      if (debug)
				System.out.println("MetaEllipse");
		      a=DC.ytransfer(params[0]);b=DC.xtransfer(params[1]);
		      c=DC.ytransfer(params[2]);d=DC.xtransfer(params[3]);
			  if (DC.aktbrush.getColor() != null)
			  {
				  g.setColor(DC.aktbrush.getColor());
				  g.fillOval(d,c,b-d,a-c);
			  }
		      g.setColor(DC.aktpen.getColor());

			  Stroke str = ((Graphics2D)g).getStroke();
			  ((Graphics2D)g).setStroke(makeStroke(DC));
		      g.drawOval(d,c,b-d,a-c);
			  ((Graphics2D)g).setStroke(str);

		      break;
		 }

		 case META_ARC:  
		 case META_PIE:
		 case META_CHORD:
		 {
		      if (debug)
		       switch (rdFunc)
			{
			 case META_ARC:System.out.println("MetaArc");break;
			 case META_PIE:System.out.println("MetaPie");break;
			 case META_CHORD:System.out.println("MetaChord");break;
			}

		      a=DC.ytransfer(params[0]);b=DC.xtransfer(params[1]); // Yend;Xend;
		      c=DC.ytransfer(params[2]);d=DC.xtransfer(params[3]); // Ystart;Xstart;
		      e=DC.ytransfer(params[4]);f=DC.xtransfer(params[5]); // Ybuttom;Xright;
		      k=DC.ytransfer(params[6]);l=DC.xtransfer(params[7]); // Ytop;Xleft

		      g.setColor(DC.aktpen.getColor());
		      int xm=l+(f-l)/2;
		      int ym=k+(e-k)/2;
		      if (rdFunc==META_PIE)
			{
				  Stroke str = ((Graphics2D)g).getStroke();
				  ((Graphics2D)g).setStroke(makeStroke(DC));
				  g.drawLine(d,c,xm,ym);
				  g.drawLine(b,a,xm,ym);
				  ((Graphics2D)g).setStroke(str);
			}
		      if (rdFunc==META_CHORD)
			  {
				  Stroke str = ((Graphics2D)g).getStroke();
				  ((Graphics2D)g).setStroke(makeStroke(DC));
				  g.drawLine(d,c,b,a);
				  ((Graphics2D)g).setStroke(str);

			  }
			
		      int beg=arcus(d-xm,c-ym);
		      int arc=arcus(b-xm,a-ym) - beg;
		      if (arc<0)
			arc+=360;
		      if (debug)
		       System.out.println("Beg="+beg+" Arc="+arc);

			  Stroke str = ((Graphics2D)g).getStroke();
			  ((Graphics2D)g).setStroke(makeStroke(DC));
		      g.drawArc(l,k,f-l,e-k,beg,arc);
			  ((Graphics2D)g).setStroke(str);
		      //
		      // FIXME: fill arc etc with selected brush  Sat May 17 19:02:27 1997
		      break;
		 }
		    case META_DELETEOBJECT:
		      if (debug) 
			System.out.println("MetaDeleteObject:"+params[0]);
		      gdiObj[params[0]]=null;
		      break;
		      
		    case META_SELECTPALETTE:
		      if (debug)
			System.out.println("MetaSelectPalette:"+params[0]+" = "+gdiObj[params[0]]);
		      if (gdiObj[params[0]].getMagic()==WmfDecObj.M_PALETTE)
			DC.aktpal=gdiObj[params[0]];
		      else
			System.out.println(" ---- internal ERROR in MetaSelectPalette -----");
		      break;
		      
		    case META_SELECTCLIPREGION:
		      if (debug)
			System.out.println("MetaSelectClipRegion:"+params[0]+" = "+gdiObj[params[0]]);
		      if (gdiObj[params[0]].getMagic()==WmfDecObj.M_CLIP)
			{
			   DC.aktclip=gdiObj[params[0]];
                           g.clipRect(DC.aktclip.getRect().x,DC.aktclip.getRect().y,
				      DC.aktclip.getRect().width,DC.aktclip.getRect().height);
			}
		      else
			System.out.println(" ---- internal ERROR in MetaSelectClipregion -----");
		      break;

		    case META_SELECTOBJECT:
		      if (debug) 
			System.out.println("MetaSelectObject:"+params[0]+" = "+gdiObj[params[0]]);
		      switch (gdiObj[params[0]].getMagic())
			{
			 case WmfDecObj.M_PEN:
			   DC.aktpen=gdiObj[params[0]];
			   break;
			 case WmfDecObj.M_FONT:
			   DC.aktfont=gdiObj[params[0]];
			   break;
			 case WmfDecObj.M_BRUSH:
			   DC.aktbrush=gdiObj[params[0]];
			   break;
			 case WmfDecObj.M_PALETTE:		// a kind of dummy
			   DC.aktpal=gdiObj[params[0]];
			   break;
			 case WmfDecObj.M_BITMAP:		// another one...
			   DC.aktbmp=gdiObj[params[0]];		
			   break;
			 case WmfDecObj.M_CLIP:
			   DC.aktclip=gdiObj[params[0]];
			   if (debug)
			     {
				System.out.println("Select clipping rect");
				g.drawRect(DC.aktclip.getRect().x,DC.aktclip.getRect().y,
		                        DC.aktclip.getRect().width,DC.aktclip.getRect().height);
			     }
                           g.clipRect(DC.aktclip.getRect().x,DC.aktclip.getRect().y,
				      DC.aktclip.getRect().width,DC.aktclip.getRect().height);
			   break;
			}
		      break;

		    case META_CREATEPENINDIRECT:
		      if (debug) 
			  {
					System.out.println("MetaCreatePenIndirect "+params[0]);
					showparams(params,rdSize,rdFunc);
			  }

		      error=false;
		      switch (params[0])
			  {
				 case PS_NULL:crco=null; //ex DC.aktbackgnd;
				   // FIXME: have to test all  DC.akt{pen|brush}.getColor()
				   // if  color equals null, do NOT paint,draw,fill etc!! Sat May 17 19:23:45 1997
				   System.out.println("MetaCreatePenIndirect: PS_NULL");  
				   break;
				 case PS_DASH:
				 case PS_DOT:
				 case PS_DASHDOT:
				 case PS_DASHDOTDOT:
				 case PS_INSIDEFRAME:
				 case PS_SOLID:crco = new Color(getLoByteVal(params[3]), getHiByteVal(params[3]), getLoByteVal(params[4]));
				   break;
				 default:
				   crco=Color.black;error=true;break;
			  }

		      if (!error)
			  {  
			   add_handle(new WmfDecObj(/*PS_SOLID*/ params[0], DC.xtransfer(params[1]), crco));
			   if (debug)
			     System.out.println(crco);
			   crco=null;
			   a=params[1];
			   b=params[2];
			}
		    if (debug || error)
			{
			  for (i=0;i<rdSize-3;i++)
			   if (i<16)
			     System.out.print(Integer.toString(params[i],16)+" ");
		         System.out.println();
			}
		    break;

		    case META_CREATEBRUSHINDIRECT:
		      if (debug)  
			  {
			     System.out.println("MetaCreateBrushIndirect: Style="+params[0]);
			     showparams(params,rdSize,rdFunc);
			  }

		      switch (params[0])
			  {
			     case 1:crco=DC.aktbackgnd;				// BS_HOLLOW
					add_handle(new WmfDecObj((Color)null, /*crco*/ WmfDecObj.M_BRUSH));
					if (debug) System.out.println(crco);			   
					break;
				 case 0:crco = new Color(getLoByteVal(params[1]),	// BS_SOLID
							 getHiByteVal(params[1]), getLoByteVal(params[2]));
				        add_handle(new WmfDecObj(crco,WmfDecObj.M_BRUSH));
				        if (debug) System.out.println(crco);
						crco=null;
					break;
				 case 2:crco = new Color(getLoByteVal(params[1]),	//BS_HATCHED
					   getHiByteVal(params[1]), getLoByteVal(params[2]));
					   add_handle(new WmfDecObj((int)params[3],crco,DC.aktbackgnd,fr));
					   if (debug) 
						   System.out.println("crco="+crco+" DC.aktbackgnd="+DC.aktbackgnd);
					   crco=null;
					   break;

			 case 3:						//BS_PATTERN
			 case 4:						//BS_INDEXED
			 case 5:						//BS_DIBPATTERN
			   // FIXME: replace workaround
			   crco=Color.gray;
			   add_handle(new WmfDecObj(crco,WmfDecObj.M_BRUSH));
			   System.out.println("pattern substitution used.");			   
			   break;
			   
			 default:System.out.println("(bad parameter!)");
			}
		      break;

		    case META_CREATEREGION:
		      if (debug)
			{
			   System.out.println("MetaCreateRegion");
			   System.out.println("params[5] sub records="+params[5]);
			   for (i=0;i<rdSize-3;i++)
			      System.out.print(Integer.toString(params[i], 10  )+" ");
		           System.out.println();
			}
		      add_handle(new WmfDecObj(DC.xtransfer(params[7]),DC.ytransfer(params[8]),
					       DC.xtransfer(params[9]),DC.xtransfer(params[10])));
		      // awt supports only rectangle clipping, currently other data ignored
		      // FIXME: fake oval or other regions
		      break;

		    case META_INTERSECTCLIPRECT:
		      //if (debug)
		        System.out.println("MetaIntersectClipRect is experimental");
		      n=DC.ytransfer(params[0]);
		      m=DC.xtransfer(params[1]);
		      l=DC.ytransfer(params[2]);
		      k=DC.xtransfer(params[3]);
		      g.clipRect(k,l,m-k,n-l);
		      break;

		    case META_CREATEFONTINDIRECT:
		      text=new byte[80];
		      for (j=i=0;i<rdSize-3-9;i++) // 9 starts FontName, 3 for overhead
			{
			  if ((text[2*i  ]=(byte)getLoByteVal(params[i+9]))==0)
			     break;
			   else
			     j++;
			  if ((text[2*i+1]=(byte)getHiByteVal(params[i+9]))==0)
			     break;
			   else
			     j++;
			}
		      s=new String(text,0,0,j);
		      if (debug)
			  {
		        System.out.println("MetaCreateFontIndirect: "+params[0]+" "+params[1]+" "+s);
				showparams(params,rdSize,rdFunc);
			  }
		      if (s.startsWith("Times"))	// may be: "Times New Roman"
		        s="TimesRoman";
		      else
		      if (s.startsWith("Arial"))
		        s="Helvetica";
		      else
		      if (s.startsWith("Courier"))
		        s="Courier";
		      else
		      if (s.startsWith("MS"))		// may be: "MS Sans Serif"
		        s="Dialog";
		      else
		      if (s.startsWith("WingDings")) 
		        s="ZapfDingbats";
		      b=params[1];		// width
		      c=params[2];d=params[3];	// esc, ori
		      e=params[4];f=params[5];	// weight, ita+underl
		      k=params[6];l=params[7];	// str+cha, out+clip
		      i=params[8];	     	// pitch
		      a=transform(params[0],minsize);
		      fo=new Font(s,(e>500 ? Font.BOLD: Font.PLAIN)+
				      (getLoByteVal(f)>0 ? Font.ITALIC : 0),a);
		      if (debug)
		        System.out.println(fo);

		      add_handle(new WmfDecObj(fo,getHiByteVal(f),d,c));
		      fo=null;
		      text=null;
		      break;

		    case META_CREATEPALETTE:
		      if (debug)		      
		       System.out.println("MetaCreatePalette");
		      crco=Color.black;
		      add_handle(new WmfDecObj(crco,WmfDecObj.M_PALETTE));
		      break;
		      
		    case META_REALIZEPALETTE:
		      if (debug)
			showparams(params,rdSize,rdFunc);
		      System.out.println("MetaRealizePalette");
		      break;
		      
	            case META_SETROP2:
		      if (debug)
		        System.out.println("MetaSetRop2: ROP code="+
			    Integer.toString( (i=params[0]),16));
		      break;

		    case META_SETPOLYFILLMODE:	  
		      if (debug)
		        System.out.println("MetaSetPolyFillmode:"+params[0]);
		      break;
		      
		    case META_SETSTRETCHBLTMODE:
		      if (debug)
		        System.out.println("MetaSetStretchBltMode:"+params[0]);
		      break;
		      
		    case META_INVERTREGION:
		      if (debug)
			showparams(params,rdSize,rdFunc);
		      System.out.println("MetaInvertRegion:"+params[0]);
		      break;
		      
		    case META_SETWINDOWEXT:
		      DC.winextY=params[0];
		      DC.winextX=params[1];
		      if (debug)		      
		        System.out.println("MetaSetWindowExt:  X:"+DC.winextX+"  Y:"+DC.winextY);
		      break;

		    case META_SETWINDOWORG:
		      DC.winorgY=params[0];
		      DC.winorgX=params[1];
		      if (debug)
		        System.out.println("MetaSetWindowOrg:  X:"+DC.winorgX+"  Y:"+DC.winorgY);
		      break;
		      
 		    case META_SETTEXTCOLOR:
		      DC.akttextc = new Color(getLoByteVal(params[0]),
					      getHiByteVal(params[0]),
                                              getLoByteVal(params[1]));
      		      if (debug)
		        System.out.println("MetaSetTextColor: "+DC.akttextc);
		      break;

		    case META_EXTTEXTOUT:
		    case META_TEXTOUT:
		      if (rdFunc==META_EXTTEXTOUT)
			{
			   a=params[2];			// text length
			   b=DC.ytransfer(params[0]);
			   c=DC.xtransfer(params[1]);
			   d=params[3];			// option
			   if (debug)
			   {
					System.out.println("ExtTextOut:option ="+Integer.toString(d,16));
					showparams(params,rdSize,rdFunc);
			   }

			   k=DC.xtransfer(params[4]);
			   l=DC.ytransfer(params[5]);
			   m=DC.xtransfer(params[6]);
			   n=DC.ytransfer(params[7]);
			   if (debug)
			     {
				System.out.println("TextAlign="+DC.akttextalign);
				System.out.println("x  ="+c+"\ty  ="+b);
				System.out.println("rx ="+k+"\try ="+l);
				System.out.println("rw ="+(m-k)+"\trh ="+(n-l));
			     }
			   e= d==0 ? 3 : 7;		// start of text
			}
		      else
			{
			   a=params[0];			//text length
		           b=DC.ytransfer(params[(a+1)/2+1]);
          		   c=DC.xtransfer(params[(a+1)/2+2]);
			   d=e=0;
			   k=l=m=n=0;
			}
	        // ------- handle ETO_... flags
		    if ((d&ETO_OPAQUE) !=0)
			{
			   g.setColor(DC.aktbackgnd);  // for testing purpose: .... ,Color.green);
		       g.fillRect(k,l,m-k-1,n-l-1);
		       if (debug)
				  System.out.println("ExtTextOut: using OPAQUE style "+DC.aktbackgnd+" "+k+","+l+","+(m-k-1)+","+(n-l-1));
			}

		      //
		      if ((d&ETO_GRAYED) !=0)
			g.setColor(Color.lightGray);
		      else
			g.setColor(DC.akttextc);
		      //
		      if ((d&ETO_CLIPPED) !=0)
			{
			 g2=g.create();
			 g2.clipRect(k,l,m-k-1,n-l-1);
			 // FIXME: intersect with original clip rect  
			 g=g2;
			 if (debug)
			     System.out.println("ExtTextOut: using clipping rect");
			}
		      else
			g2=null;
		      // ------------------
		      g.setFont(DC.aktfont.getFont());
		      FontMetrics fm= g.getFontMetrics();
		      text=new byte[a];
		      for (i=0;i<a;i++)
			   if (i%2 ==0)
			  	text[i]=(byte)getLoByteVal(params[e+i/2+1]);
			   else
			  	text[i]=(byte)getHiByteVal(params[e+i/2+1]);
		      s=new String(text,0);

		      // ---- draw text ---
//	     	  if (DC.aktfont.getFontOrientation()!=0)
//	              System.out.println("non horizontal text is not supported: "+s);
//	          else
	          {
				  int esc = DC.aktfont.getFontEscapement();
                  java.awt.geom.AffineTransform saveAT = null;

				  if (esc != 0) 
				  {
					  saveAT = ((Graphics2D)g).getTransform();
					  ((Graphics2D)g).rotate(-esc / 10.0 / 180.0 * 3.14159,c,b);
				  }

				  if (DC.akttextalign == TA_TOP)
						 b+=DC.aktfont.getFont().getSize();
					 else if ((DC.akttextalign & TA_CENTER) > 0)
						 c -= (fm.stringWidth(s) / 2);

				  if (DC.aktbkmode!=1)
				  {
					 Color hld = g.getColor();
					 g.setColor(DC.aktbackgnd);  // for testing purpose: .... ,Color.green);
					 int sz = DC.aktfont.getFont().getSize();
					 g.fillRect(c,b-sz,fm.stringWidth(s),sz);
					 g.setColor(hld);
				  }

				  g.drawString(s,c,b);
				  if (DC.aktfont.isUnderlined())
				  {
					  int h = fm.getDescent() / 3;
					  if (h < 1)
						  h = 1;
					g.fillRect(c,b+fm.getDescent()-h-1,fm.stringWidth(s),h);
//					g.drawLine(c,b+2,c+fm.stringWidth(s),b+2);
				  }

				  if (saveAT != null)
					  ((Graphics2D)g).setTransform(saveAT);
              }
		      // ------------------
		      if (debug)
				System.out.println((rdFunc==META_EXTTEXTOUT ?
					   "MetaExtTextOut: ":"MetaTextOut: ")
					   +(new String(text,0))+" (len="+a+") x="+c+" y="+b);
		      text=null;
		      if (g2!=null)
			g2.dispose();
		      break;

		    case META_SETMAPMODE:
		      if (debug)
			showparams(params,rdSize,rdFunc);
		      System.out.println("MetaSetMapMode: "+params[0]+" (ignored)");
		      break;

		    case META_SETBKCOLOR:
		      DC.aktbackgnd=new Color(getLoByteVal(params[0]),
					      getHiByteVal(params[0]),
					      getLoByteVal(params[1]));
		      if (debug)
		        System.out.println("MetaSetBkColor "+DC.aktbackgnd);
		      break;
		      
		    case META_SETTEXTJUSTIFICATION:
		      if (debug)
			showparams(params,rdSize,rdFunc);
		      if (debug || params[0]!=0 || params[1]!=0)
		         System.out.println("MetaSetTextJustification: "+params[0]+" "+params[1]);
		      break;

		    case META_SETBKMODE:
		      if (debug)
		        System.out.println("MetaSetBkMode:" + (params[0]==1? "TRANSPARENT":"OPAQUE"));
		      DC.aktbkmode=params[0];
		      break;

		    case META_SETTEXTALIGN:
		      if (debug)
		        System.out.println("MetaSetTextalign: "+params[0]);
		      DC.akttextalign=params[0];
		      break;

	            case META_SAVEDC:
		      if (debug)
			System.out.println("MetaSaveDC");
		      try
			{ 
			  DC=(WmfDecDC)DCstack.push(DC.clone());
			  DC.slevel++;
			  DC.gr=g.create();
			}
		      catch (Exception ex)
			{ 
			   System.out.println(" ---- internal ERROR in MetaSaveDC -----");
			}
		      break;

		    case META_RESTOREDC:
		      if (debug)
			System.out.println("MetaRestoreDC"+params[0]);
		      switch (params[0])
			{
			 case -1:g.dispose();
			         DCstack.pop();
			         DC=(WmfDecDC)DCstack.peek();
			   	 break;
			 default:while (DC.slevel>params[0] && !DCstack.empty())
			         {
				   g.dispose();
			     	   DC=(WmfDecDC)DCstack.pop();
				   g=DC.gr; 
			         }
			         break;
			}
		      break;

		    case META_PATBLT:
		      e=(params[1]<<16)+params[0];
		      if (debug)
			{	
			   System.out.println("MetaPatBlt: ROP code="+Integer.toString(e,16));
			   System.out.println(DC.aktbrush.getImage(DC.aktbkmode==1));
			}
		      a=DC.ytransfer(params[2]);
		      b=DC.xtransfer(params[3]);
		      c=DC.ytransfer(params[4]);
		      d=DC.xtransfer(params[5]);
		      switch (e)
			{
			 case WHITENESS:
			   g.setColor(Color.white);	// <------ not yet debugged
			   g.fillRect(d,c,b,a);
			   break;
			 case BLACKNESS:
			   g.setColor(Color.black);
			   g.fillRect(d,c,b,a);
			   break;
			 case PATCOPY:
			   if ((im=DC.aktbrush.getImage(DC.aktbkmode==1))!=null)
					drawOpaqePattern(g,im,d,c,d+b,c+a,fr);
			   else if (DC.aktbrush.getColor() != null)
			   {
					g.setColor(DC.aktbrush.getColor());
					g.fillRect(d,c,b,a);
			   }		
			   break;
			 case PATINVERT:
			 case DSTINVERT:// FIXME
			 default:
			   System.out.println("unsupported ROP code:"+Integer.toString(e,16));
			}
		      
		      break;

		    case META_STRETCHBLT:
		      if (debug)
		        System.out.println("MetaStretchBlt:"+rdSize);
                      e=(params[1]<<16)+params[0];
		      a=DC.ytransfer(params[6]);
		      b=DC.xtransfer(params[7]);
		      c=DC.ytransfer(params[8]);
		      d=DC.xtransfer(params[9]);
		      switch (e)
			{
			 case WHITENESS:
			   g.setColor(Color.white);	// <------ not yet debugged
			   g.fillRect(d,c,b,a);
			   break;
			 case BLACKNESS:
			   g.setColor(Color.black);
			   g.fillRect(d,c,b,a);
			   break;
                         case SRCCOPY:
		           im=OldBitmapImage(10,params,fr);
		           if (im!=null)
			     {
			        g.drawImage(im,d,c,b,a,fr);
			        im=null;
			     }
			   else
			     if (drawCross_if_error)
			     {
				g.setColor(Color.black);
			        g.drawLine(0,0,DC.xtransfer(params[7]),DC.ytransfer(params[6]));
			        g.drawLine(DC.xtransfer(params[7]),0,0,DC.ytransfer(params[6]));
			     }
		           break;
                         default:
                          System.out.println("unsupported ROP code:"+Integer.toString(e,16));
                        }
                      break;

		    case META_DIBCREATEPATTERNBRUSH:
		      if (debug)
			System.out.println("MetaDibCreatePatternBrush:"+params[0]);
		      im = DIBBitmapImage(2,params,fr);
		      if (im!=null)
		        add_handle(new WmfDecObj(im));
		      else
			System.out.println("Error in MetaDibCreatePatternBrush");
		      break;
		      
		    case META_DIBBITBLT:
		    case META_STRETCHDIB:
		    case META_DIBSTRETCHBLT:
		      k=0;
		      switch (rdFunc)
			{
		      case META_DIBBITBLT:k=-2;			// 2 params less
			   if (debug) System.out.println("MetaDibBitBlt");
			   break;	
		      case META_STRETCHDIB:k=1;			// 1 param  more
			   if (debug) System.out.println("MetaStretchDib");
			   break;	
		      case META_DIBSTRETCHBLT:k=0;
			   if (debug) System.out.println("MetaDibStretchBlt");
			   break;
			}
		      a=DC.ytransfer(params[6+k]);
		      b=DC.xtransfer(params[7+k]);
		      c=DC.ytransfer(params[8+k]);
		      d=DC.xtransfer(params[9+k]);
		      e=(params[1]<<16)+params[0];
		      if (debug)
			{
			   System.out.println("dest X= "+d);
			   System.out.println("dest Y= "+c);
			   System.out.println("width = "+b);
			   System.out.println("height= "+a);
			}
		      switch (e)
			{
			 case WHITENESS:
			   g.setColor(Color.white);	// <------ not yet debugged
			   g.fillRect(d,c,b,a);
			   break;
			 case BLACKNESS:
			   g.setColor(Color.black);
			   g.fillRect(d,c,b,a);
			   break;
			 case SRCCOPY:
			   im = DIBBitmapImage(10+k,params,fr);	// here starts bmp
			   if (im!=null)
			     {
				g.drawImage(im,d,c,b,a,fr);
				im=null;
			     }
			   else
			     if (drawCross_if_error)
			     {  // draw a cross X
				g.setColor(Color.black);
				g.drawLine(d,c,d+b,c+a);
				g.drawLine(d+b,c,d,c+a);
			     }
			   break;
			 default:
			   System.out.println("unsupported ROP code:"+Integer.toString(e,16));
			}
		      break;

		    case META_ESCAPE:
		      switch (params[0])
			{
			 case MFCOMMENT:
			   if (debug)
			     {
				text=new byte[params[1]];
				for (i=0;i<params[1];i++)
				  {
				     if (i%2 ==0)
				       text[i]=(byte)getLoByteVal(params[i/2+2]);
				     else
				       text[i]=(byte)getHiByteVal(params[i/2+2]);
				     if (text[i]==0)break;
				  }
				s=new String(text,0);
				System.out.println("MetaEscape/MFCOMMENT: "+s);
			     }
			   break;
			 default:
			   if (debug)
			     System.out.println("MetaEscape #"+params[0]+" "+((params[1]+1)>>>2) +" Words");
			}
		      break;

		    case 0:return false;		// EOF
		      
		    default:
		      showparams(params,rdSize,rdFunc);
		      break;
		   }
	    return true;
	 }

	 private Stroke makeStroke(WmfDecDC DC)
	 {
		 float dash[] = null;
		 switch (DC.aktpen.getPenStyle()) 
		 {
			 case PS_DASH:
				 dash = dashpen;
				 break;

			 case PS_DOT:
				 dash = dotpen;
				 break;

			 case PS_DASHDOT:
				 dash = dashdotpen;
				 break;

			 case PS_DASHDOTDOT:
				 dash = dashdotdotpen;
				 break;
		 }
		 return new BasicStroke(dash==null ? (float)DC.aktpen.getWidth() : 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
	 }

       private void drawOpaqePattern(Graphics g,Image im,int x1,int y1,int x2,int y2,ImageObserver fr)
       {
	  // it's just a little bit tricky ;-)
	  // subsequent calls to clipRect allow only to make the clip-region smaller
	  // ... but not to set it to a different location that was not covered by the 
	  // ... current clipping region. So we have to create a new graphics, g2
	  int width=x2-x1;
	  int height=y2-y1;
	  int i,j;
	  Graphics g2=g.create(x1-x1%8,y1-y1%8,width+8,height+8);
	  g2.clipRect(x1%8,y1%8,width,height);
	  for (i=0;i<width+9;i+=8)
	    for (j=0 ;j<height+9;j+=8)
	      g2.drawImage(im,i,j,fr);
          g2.dispose();
       }

       private int getHiByteVal(int hhh)
	 {
	    byte b;
	    if (hhh>0)
		 b=(byte)(hhh/256);
	    else
	      {
		 int iii=~hhh;
		 b=(byte)(iii>>>8);
		 b=(byte)((byte)255-b);
	      }
	    return b<0 ? (int)b+256 :b;
	 }

       private int getLoByteVal(int hhh)
	 {
	    byte b;
	    if (hhh>0)
		 b=(byte)(hhh%256);
	    else
	      {
		 int iii=~hhh;
		 b=(byte)(iii&0xff);
		 b=(byte)((byte)255-b);
	      }
	    return b<0 ? (int)b+256 :b;
	 }

       private int transform( int param,int minsize)
       {
	  int i=param;
	  if (i<0)
	    i=-i;
	  try
	    {
		i=(i*res)/inch;
		if (i<minsize)
		    i=minsize;
    	    }
	  catch (ArithmeticException ex){ }
	  return i;
       }
  

      private void showparams(short[]params,int recSize,int Func)
      {
         System.out.println("MetaRecord: "+Integer.toString(Func,16)+ " RecSize="+recSize);
	 System.out.print("Data: ");
	 for (int i=0;i<recSize-3;i++)
	 if (i<16)
	   System.out.print(Integer.toString(params[i],16)+" ");
	System.out.println();
      }
    
      private int add_handle(WmfDecObj x)
	 {
	    int i;
	    for (i=0;i<obj;i++)
	      if (gdiObj[i]==null)
	      {	
		 gdiObj[i]=x;
		 if (debug)
		  System.out.println("Handle: "+i+"Obj: "+x);
               return i;
	      }
	    return -1;
	 }
       
    private int readInt32(InputStream in) throws IOException 
       {
	int ch1 = in.read();
	int ch2 = in.read();
	int ch3 = in.read();
	int ch4 = in.read();
	if ((ch1 | ch2 | ch3 | ch4) < 0)
	     throw new EOFException();
	return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
       }       

     private short readInt16(InputStream in) throws IOException 
       {
	int ch1 = in.read();
	int ch2 = in.read();
	if ((ch1 | ch2 ) < 0)
	     throw new EOFException();
	return (short)((ch2 << 8) + (ch1 << 0));
       }       


      private int arcus(int ank,int geg)
       {
	    int val=-(int) (Math.atan((float)(geg)/
				      (float)(ank)) * 180/Math.PI); // div 0.0 is "inf"
	    if (ank<0)
		val+=180;	 
	    else
	    	if (geg>=0)
	      	  val+=360;
	    return val;
       }

      // ----------- called by META_STRETCHDIB ----------- (1, 4, 8 bpp only, no RLE) --------
      private Image DIBBitmapImage(int off,short params[],Component comp)
	 {
	    int width=params[off+2];
	    int height=params[off+4];
	    int size=width*height;
	    int bpp=params[off+7];
	    int colors,i,j,k,l,m,x=0,startbitmap;
	    if (params[off+0]!=40 || params[off+1]!=0)
	      {
		 System.out.println("unsupported data format");
		 return null;
	      }
	    if (params[off+6]!=1)
	      {
		 System.out.println("not supported: planes="+params[off+17]);
		 return null;
	      }
	    if (bpp != 4 && bpp != 8 && bpp != 1)
	      {
		 System.out.println("not supported: "+ bpp +" bits per pixel");
		 return null;
	      }
	    if (params[off+8]!=0 || params[off+9]!=0)
	      {
		 System.out.println("not supported: RLE-compression");
		 return null;
	      }

	    colors = params[off+16]!=0 ? params[off+16] : (1 << bpp);
	    int palette[]=new int[colors];
	    for (i=0;i<colors;i++)			// palette data starts at params[off+20]
	      {
		 x=params[off+21+2*i];			// range = 0...255
		 palette[i]=x<<16;
		 x=getHiByteVal(params[off+20+2*i]);
		 palette[i]+=x<<8;
		 x=getLoByteVal(params[off+20+2*i]);
		 palette[i]+=x;
		 palette[i]-=0x1000000;   		// add bit_pattern (unsigned)0xFF000000 (for alpha)
	      }

	    startbitmap=20+off+2*colors;		// beginning after header+palette data
	    int pixels[] = new int[size];
	    if (debug)
	      System.out.println("bpp = "+bpp);
	    switch (bpp)
	      {
	       case 1:
		 for (k=height-1,i=0;k>=0;k--)
		   for (l=0;l<width;l+=16)
                   {
                      i++;
                      m=params[i-1+startbitmap];
                      if (m<0) m+=65536;
		      for (j=0,m =(m>>8)|(m<<8); j+l< width && j<16;j++)
		      {
		        pixels[k*width+l+j]= (m&0x8000)==0 ? -0x1000000:-0x1;
		        m<<=1;
		      }
		      if (i%2!=0)
			i++;
                   }
		 break;

	       case 4:
		 for (k=height-1,i=0;k>=0;k--)
		   for (l=0;l<width;l++)
		   {
		      switch (l%4)
			{
			 case 0:i++;
			        x=getLoByteVal(params[i-1+startbitmap]) >>>4;break;
			 case 1:x=getLoByteVal(params[i-1+startbitmap]) &0xf;break;
	                 case 2:x=getHiByteVal(params[i-1+startbitmap]) >>>4;break;
	                 case 3:x=getHiByteVal(params[i-1+startbitmap]) &0xf;break;
	                }
	                pixels[k*width+l]=palette[x];
		   }
		 break;
		 
	       case 8:
		 for (k=height-1,i=0;k>=0;k--)
		   {  
		      for (l=0;l<width;l++)
			{
			   switch (l%2)
			     {
			      case 0:i++;
		                     x=getLoByteVal(params[i-1+startbitmap]);break;
	                      case 1:x=getHiByteVal(params[i-1+startbitmap]);break;
	                     }
			     pixels[k*width+l]=palette[x];
			}
		      if (i%2!=0)
			i++;
		   }
		 break;
	      }
	    Image im = comp.createImage(new MemoryImageSource(width, height, pixels, 0, width));
	    pixels=null;
	    return im;
	 }

       
	 // ----------- called by META_STRETCHBLT ----------- (here monochrome only) --------
	private Image OldBitmapImage(int off,short params[],Component comp)
	 {
	    int width=params[off];
	    int height=params[off+1];
	    int i,j,k,l,m;
	    if ((params[off+3]!=1) || (params[off+4] != 1))
	      {
		 System.out.println("sorry, the only supported format is: planes=1,bpp=1");
		 return null;
	      }
	    int pixels[] = new int[width*height];
	    for (k=0,i=0;k<height;k++)
	      for (l=0;l<width;l+=16)
	      {
		m=params[off+5+i++];
		if (m<0) m+=65536;
		for (j=0,m =(m>>8)|(m<<8); j+l< width && j<16;j++)
		   {
		      pixels[k*width+l+j]= (m&0x8000)==0 ? -0x1000000:-0x1;
		      m<<=1;
		   }
	      }
	    Image im = comp.createImage(new MemoryImageSource(width, height, pixels, 0, width));
	    pixels=null;
	    return im;
	 }
       
private final static int META_SETBKCOLOR              = 0x0201; //
private final static int META_SETBKMODE               = 0x0102; //
private final static int META_SETMAPMODE              = 0x0103; //
private final static int META_SETROP2                 = 0x0104; //
private final static int META_SETRELABS               = 0x0105;
private final static int META_SETPOLYFILLMODE         = 0x0106; //
private final static int META_SETSTRETCHBLTMODE       = 0x0107;
private final static int META_SETTEXTCHAREXTRA        = 0x0108;
private final static int META_SETTEXTCOLOR            = 0x0209; //
private final static int META_SETTEXTJUSTIFICATION    = 0x020A; //
private final static int META_SETWINDOWORG            = 0x020B; //
private final static int META_SETWINDOWEXT            = 0x020C; //
private final static int META_SETVIEWPORTORG          = 0x020D;
private final static int META_SETVIEWPORTEXT          = 0x020E;
private final static int META_OFFSETWINDOWORG         = 0x020F;
private final static int META_SCALEWINDOWEXT          = 0x0410;
private final static int META_OFFSETVIEWPORTORG       = 0x0211;
private final static int META_SCALEVIEWPORTEXT        = 0x0412;
private final static int META_LINETO                  = 0x0213; //
private final static int META_MOVETO                  = 0x0214; //
private final static int META_EXCLUDECLIPRECT         = 0x0415;
private final static int META_INTERSECTCLIPRECT       = 0x0416;
private final static int META_ARC                     = 0x0817; //
private final static int META_ELLIPSE                 = 0x0418; //
private final static int META_FLOODFILL               = 0x0419;
private final static int META_PIE                     = 0x081A; //
private final static int META_RECTANGLE               = 0x041B; //
private final static int META_ROUNDRECT               = 0x061C; //
private final static int META_PATBLT                  = 0x061D; //
private final static int META_SAVEDC                  = 0x001E;
private final static int META_SETPIXEL                = 0x041F; //
private final static int META_OFFSETCLIPRGN           = 0x0220;
private final static int META_TEXTOUT                 = 0x0521; //
private final static int META_BITBLT                  = 0x0922;
private final static int META_STRETCHBLT              = 0x0B23; //
private final static int META_POLYGON                 = 0x0324; //
private final static int META_POLYLINE                = 0x0325; //
private final static int META_ESCAPE                  = 0x0626; //
private final static int META_RESTOREDC               = 0x0127; //
private final static int META_FILLREGION              = 0x0228;
private final static int META_FRAMEREGION             = 0x0429;
private final static int META_INVERTREGION            = 0x012A;
private final static int META_PAINTREGION             = 0x012B;
private final static int META_SELECTCLIPREGION        = 0x012C; //
private final static int META_SELECTOBJECT            = 0x012D; //
private final static int META_SETTEXTALIGN            = 0x012E; //
private final static int META_DRAWTEXT                = 0x062F;
private final static int META_CHORD                   = 0x0830; //
private final static int META_SETMAPPERFLAGS          = 0x0231;
private final static int META_EXTTEXTOUT              = 0x0A32; //
private final static int META_SETDIBTODEV             = 0x0D33;
private final static int META_SELECTPALETTE           = 0x0234; //
private final static int META_REALIZEPALETTE          = 0x0035; //
private final static int META_ANIMATEPALETTE          = 0x0436;
private final static int META_SETPALENTRIES           = 0x0037;
private final static int META_POLYPOLYGON             = 0x0538; //
private final static int META_RESIZEPALETTE           = 0x0139;
private final static int META_DIBBITBLT               = 0x0940; //
private final static int META_DIBSTRETCHBLT           = 0x0B41; //
private final static int META_DIBCREATEPATTERNBRUSH   = 0x0142; //
private final static int META_STRETCHDIB              = 0x0F43; //
private final static int META_EXTFLOODFILL            = 0x0548;
private final static int META_RESETDC                 = 0x014C;
private final static int META_STARTDOC                = 0x014D;
private final static int META_STARTPAGE               = 0x004F;
private final static int META_ENDPAGE                 = 0x0050;
private final static int META_ABORTDOC                = 0x0052;
private final static int META_ENDDOC                  = 0x005E;
private final static int META_DELETEOBJECT            = 0x01F0; //
private final static int META_CREATEPALETTE           = 0x00F7; //
private final static int META_CREATEBRUSH             = 0x00F8;
private final static int META_CREATEPATTERNBRUSH      = 0x01F9;
private final static int META_CREATEPENINDIRECT       = 0x02FA; //
private final static int META_CREATEFONTINDIRECT      = 0x02FB; //
private final static int META_CREATEBRUSHINDIRECT     = 0x02FC; //
private final static int META_CREATEBITMAPINDIRECT    = 0x02FD;
private final static int META_CREATEBITMAP            = 0x06FE;
private final static int META_CREATEREGION            = 0x06FF; //

private final static int MFCOMMENT = 15;
private final static int SRCCOPY   = 0xCC0020;
private final static int PATCOPY   = 0xF00021;
private final static int PATINVERT = 0x5A0049;
private final static int DSTINVERT = 0x550009;
private final static int BLACKNESS = 0x000042;
private final static int WHITENESS = 0xFF0062;
private final static int BI_RLE8   = 1;
private final static int BI_RLE4   = 2;

private final static int TA_BASELINE = 24;  // TextAlign options
private final static int TA_BOTTOM   =  8;
private final static int TA_CENTER   =  6;
private final static int TA_UPDATECP =  1;  // FIXME: update current postion
        final static int TA_TOP      =  0;
        final static int OPAQUE      =  2;
        final static int TRANSPARENT =  1;
        final static int ETO_GRAYED  =  1;
        final static int ETO_OPAQUE  =  2;
        final static int ETO_CLIPPED =  4;
        final static int PS_SOLID    =  0;
        final static int PS_DASH     = 1;
        final static int PS_DOT	     = 2;
        final static int PS_DASHDOT  = 3;
        final static int PS_DASHDOTDOT  =4;
	final static int PS_NULL     = 5;
        final static int PS_INSIDEFRAME =6;
}

// this an all_in_one class of GDI-objects -------------------------------------
class WmfDecObj
{
     final static int M_PEN     = 1;// the Windows GDI uses some other magic words...
     final static int M_BRUSH   = 2;
     final static int M_FONT    = 3;
     final static int M_BITMAP  = 4;
     final static int M_CLIP    = 5;
     final static int M_PALETTE = 6;

     private Color c;
     private Font f;
     private boolean f_underl;
     private int f_orient;
     private Rectangle r;
     private int magic;
     private Image ibrush, transbrush;
     private int hatch;
     private int p_style;
	 private int width;
	 private int f_escapement;
     
     WmfDecObj(Color cc,int mm)
       {
	  c=cc;
	  magic=mm;
       }
     WmfDecObj(int penattr,int w, Color cc)
     {
	  c=cc;
	  magic=M_PEN;
	  p_style=penattr;
	  width = (w<0||w>127)? 1:w;	// ak Sun Oct 30 09:48:00 CET 2005
     }
     WmfDecObj(Font ff,int underlined,int orientation, int esc)
       {
	  f=ff;
	  f_underl=underlined==0?false:true;
	  f_orient=orientation;
	  f_escapement = esc;
	  magic=M_FONT;
       }
     WmfDecObj(Image ii)
       {
	  ibrush=ii;
	  c=null;
	  magic=M_BRUSH;
       }

     WmfDecObj(int hatchstyle,Color cc,Color back,Component fr)
       {
	  c=cc;				// TRANSPARENT mode not suppd
	  hatch=hatchstyle;
	  ibrush=createHatchImage(hatchstyle,cc,back,fr,false);
	  transbrush=createHatchImage(hatchstyle,cc,back,fr,true);
	  magic=M_BRUSH;
       }

     WmfDecObj(int left,int top,int right,int bottom)
       {
	  r=new Rectangle(left,top,right-left,bottom-top);
	  magic=M_CLIP;
       }

     Color getColor()
       {
	  return c;
       }
     Image getImage()
       {
	  return ibrush;	//draw a hatch image if OPAQUE mode ?
       }

     Image getImage(boolean trans)
     {
		if (trans && transbrush != null)
			return transbrush;
		return ibrush;
     }

     Font getFont()
       {
	  return f;
       }
     boolean isUnderlined()
     { 
	  return f_underl;
     }
     int getFontOrientation()
     { 
	  return f_orient;
     }
     int getPenStyle()
     { 
	  return p_style;
     }

     Rectangle getRect()
       {
	  return r;
       }
//     int getHatch()
//     {
//	return hatch;
//     }
   
     int getMagic()
       {
	  return magic;
       }

	 int getWidth()
	 {
		 return width;
	 }

	 int getFontEscapement()
	 {
		 return f_escapement;
	 }

   Image createHatchImage(int hatchstyle,Color cc,Color back,Component fr, boolean trans)
     {
	Image im;
	int i,pixels[] =new int[64];
	// from HS_HORIZONTAL=0   up to   HS_DIAGCROSS=5
	int set[][]={{32,33,34,35,36,37,38,39},{4,12,20,28,36,44,52,60},
	             {0,9,18,27,36,45,54,63},{7,14,21,28,35,42,49,56},
	             {32,33,34,35,36,37,38,39,4,12,20,28,   44,52,60},
	             {0,9,18,27,36,45,54,63,7,14,21,28,35,42,49,56 }};

	int bg = trans ? new Color(255,255,255,0).getRGB() : back.getRGB();
	for (i=0;i<64;i++)
		pixels[i]=bg; //back.getRGB();/*Color.white.getRGB();*/

	try
	  {
	     for (i=0;i<set[hatchstyle].length;i++)
	  	  pixels[ set[hatchstyle][i] ]=cc.getRGB();
	     MemoryImageSource mis=
	          new MemoryImageSource(8, 8, ColorModel.getRGBdefault(), pixels, 0, 8);
	     im=fr.createImage(mis);
	     mis=null;
	  }
	catch (ArrayIndexOutOfBoundsException e)
	  {
	     im=null;
	     System.out.println("unknown hatchstyle found.");
	  }
	return im;
     }

}


// Consider: this is NOT really what Windows-GDI does!
class WmfDecDC implements Cloneable
{
     WmfDecDC(int extX,int extY,int orgX,int orgY)
       {
	  // init (some metafiles don't call META_SETWINDOWEXT/META_SETWINDOWORG !)
	  winextX=(short)(truewidth=extX);
	  winextY=(short)(trueheight=extY);
	  winorgX=(short)orgX;
	  winorgY=(short)orgY;
	  aktclip= new WmfDecObj(winorgX,winorgY,winextX,winextY);
          aktpen = new WmfDecObj(WmfDecoder.PS_SOLID,1,Color.black);
          aktbrush=new WmfDecObj(Color.white,WmfDecObj.M_BRUSH);
          aktpal  =new WmfDecObj(Color.white,WmfDecObj.M_PALETTE);
          aktbmp  =new WmfDecObj(Color.white,WmfDecObj.M_BITMAP);
          aktfont =new WmfDecObj(new Font("Courier",Font.PLAIN,12),0,0,0);
      }

	// this is our "device context" (DC), init'd with GDI defaults
	// it has 6 selectable objects
	public WmfDecObj aktpen,aktbrush,aktpal,aktbmp,aktclip,aktfont;

	public Color akttextc=Color.black;
	public Color aktbackgnd=Color.white;	// for usage in NULL-pens and -brushs
	public int aktYpos=0;			// current graphic cursor (MoveTo,LineTo)
	public int aktXpos=0;
	public short winextX=(short)1;		// for SetWindowExt()
	public short winextY=(short)1;
	public short winorgX=(short)0;		// for SetWindowOrg()
	public short winorgY=(short)0;
	public int slevel=0;			// SaveDC-level
        public int akttextalign=WmfDecoder.TA_TOP;
        public int aktbkmode=WmfDecoder.OPAQUE;
	public Graphics gr;

	//--------- still missing: ROP, ViewPort,MapMode,PolyfillMode etc.


	private int trueheight,truewidth;	// not part of GDI device context

        int ytransfer(short coo)
	{
	    int icoo=coo;	    
	    icoo-=winorgY;
	    icoo*=trueheight;
	    return icoo/winextY;
	}
        int xtransfer(short coo)
	{
	    int icoo=coo;
	    icoo-=winorgX;
	    icoo*=truewidth;
	    return icoo/winextX;
	}

        public Object clone()	// not yet ready
	{
	    try 
	      { return super.clone(); }
	    catch (CloneNotSupportedException e) 
	      { return null; }  // this never happens?
	}
}
