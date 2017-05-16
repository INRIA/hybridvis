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

import java.awt.BasicStroke;
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

public class HybridTextLegibilityModelTest extends HybridImageRenderer {

	Font[] testedfonts = new Font[] {
			//FontLoader.loadFont("CartoGothicStd-Bold.otf"), // shameful copy of Frutiger. Avoid using it.
			//FontLoader.loadFont("ClearviewHwy2W.ttf"), // this font is copyrighted, don't use it!
			new Font("Helvetica", Font.BOLD, 12),
			new Font("Helvetica", 0, 12),
			new Font("Times", 0, 12),
	};
	
	double testedBlurRadius = 50;

	double[] testedLegibilityCoefficients = new double[] {
			0.35, // the limit of legibility is around 0.28
			0.25, // the limit of legibility is around 0.28
			0.15,
			0.10,
	};

	String[] labelsForLegibilityCoefficients = new String[] {
			"This text should be illegible",
			"This text should be barely legible",
			"This text should be legible",
			"This text should be comfortable to read",
	};
	
	int tileMargin = 200; 

	/**
	 * The code to run this class. It will instantiate this component, put it into a JFrame and make it visible on the screen.
	 * @param args
	 */
	public static void main(String[] args) {
		HybridTextLegibilityModelTest viewer = new HybridTextLegibilityModelTest();
		viewer.showOnScreen();
	}

	/**
	 * Initialization code.
	 */
	public HybridTextLegibilityModelTest() {

		super();

		// Configure the HybridImage Renderer.
		setDrawNearImage(false);
		setHipassRadius(0);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setDrawFarImage(true);
		setBlurRadius(testedBlurRadius);
		setFarImageOpacity(0.5);
		setPostContrast(1.5);
		setPostBrightness(0.77);
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

		 for (int uppercase_ = 0; uppercase_ <= 1; uppercase_++) {
			 boolean uppercase = (uppercase_ == 0);
			 int x = !uppercase ? tileMargin : 3 * tw + tileMargin;
			 int y = tileMargin;
			 int currentTileY = 0;
			 for (int i=0; i<testedLegibilityCoefficients.length; i++) {
				 double legibilityCoeff = testedLegibilityCoefficients[i];
				 String text = labelsForLegibilityCoefficients[i];
				 for (Font font : testedfonts) {
					 
					 // This is where we get the appropriate font size for the desired legibility,
					 // given the typeface used and amount of blur applied
					 int size = TextLegibility.getFontSizeForLegibility(font, uppercase, testedBlurRadius, legibilityCoeff);
					 
					 if (y + size > (currentTileY + 1) * th) {
						 currentTileY++;
						 y = currentTileY * th + tileMargin;
					 }
					 drawFont(g, font, uppercase, text, size, x, y);
					 y += 20 + size * 1.5;
				 }
				 g.setStroke(new BasicStroke(20));
				 y += 100;
				 g.drawLine(x, y, x + 3 * tw - 2 * tileMargin, y); 
				 y += 100;
			 }
		 }
	 }

	 protected void drawFont(Graphics2D g, Font font, boolean uppercase, String text, int size, double x, double y) {
		 Font resizedfont = font.deriveFont((float)size);
		 g.setFont(resizedfont);
		 if (uppercase)
			 text = text.toUpperCase();
		 GUIUtils.drawText(g, text, x, y, Double.MAX_VALUE, Double.MAX_VALUE, GUIUtils.HALIGN.Left, GUIUtils.VALIGN.Top, 0);
	 }
}
