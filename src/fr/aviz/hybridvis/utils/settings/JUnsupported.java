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

package fr.aviz.hybridvis.utils.settings;

import java.awt.Color;
import java.lang.reflect.Field;

import javax.swing.JLabel;

/**
 * 
 * @author dragice
 *
 */
public class JUnsupported extends JLabel {

	public JUnsupported(Field f) {
		super("Fields of type " + f.getType().getSimpleName() + " are not yet supported.");
		setForeground(new Color(0.75f, 0f, 0f));
	}
	
}
