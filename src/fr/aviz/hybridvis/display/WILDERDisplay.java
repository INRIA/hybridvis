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

/**
 * 
 * @author dragice
 *
 */
public class WILDERDisplay extends DisplayConfiguration {
	
	//screen called AD 22-Salvador
	//diagonal: 549mm
	//pixels: 960x960
	//active screen area: 387.4mm (same for height and width)
	//physical dimension: 395mm
	//bezel width: 5.9mm (.23'')
	
	public WILDERDisplay() {
		super(
			15 * 960, // total horizontal resolution
			5 * 960, // total vertical resolution
			15, // number of horizontal tiles
			5, // number of vertical tiles
			0.4035 * MM, // single pixel width (verify)
			0.4035 * MM // single pixel height (verify)
		); 
	}

	public WILDERDisplay(double viewerDistance) {
		this();
		setViewerDistance(viewerDistance);
	}
}
