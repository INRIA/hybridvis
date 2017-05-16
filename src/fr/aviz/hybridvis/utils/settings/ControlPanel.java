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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

public class ControlPanel extends JPanel {

	Settings settings;

	public ControlPanel(Settings settings) {
		this.settings = settings;
		addComponents();
	}

	private void addComponents() {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(layout);
		Font labelfont = new Font("Helvetica", 0, 12);
		
		// Get fields from class and all superclasses
		ArrayList<Field> allFields = new ArrayList<Field>();
		Class cl = settings.getClass();
		while (cl != null) {
			allFields.addAll(Arrays.asList(cl.getDeclaredFields()));
			cl = cl.getSuperclass();
		}

		int row = 0;
		for (Field f : allFields) {
			Configurable conf = null;
			boolean separator = false;
			//System.err.println(f);
			for (Annotation a :f.getAnnotations()) {
				if (a instanceof Separator) 
					separator = true;
				else if (a instanceof Configurable)
					conf = (Configurable)a;
			}
			if (conf != null) {
				
				if (separator) {
					c.weightx = 1.0;
					c.insets = new Insets(2,2,2,2);
					c.fill = GridBagConstraints.BOTH;
					c.gridx = 0;
					c.gridy = row;
					c.gridwidth = 2;
					add(createSeparator(), c);
					row++;
				}
				
				c.weightx = 0;
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(5,5,5,5);
				c.gridx = 0;
				c.gridy = row;
				c.gridwidth = 1;
				String label = conf.label();
				if (label.isEmpty())
					label = getLabelFromFieldname(f.getName());
				JLabel lbl = new JLabel(label);
				if (!conf.help().isEmpty())
					lbl.setToolTipText(conf.help());
				lbl.setFont(labelfont);
				lbl.setHorizontalAlignment(SwingConstants.RIGHT);
				lbl.setVerticalAlignment(SwingConstants.CENTER);
				add(lbl, c);
				
				JComponent widget = createWidget(settings, f, conf);
				c.weightx = 1.0;
				c.insets = new Insets(0,0,0,0);
				c.fill = GridBagConstraints.BOTH;
				c.gridx = 1;
				c.gridy = row;
				c.gridwidth = 1;
				add(widget, c);
			}
			row++;
		}
		setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
	}

	protected static JComponent createWidget(Settings o, Field f, Configurable options) {
		JComponent widget = null;
		Class type = f.getType();
		if (type == boolean.class)
			widget = new JBoolean(o, f);
		else if (type == double.class || type == float.class)
			widget = new JFloatSlider(o, f, options.min(), options.max(), options.majorTick(), options.minorTick(), options.precision(), options.pow());
		else if (type == int.class)
			widget = new JFloatSlider(o, f, options.min(), options.max(), options.majorTick(), options.minorTick(), 0, options.pow());
		else
			widget = new JUnsupported(f);
		return widget;
	}
	
	protected static JComponent createSeparator() {
		return new JSeparator(SwingConstants.HORIZONTAL);
	}
	
	protected static String getLabelFromFieldname(String s) {
		String res = "";
		int offset = 0, strLen = s.length();
		boolean firstChar = true;
		while (offset < strLen) {
		  int c = s.codePointAt(offset);
		  if (Character.isLetter(c)) {
			  if (firstChar)
				  res += new String(Character.toChars(Character.toUpperCase(c)));
			  else if (Character.isUpperCase(c))
				  res += " " + new String(Character.toChars(Character.toLowerCase(c)));
			  else
				  res += new String(Character.toChars(c));
			  firstChar = false;
		  } else {
			  res += new String(Character.toChars(c));
		  }
		  offset += Character.charCount(c);
		  // do something with curChar
		}
		return res;
	}
}
