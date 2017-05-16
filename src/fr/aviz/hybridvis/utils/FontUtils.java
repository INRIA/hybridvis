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

package fr.aviz.hybridvis.utils;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.FontMetrics;

public class FontUtils {

	public static int findFontSizeForWidth(Graphics2D g, String label, Font maxFont, double maxLabelWidth ) {
		boolean fits = false;
		//boolean trimLabel = false; 
		FontMetrics metrics = null;
		int fontSize = maxFont.getSize();
		//System.out.println("Trimmin Font");

		while(!fits){

			//if (!trimLabel) {
			metrics = g.getFontMetrics(maxFont);
			Rectangle2D stringBounds = metrics.getStringBounds(label, g);

			if (stringBounds.getWidth() <= maxLabelWidth) {
				fits = true;
			} 
			else {
				//if (!trimLabel) {
				fontSize -= 1 + fontSize / 10;
				if (fontSize < 9) {
					fontSize = 9;
					label += "\u2026";
				}
				maxFont = new Font(maxFont.getFontName(), 0, fontSize);
			}	
		}

		//}
		return fontSize;
	}
}
