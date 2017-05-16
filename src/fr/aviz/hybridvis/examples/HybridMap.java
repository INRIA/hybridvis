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


/**
 * Blends two maps using the hybrid image method.
 * 
 * This code can be reused to blend any two images loaded from the disk.
 * The near and far images do not need to be the same resolution, but they need to be aligned when scaled at
 * the same resolution (you can use photoshop for that). The near image image will be shown with scale 1:1 on the
 * wall-sized display (centered), and the far image will then be scaled up to occupy the same space.
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized image (key 'S').
 *  
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Click again to switch back to the overview mode.
 * - When transitioning between zoom levels, a preview will be shown consisting in the near and far content alpha-blended (no high-pass and blurring).
 * - Hit S to generate the full image and save it on the disk (this process will take about 10 min).
 * 
 * See HybridImageRenderer for more info.
 * 
 * @author dragice
 * 
 */

public class HybridMap extends HybridImageRenderer {

	// Example 1 uses two google maps.
	// Example 2 uses a higher-resolution metro map.
	public static int EXAMPLE = 2;
	
	BufferedImage hiresImage = null;
	BufferedImage lowresImage = null;
	Rectangle mapImageBounds = new Rectangle();

	public static void main(String[] args) {
		HybridMap viewer = new HybridMap();
		viewer.showOnScreen();
	}
	
	public HybridMap() {
		
		super();

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
			setHipassRadius(12);
			setHipassContrast(1.7f);
			setHipassBrightness(1.4f);
			setBlurRadius(12);
			setFarImageOpacity(0.4);
			setPostContrast(2.3);
			setPostBrightness(0.77);
		}
		
		// Load the near and far map images
		System.out.print("Loading map images... ");
		try {
			hiresImage = ImageIO.read(new File(nearMapFile));
			lowresImage = ImageIO.read(new File(farMapFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
		
		// Place the hybrid map on the wall-sized display so its resolution is the same as the hires image and it is centered.
		mapImageBounds.setRect((getWallWidth() - hiresImage.getWidth()) / 2, (getWallHeight() - hiresImage.getHeight()) / 2, hiresImage.getWidth(), hiresImage.getHeight());		
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
		g.drawImage(hiresImage, (mapImageBounds.x), (mapImageBounds.y), (mapImageBounds.width), (mapImageBounds.height), null);
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
		g.drawImage(lowresImage, (mapImageBounds.x), (mapImageBounds.y), (mapImageBounds.width), (mapImageBounds.height), null);
	}
	
}
