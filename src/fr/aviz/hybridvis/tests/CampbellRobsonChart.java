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

import fr.aviz.hybridvis.WallRenderer;
import fr.aviz.hybridvis.models.CSF;
import fr.aviz.hybridvis.models.FrequenciesAndAngles;

/**
 * Draw a Campbell-Robson Contrast Sensitivity Chart.
 * 
 * Code inspired from http://faculty.washington.edu/gboynton/publications/MakeCSF.m
 * 
 * @author dragice
 *
 */
public class CampbellRobsonChart extends WallRenderer {

	// FIXME: the following parameters are optimized for the WILD display. Generalize.
	
	// Defines the initial period for the x-axis.
	// The value of 7.38 gives a final period of 4 (2-pixel wide stripes) on the WILD display.
	// A final period of 2 creates too many Moiré artefacts.
	double initial_period_coeff = 7.38;
	
	// Drawing resolution in the preview mode
	int xstep0_preview = 100;
	int xstep1_preview = 20;
	int ystep_preview = 100;
	
	// Drawing resolution for the final rendering
	int xstep0 = 10;
	int xstep1 = 0;
	int ystep = 10;
	
	// The y axis value normalized between 0 and 1 is raised to that power
	double y_pow = 1;
	
	// Draw CSF curve?
	boolean drawCSF = true;

	// CSF curve thickness in pixels
	double CSF_thickness = 40;	

	// Inverse CSF curve?
	boolean inverseCSF = false;
	
	public static void main(String[] args) {
		CampbellRobsonChart viewer = new CampbellRobsonChart();
		viewer.showOnScreen();
	}
	
	public CampbellRobsonChart() {
		super();
	}
	
	@Override
	public void drawPreview(Graphics2D g) {
		drawCambpellRobsonChart(g, xstep0_preview, xstep1_preview, ystep_preview);
	}

	@Override
	public void drawFinal(Graphics2D g) {
		drawCambpellRobsonChart(g, xstep0, xstep1, ystep);
	} 

	public void drawCambpellRobsonChart(Graphics2D g, int stepx0, int stepx1, int stepy) {
		
		if (drawCSF && !getSimulatedDisplay().isInitialized())
			return;
		
		int w = getWallWidth();
		int h = getWallHeight();
		Rectangle2D.Double tmpRect = new Rectangle2D.Double();
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		// The value of 7.38 gives a final period of 4 (2-pixel wide stripes). A period of 2 creates too many Moiré artefacts.
		double a = initial_period_coeff / w;
		
		int stepx = stepx0;
		double ty, brightness;
		double xs, sin_xs, period = 0;
		for (int x = 0; x <= w; x += stepx) {
			xs = Math.exp(1 + a*x);
			sin_xs = Math.sin(xs);
			period = 2 * Math.PI / (a * xs); // for CSF. Derivative of xs is a * exp(1 + a*x).
			for (int y = 0; y <= h; y += stepy) {
				ty = (y / (double)h);
				if (y_pow != 1)
					ty = Math.pow(ty, y_pow);
				brightness = sin_xs * Math.pow(ty, 3);
				brightness = (brightness + 1) / 2.0;
				g.setColor(new Color((float)brightness, (float)brightness, (float)brightness));
				tmpRect.setFrame(x, y, stepx+1, stepy+1);
				g.fill(tmpRect);
			}
			
			if (drawCSF) {
				double fi = 1 / period;
				double fs = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree(fi, getSimulatedDisplay());
				
				// Original CSF
				double A = CSF.getCSF(2*fs);
				
				// CSF corrected for what we actually see on the screen (needs more tuning)
				//double A = Math.pow(CSF.getCSF(fs*2), 0.3);
				
				if (inverseCSF)
					A = 1 - A;
				
				final double r = CSF_thickness;
				tmpRect.setFrame(x - r, (1 - A) * h - r, r, r);
				g.setColor(Color.red);
				g.fill(tmpRect);
			}
				
			stepx = Math.max(1, stepx0 + (stepx1 - stepx0) * x / w);
		}
//		System.err.println("The final period is " + period + " cycles per pixel.");
	}

}
