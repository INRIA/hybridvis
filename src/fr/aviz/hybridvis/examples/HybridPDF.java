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
import java.awt.geom.Rectangle2D;

import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.utils.PDF.MultiScalePDFViewer;

public class HybridPDF extends HybridImageRenderer {

	MultiScalePDFViewer pdfv;

	/**
	 * Main program
	 * 
	 * @param args
	 *            a graph file to load or nothing
	 */
	public static void main(String[] args) {

		HybridPDF viewer = new HybridPDF();
		viewer.init();
		viewer.showOnScreen();
	}

	/*
	 * Init info related to MultiScalePDFViewer .
	 */
	private void init() {
		pdfv = new MultiScalePDFViewer();

		// setup read and debug directories
		MultiScalePDFViewer.readDir = "data/pdf/read/";
		MultiScalePDFViewer.debugDir = "data/pdf/";
		System.out.println("Setup for wall size (" + this.getWallWidth() + ","
				+ this.getWallHeight() + ")");
		pdfv.initialize(this.getWallWidth(), this.getWallHeight());
	}

	private HybridPDF() {
		setHipassRadius(30);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setBlurRadius(30);
		setFarImageOpacity(0.4);
		setPostContrast(1.5);
		setPostBrightness(0.77);

		if (false) { // alternative setups
			setHipassRadius(40);
			setBlurRadius(30);

			setHipassRadius(40);
			setBlurRadius(20);
		}
	}

	@Override
	public void drawNearGraphics(Graphics2D g) {
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(),
				getWallHeight());
		g.drawImage(MultiScalePDFViewer.closeImg, null, 0, 0);
	}

	@Override
	public void drawFarGraphics(Graphics2D g) {
		Rectangle2D wallbounds = new Rectangle2D.Double(0, 0, getWallWidth(),
				getWallHeight());
		g.drawImage(MultiScalePDFViewer.farImg, null, 0, 0);

	}

}
