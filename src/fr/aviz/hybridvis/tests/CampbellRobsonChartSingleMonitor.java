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

import fr.aviz.hybridvis.display.ClientDisplayConfiguration;

/**
 * Draws a Campbell-Robson Contrast Sensitivity Chart on a 2560x1600 monitor.
 * 
 * FIXME: this code is somehow bugged.
 * 
 * @author dragice
 *
 */
public class CampbellRobsonChartSingleMonitor extends CampbellRobsonChart {

	public static void main(String[] args) {
		CampbellRobsonChartSingleMonitor viewer = new CampbellRobsonChartSingleMonitor();
		viewer.showOnScreen();
	}
	
	public CampbellRobsonChartSingleMonitor() {
		super();
		
		setSimulatedDisplay(new ClientDisplayConfiguration());
		getSimulatedDisplay().setViewerDistance(1.10);
		getClientDisplay().setViewerDistance(1.10);
		drawTiles = false;
		
		// FIXME: the following parameters are optimized for a 2560x1600 display. Generalize.
		
		// Defines the initial period for the x-axis.
		initial_period_coeff = 5.581;
		
		// Drawing resolution in the preview mode
		xstep0_preview = 10;
		xstep1_preview = 20;
		ystep_preview = 10;
		
		// Drawing resolution for the final rendering
		xstep0 = 1;
		xstep1 = 1;
		ystep = 2;
		
		// The y axis value normalized between 0 and 1 is raised to that power
		// Here using a value of ~3.5 will make the boundary of the campbell robson chart almost
		// superimpose with the CSF function.
		y_pow = 3.5;
		
		// CSF curve thickness in pixels
		CSF_thickness = 2;
		
		// Inverse CSF curve?
		inverseCSF = false;
	}
}
