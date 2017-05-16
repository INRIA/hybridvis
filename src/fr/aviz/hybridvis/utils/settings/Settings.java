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

/**
 * Authors: Pierre Dragicevic
 * Created: Jun 19, 2010 2:42:09 PM
 */
package fr.aviz.hybridvis.utils.settings;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @author Pierre Dragicevic
 */
public class Settings {

	public static interface SettingsListener {
		public void settingsChangedFromControlPanel();
		public void settingsChangedFromCode();
	}

	private String name;
	private ControlPanel controlpanel = null;
	private final ArrayList<SettingsListener> changeListeners = new ArrayList<SettingsListener>();
	
	public Settings(String name) {
		this.name = name;
	}

	public Settings() {
		this("");
	}

	public String getName() {
		return name;
	}
	
	public JPanel getControlPanel() {
		if (controlpanel == null)
			controlpanel = new ControlPanel(this);
		return controlpanel;
	}

        public JFrame makeControlFrame() {
		JFrame window = new JFrame(getName());
		window.getContentPane().add(getControlPanel());
		window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		window.pack();
		window.setVisible(true);
                return window;
        }
	
        // [RP] left here for backward compatibility
	public void showControlPanel() {
		makeControlFrame();
	}
	
	/**
	 * Adds a change listener to these settings.
	 * Note: the listener will only be notified of the changes made from the control panel.
	 * FIXME: should we also support programmatical changes?
	 * @param l
	 */
	public void addListener(SettingsListener l) {
		changeListeners.add(l);
		l.settingsChangedFromControlPanel();
	}

	public void removeChangeListener(SettingsListener l) {
		changeListeners.remove(l);
	}
	
	public void settingsChangedFromCode() {
		fireSettingsChangedFromCode();
	}
	
	protected void fireSettingsChangedFromControlPanel() {
		for (SettingsListener l : changeListeners)
			l.settingsChangedFromControlPanel();
	}

	public void fireSettingsChangedFromCode() {
		for (SettingsListener l : changeListeners)
			l.settingsChangedFromCode();
	}

}
