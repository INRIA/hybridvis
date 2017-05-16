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
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import fr.aviz.hybridvis.HybridImageRenderer;

public class HybridSignalTest extends HybridImageRenderer {

	public HybridSignalTest() {
		super();
		
		// Configure the HybridImage Renderer.
		setHipassRadius(0);
	}

	@Override
	public void drawNearGraphics(Graphics2D g) {
		int w = getSimulatedDisplay().getXResolution();
		int h = getSimulatedDisplay().getYResolution();;
		Rectangle2D.Double tmpRect = new Rectangle2D.Double();
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		drawSinPattern(g, 100.0, new Rectangle2D.Double(0, 0, w, h), 1f);
		drawSinPattern(g, 20.0, new Rectangle2D.Double(0, 0, w, h), 0.3f);
	}

	protected void drawSinPattern(Graphics2D g, double period,
			Rectangle2D.Double rectangle, float alpha){
		for (int x = (int)rectangle.x; x <= rectangle.width; x++) {
			double sinX = Math.sin(x * 2 * Math.PI / period);
			double brightness = (sinX + 1.0) / 2.0;
			g.setColor(new Color((float)brightness, (float)brightness, (float)brightness, alpha));
			g.drawRect(x, (int)rectangle.y, 1, (int)(rectangle.y + rectangle.height));
		}
	}
	
	
	@Override
	public void drawFarGraphics(Graphics2D g) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HybridSignalTest viewer = new HybridSignalTest();
		viewer.showOnScreen();
	}
}
