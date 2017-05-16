/*
 * HybridVis - Hybrid visualizations generator and library
 * Copyright (C) 2016 Inria
 *
 * This file is part of HybridVis.
 *
 * HybridVis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HybridVis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HybridVis.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.aviz.hybridvis.examples;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.display.DisplayConfiguration;


/**
 * Used for the figures in section 4 of the paper.
 * 
 * @author dragice
 * 
 */

public class HybridMapSection4 extends HybridImageRenderer {

	// Example 1 uses two google maps.
	// Example 2 uses a higher-resolution metro map.
	public static int EXAMPLE = 2;
	
	BufferedImage hiresImage = null;
	BufferedImage lowresImage = null;
	Rectangle mapImageBounds = new Rectangle();
	final int copies = 1;

	public static void main(String[] args) {
		DisplayConfiguration simulatedDisplay = new DisplayConfiguration(3 * 2560, 4 * 1600, 3, 4, 0.250 * DisplayConfiguration.MM, 0.250 * DisplayConfiguration.MM);
		HybridMapSection4 viewer = new HybridMapSection4(simulatedDisplay);
		viewer.showOnScreen();
	}
	
	public HybridMapSection4(DisplayConfiguration display) {
		
		super(display);

		String nearMapFile, farMapFile;

		if (EXAMPLE == 1) {
			// Settings for using the two google maps.
			nearMapFile = "data/maps/paris-hires.png";
			farMapFile = "data/maps/paris-lowres.png";
			setHipassRadius(30);
			setHipassContrast(1.5f);
			setHipassBrightness(1.5f);
			setBlurRadius(30);
			setFarImageOpacity(0.5);
			setPostContrast(2.0);
			setPostBrightness(0.77);
		} else {
			// Settings for using a higher-resolution metro map.
			nearMapFile = "data/maps/paris-hires.png";
			farMapFile = "data/maps/paris-lowres2.png";
			setHipassRadius(10);
			setHipassContrast(1.5f);
			setHipassBrightness(1.5f);
			setBlurRadius(15);
			setFarImageOpacity(0.5);
			setPostContrast(2.0);
			setPostBrightness(0.77);
		}
		
		setDrawBezels(false);
		setDrawBackground(false);
		setDrawPowerSpectrum(false);
		setDrawSettings(false);
		
		// Load the near and far map images
		System.out.print("Loading map images... ");
		try {
			hiresImage = ImageIO.read(new File(nearMapFile));
			lowresImage = ImageIO.read(new File(farMapFile));
			//lowresImage = createHeadlessBufferedImage(lowresImage, hiresImage.getWidth(), hiresImage.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
		
		// Place the hybrid map on the wall-sized display so its resolution is the same as the hires image and it is centered.
		mapImageBounds.setRect((getWallWidth() - hiresImage.getWidth()) / 2, (getWallHeight() - hiresImage.getHeight()) / 2, hiresImage.getWidth(), hiresImage.getHeight());		
	}
	
	/**
	   * Creates a <code>BufferedImage</code> from an <code>Image</code>. This method can
	   * function on a completely headless system. This especially includes Linux and Unix systems
	   * that do not have the X11 libraries installed, which are required for the AWT subsystem to
	   * operate. This method uses nearest neighbor approximation, so it's quite fast. Unfortunately,
	   * the result is nowhere near as nice looking as the createHeadlessSmoothBufferedImage method.
	   * 
	   * @param image  The image to convert
	   * @param w The desired image width
	   * @param h The desired image height
	   * @return The converted image
	   * @param type int
	   */
	  public static BufferedImage createHeadlessBufferedImage(BufferedImage image, int width, int height)
	  {
      int type = BufferedImage.TYPE_INT_ARGB;

	    BufferedImage bi = new BufferedImage(width, height, type);

	    for (int y = 0; y < height; y++) {
	      for (int x = 0; x < width; x++) {
	        bi.setRGB(x, y, image.getRGB(x * image.getWidth() / width, y * image.getHeight() / height));
	      }
	    }

	    return bi;
	  }

	/**
	 * Draws the visual content that will be visible when the user is close to the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * The high-pass filter requires an opaque image, so make sure you first fill the background with an opaque color.
	 * 
	 */	@Override
	public void drawNearGraphics(Graphics2D g) {
		for (int i=0; i<copies; i++) {
			int x0 = i * (getWallWidth() / copies) + (getWallWidth() / copies / 2) - (mapImageBounds.width) / 2; 
			g.drawImage(hiresImage, x0, (mapImageBounds.y), (mapImageBounds.width), (mapImageBounds.height), null);
		}
	}

	/**
	 * Draws the visual content that will be visible when the user is far from the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * For best results, first fill the background with an opaque color. 
	 */
	@Override
	public void drawFarGraphics(Graphics2D g) {
		for (int i=0; i<copies; i++) {
			int x0 = i * (getWallWidth() / copies) + (getWallWidth() / copies / 2) - (mapImageBounds.width) / 2; 
			g.drawImage(lowresImage, x0, (mapImageBounds.y), (mapImageBounds.width), (mapImageBounds.height), null);
		}
	}
	
}
