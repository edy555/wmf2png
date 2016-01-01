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

public class WmfConverter implements ImageConsumer
{
    long startTime;
    private final int TERMINATE_NOW=123456789;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();
 
    static public void main(String[] args) 
    {
        System.setProperty("java.awt.headless", "true");

        if (args.length > 0) {
            WmfConverter cvt = new WmfConverter();
            try {
                String outname = outputFile(args[0]);
                cvt.convert(args[0], outname);
            } catch(IOException e) {
                System.out.println(e);
            }
        } else
            System.out.println("Usage: java WmfConverter wmffile");
    }

    static String outputFile(String filename) {
        // replace extension to ".png"
        int i = filename.lastIndexOf('.');
        if (i >= 0) 
            filename = filename.substring(0, i);
        return filename + ".png";
    }

    public void convert(String filename, String outname)
        throws IOException {
        startTime = System.currentTimeMillis();
        WmfDecoder wdec = new WmfDecoder(new FileInputStream(filename));
        Image image = toolkit.createImage(wdec);
        wdec.startProduction(this);
        saveImage(image, outname);
    }

    public void saveImage(Image image, String savefilename)
        throws IOException {
        int w = 640, h = 480;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics big = bi.getGraphics();
        big.drawImage(image, 0, 0, null);

        File saveFile = new File(savefilename);
        ImageIO.write(bi, "png", saveFile);
    }

    /* implements ImageConsumer */
    public void imageComplete(int status) {}
    public void setColorModel(ColorModel model) {}
    public void setDimensions(int width, int height) {}
    public void setHints(int hintflags) {}
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {}
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {}
    public void setProperties(Hashtable<?,?> props) {}
}

