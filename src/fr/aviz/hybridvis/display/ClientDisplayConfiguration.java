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

package fr.aviz.hybridvis.display;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;

import fr.aviz.hybridvis.utils.MathUtils;

/**
 * Automatically creates a DisplayConfiguration that describes the client where this code is running.
 * 
 * @author dragice
 *
 */
public class ClientDisplayConfiguration extends DisplayConfiguration {

	public ClientDisplayConfiguration() {
		this(0);
	}
	
	/**
	 * @param screenIndex 0 for the primary screen, 1 for the secondary screen.
	 */
	public ClientDisplayConfiguration(int monitor) {

		super();
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getScreenDevices()[monitor];
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		DisplayMode dm = gd.getDisplayMode();
		xResolution = dm.getWidth();
		yResolution = dm.getHeight();
		
		AffineTransform at = gc.getDefaultTransform();
		at.concatenate(gc.getNormalizingTransform());
		// After applying the transform, 72 pixels should cover 1 inch (2.54 cm)
		double xPixelSize = MathUtils.round(1.0 / at.getScaleX() / 72 * (2.54 * CM), 5);
		double yPixelSize = MathUtils.round(1.0 / at.getScaleY() / 72 * (2.54 * CM), 5);
		
		init(
			xResolution, // total horizontal resolution
			yResolution, // total vertical resolution
			1, // number of horizontal tiles
			1, // number of vertical tiles
			xPixelSize, // single pixel width and height
			yPixelSize // single pixel width and height
		); 
	}

	public static void main(String[] args) {
		ClientDisplayConfiguration csc = new ClientDisplayConfiguration();
		System.out.println(csc);
	}
	
}
