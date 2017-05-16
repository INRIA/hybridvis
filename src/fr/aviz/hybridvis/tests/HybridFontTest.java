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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.models.TextLegibility;
import fr.aviz.hybridvis.utils.GUIUtils;


/**

 * 
 * @author dragice
 * 
 */

public class HybridFontTest extends HybridImageRenderer {

	// Note: more custom fonts have been added in the fonts/ folder and can be tested.
	//
	// The Frutiger font is used for signage and in the CDG airport. It's not free, though, and
	// I'm not sure the font file I found is correct. The CartoGothicStd is a copy of Frutiger.
	// 
	// FontLoader.loadFont("FrutigerBold.ttf"),
	// FontLoader.loadFont("CartoGothicStd-Bold.otf"),
	// 
	
	Font[] testedfonts = new Font[] {
			new Font("Helvetica", Font.BOLD, 12),
			new Font("Helvetica", 0, 12),
			new Font("Times", 0, 12),
			new Font("Courier", 0, 12)
	};

	int[] testedFontSizes_oneperscreen = new int[] {
			6, 8, 10, 12, 16, 20, 28, 40
	};

	int[] testedFontSizes_onefontperscreen = new int[] {
			50, 60, 80, 100, 150
	};

	int[] testedFontSizes_allscreens = new int[] {
			200, 300, 350
	};

	int[] testedFontSizes_allscreenssinglefont = new int[] {
			500, 700, 1000
	};

	int tileMargin = 200; 

	/**
	 * The code to run this class. It will instantiate this component, put it into a JFrame and make it visible on the screen.
	 * @param args
	 */
	public static void main(String[] args) {
		HybridFontTest viewer = new HybridFontTest();
		viewer.showOnScreen();
	}

	/**
	 * Initialization code.
	 */
	public HybridFontTest() {

		super();

		// Configure the HybridImage Renderer.
		setDrawNearImage(false);
		setHipassRadius(10);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setDrawFarImage(true);
		setBlurRadius(50);
		setFarImageOpacity(0.5);
		setPostContrast(1.5);
		setPostBrightness(0.77);
		setDrawPowerSpectrum(false);
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
	 */	@Override
	 public void drawNearGraphics(Graphics2D g) {
		 drawAllFonts(g);
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
		 drawAllFonts(g);
	 }


	 protected void drawAllFonts(Graphics2D g) {

		 // This will activate more precise (but also more ugly) font rendering when zoomed out
		 if (g.getTransform().getScaleX() != 1)
			 g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		 int tx = getSimulatedDisplay().getXTiles();
		 int ty = getSimulatedDisplay().getYTiles();
		 int tw = getSimulatedDisplay().getTileXResolution();
		 int th = getSimulatedDisplay().getTileYResolution();

		 // fill the background (important)
		 g.setColor(Color.white);
		 g.fillRect(0, 0, tx*tw, ty*th);

		 // fonts
		 g.setColor(Color.black);

		 int y = tileMargin;
		 for (int size : testedFontSizes_oneperscreen) {
			 for (int i=0; i<tx; i++) {
				 int x = i*tw + tileMargin;
				 for (Font font : testedfonts) {
					 drawFont(g, font, size, x, y);
					 x += (tw - tileMargin*2) / (testedfonts.length);
				 }
			 }
			 y += 20 + size * 1.5;
		 }

		 for (int size : testedFontSizes_onefontperscreen) {
			 for (int i=0; i<tx; i++) {
				 int x = i*tw + tileMargin;
				 Font font = testedfonts[i % testedfonts.length];
				 drawFont(g, font, size, x, y);
			 }
			 y += 20 + size * 1.5;
		 }

		 y = th + tileMargin;
		 for (int size : testedFontSizes_allscreens) {
			 int x = tileMargin;
			 for (Font font : testedfonts) {
				 drawFont(g, font, size, x, y);
				 x += (tx*tw) / (testedfonts.length);
			 }
			 y += 20 + size * 1.5;
		 }

		 y = 2*th + tileMargin;
		 for (int size : testedFontSizes_allscreenssinglefont) {
			 int x = tileMargin;
			 Font font = testedfonts[0];
			 drawFont(g, font, size, x, y);
			 y += 20 + size * 1.3;
		 }
	 }

	 protected void drawFont(Graphics2D g, Font font, int size, double x, double y) {
		 Font resizedfont = font.deriveFont((float)size);
		 g.setFont(resizedfont);
		 double xheight = TextLegibility.getXHeightInPixels(resizedfont);
		 double width = TextLegibility.getAverageLetterWidthInPixels(resizedfont, false);
		 String text = font.getFontName() + " " + size + "-" + (int)Math.round(xheight) + "-" + (int)Math.round(width);;
		 GUIUtils.drawText(g, text, x, y, Double.MAX_VALUE, Double.MAX_VALUE, GUIUtils.HALIGN.Left, GUIUtils.VALIGN.Top, 0);
	 }


}
