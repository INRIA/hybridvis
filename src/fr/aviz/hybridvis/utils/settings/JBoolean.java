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

import java.awt.BorderLayout;
import java.lang.reflect.Field;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author Pierre Dragicevic
 *
 */
public class JBoolean extends JPanel {

	static final int SLIDER_MIN = 0;
	static final int SLIDER_MAX = 10000;

	final Settings object;
	final Field field;
	JCheckBox checkbox;
	
	boolean value;
	
	public JBoolean(Settings object, Field field) {
		super();
		this.object = object;
		this.field = field;
		this.value = getObjectValue();
		
		// --- Create checkbox
		checkbox = new JCheckBox();
                checkbox.setSelected(value);
		
		// Listeners
		checkbox.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean v = checkbox.isSelected();
				setObjectValue(v);
			}
		});
		object.addListener(new Settings.SettingsListener() {
			@Override
			public void settingsChangedFromControlPanel() {
			}
			@Override
			public void settingsChangedFromCode() {
				boolean value = getObjectValue();
				checkbox.setSelected(value);
			}
		});

		
		// Add components
		setLayout(new BorderLayout());
		add(checkbox, BorderLayout.CENTER);
	}
	
	protected boolean getObjectValue() {
		return getBooleanValue(object, field);
	}
	
	protected void setObjectValue(boolean value) {
		if (value == this.value)
			return;
		this.value = value;
		setBooleanValue(this.object, this.field, value);
		object.fireSettingsChangedFromControlPanel();
	}
	
	private static boolean getBooleanValue(Object o, Field f) {
		f.setAccessible(true);
		Class t = f.getType();
		try {
			if (t == boolean.class)
				return ((Boolean)f.get(o)).booleanValue();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void setBooleanValue(Object o, Field f, boolean value) {
		f.setAccessible(true);
		Class t = f.getType();
		try {
			if (t == boolean.class)
				f.set(o, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
