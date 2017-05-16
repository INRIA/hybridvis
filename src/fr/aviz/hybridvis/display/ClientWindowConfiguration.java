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

import java.awt.GraphicsConfiguration;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

/**
 * Creates a DisplayConfiguration that is linked to the window/component given to the constructor.
 * 
 * @author dragice
 *
 */
public class ClientWindowConfiguration extends DisplayConfiguration {

	JComponent window;
	boolean manualResolutionUpdate = false;
	
	public ClientWindowConfiguration(JComponent window, boolean manualResolutionUpdate) {
		
		super();
		this.window = window;
		this.manualResolutionUpdate = manualResolutionUpdate;
		update();
		window.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				update();
			}
			
			@Override
			public void componentMoved(ComponentEvent arg0) {
				update();
			}
		});
	}
	
	public void update() {
		
		if (!window.isShowing()) {
			return;
		}
		
		// Get window dimensions
		if (!(initialized && manualResolutionUpdate)) {
			xResolution = window.getWidth();
			yResolution = window.getHeight();
		}
		
		// Get pixel size
		GraphicsConfiguration gc = window.getGraphicsConfiguration();
		AffineTransform at = gc.getDefaultTransform();
		at.concatenate(gc.getNormalizingTransform());
		// After applying the transform, 72 pixels should cover 1 inch (2.54 cm)
		pixelWidth = round(1.0 / at.getScaleX() / 72 * (2.54 * CM), 5);
		pixelHeight = round(1.0 / at.getScaleY() / 72 * (2.54 * CM), 5);
		
		initialized = true;
		
		window.repaint();
	}

	public void setManualResolutionUpdate(boolean manualResolutionUpdate) {
		this.manualResolutionUpdate = manualResolutionUpdate;
	}

	/**
	 * Change resolution manually
	 */
	public void setResolution(int width, int height) {
		xResolution = width;
		yResolution = height;
		window.repaint();
	}
	
	private static double round(double n, int precision) {
		int mul = (int)Math.pow(10, precision);
		return Math.round(n * mul) / (double)mul;
	}
}
