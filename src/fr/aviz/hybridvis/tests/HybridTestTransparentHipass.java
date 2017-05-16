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

package fr.aviz.hybridvis.tests;

import java.awt.Color;
import java.awt.Graphics2D;

import fr.aviz.hybridvis.HybridImageRenderer;


/**
 * An example illustrating how to use HybridImageRenderer.
 * It draws vertical lines visible from close and horizontal lines visible from far. 
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized
 *   image (key 'S').
 *  
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Right-click to switch back to the overview mode.
 * - Mouse drag to pan
 * - Mouse wheel to zoom in/out
 * - Hit S to generate the full image and save it on the disk (this process will take about 10 min).
 * 
 * See HybridImageRenderer for more info.
 * 
 * @author dragice
 * 
 */

public class HybridTestTransparentHipass extends HybridImageRenderer {

	final int stripewidth = 10;
	final int bigstripewidth = 100;
	
	/**
	 * The code to run this class. It will instantiate this component, put it into a JFrame and make it visible on the screen.
	 * @param args
	 */
	public static void main(String[] args) {
		HybridTestTransparentHipass viewer = new HybridTestTransparentHipass();
		viewer.showOnScreen();
	}
	
	/**
	 * Initialization code.
	 */
	public HybridTestTransparentHipass() {
		
		super();

		setDrawBackground(true);
		
		setDrawNearImage(true);
		setHipassRadius(5);
		setTransparentHipass(true);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setNearImageOpacity(1.0f);
		
		setDrawFarImage(true);
		setBlurRadius(50);
		setFarImageOpacity(1.0f);
		
		setPostContrast(1.5);
		setPostBrightness(0.77);
		
	}

	@Override
	public void drawBackgroundGraphics(Graphics2D g) {
		g.setColor(Color.red);
		g.fillRect(0, 0, getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution());
	}
	
	/**
	 * Draws the visual content that will be visible when the user is close to the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is
	 *   (getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * The high-pass filter requires an opaque image, so make sure you first fill the background with an opaque color.
	 * 
	 */
	@Override
	public void drawNearGraphics(Graphics2D g) {
		int w = getSimulatedDisplay().getXResolution();
		int h = getSimulatedDisplay().getXResolution();
		
		// still important to fill the background!
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		// vertical lines
		g.setColor(Color.blue);
		for (int x = 0; x < w; x+= stripewidth*8) {
			g.fillRect(x, 0, stripewidth+1, h);
		}		
	}

	/**
	 * Draws the visual content that will be visible when the user is far from the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is
	 *   (getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * For best results, first fill the background with an opaque color. 
	 */
	@Override
	public void drawFarGraphics(Graphics2D g) {
		int w = getSimulatedDisplay().getXResolution();
		int h = getSimulatedDisplay().getYResolution();
		
		// don't fill the background
//		g.setColor(Color.white);
//		g.fillRect(0, 0, w, h);
		
		// horizontal lines
		g.setColor(Color.blue);
		for (int y = 0; y < h; y += bigstripewidth*8) {
			g.fillRect(0, y, w, bigstripewidth+1);
		}
	}
	
}
