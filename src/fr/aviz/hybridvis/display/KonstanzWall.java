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

public class KonstanzWall extends DisplayConfiguration {

	public KonstanzWall() {
		super(
			5224, // total horizontal resolution
			2160, // total vertical resolution
			4, // number of horizontal tiles
			2, // number of vertical tiles
			0.995 * MM, // single pixel width (verify)
			0.995 * MM // single pixel height (verify)
			
			//5224*2160
			//5.2*2.15
		); 
	}

	public KonstanzWall(double viewerDistance) {
		this();
		setViewerDistance(viewerDistance);
	}
}
