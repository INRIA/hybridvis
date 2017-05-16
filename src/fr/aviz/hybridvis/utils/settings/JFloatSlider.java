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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.aviz.hybridvis.utils.MathUtils;
import fr.aviz.hybridvis.utils.NiceLabels;

/**
 * 
 * @author Pierre Dragicevic
 *
 */
public class JFloatSlider extends JPanel {

	static final int SLIDER_MIN = 0;
	static final int SLIDER_MAX = 10000;

	final Settings object;
	final Field field;
	final int precision;
	final double pow;
	final double min, max;
	final JSlider slider;
	final NiceLabels niceLabels;
	final JTextField text;
	boolean enableTextToSliderUpdate = true;
	boolean enableSliderToTextUpdate = true;
	
	double value;
	
	public JFloatSlider(Settings object, Field field, double min, double max, double majorTick, double minorTick, int precision, double pow) {
		super();
		this.object = object;
		this.field = field;
		this.min = min;
		this.max = max;
		this.precision = precision;
		this.pow = pow;
		this.value = getObjectValue();
		
		// --- Create text area
		text = new JTextField(niceFormat(value));
		text.setPreferredSize(new Dimension(50, 32));
		text.setAlignmentY(Component.CENTER_ALIGNMENT);

		// --- Create slider
		slider = new JSlider(SLIDER_MIN, SLIDER_MAX, clip(valueToSlider(value)));
		niceLabels = new NiceLabels(min, max, 6);

		if (pow == 1) {
			slider.setMajorTickSpacing(valueToSlider(majorTick));
			slider.setMinorTickSpacing(valueToSlider(minorTick));
		}

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		for(double v: niceLabels.computeLabels()){
			String s = niceFormat(MathUtils.round(v, precision));
			labels.put(new Integer(valueToSlider(v)), new JLabel(s));
		}
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		// Smaller fonts
		Font smallFont = new Font("Helvetica", 0, 10);
		for (JLabel lbl : labels.values()) {
			lbl.setFont(smallFont);
			lbl.setSize(lbl.getPreferredSize());
		}
		slider.setPreferredSize(new Dimension(200, 45));
		
		// Listeners
		slider.getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!slider.getValueIsAdjusting())
					return;
				if (enableSliderToTextUpdate) {
					double v = sliderToValue(slider.getValue());
					v = MathUtils.round(v, JFloatSlider.this.precision);
					setObjectValue(v);
					enableTextToSliderUpdate = false;
					text.setText(niceFormat(v));
					enableTextToSliderUpdate = true;
				}
			}
		});		
		text.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (enableTextToSliderUpdate) {
					double v = clipValue(Double.parseDouble(text.getText()));
					setObjectValue(v);
					enableSliderToTextUpdate = false;
					slider.setValue(valueToSlider(v));
                                        // Update text as object value may have been clipped.
                                        text.setText(niceFormat(v));
					enableSliderToTextUpdate = true;
				}
			}
		});
		object.addListener(new Settings.SettingsListener() {
			@Override
			public void settingsChangedFromControlPanel() {
			}
			@Override
			public void settingsChangedFromCode() {
				double value = getObjectValue();
				text.setText(niceFormat(value));
				slider.setValue(valueToSlider(value));
			}
		});
		
		// Add components
		setLayout(new BorderLayout());
		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
		wrapper.add(text);
		add(wrapper, BorderLayout.WEST);
		add(slider, BorderLayout.CENTER);
	}
	
	protected double getObjectValue() {
		return getDoubleValue(object, field);
	}
	
	protected void setObjectValue(double value) {
		if (value == this.value)
			return;
		JFloatSlider.this.value = value;
		setDoubleValue(JFloatSlider.this.object, JFloatSlider.this.field, value);
		object.fireSettingsChangedFromControlPanel();
	}
	
	private double sliderToValue(int v) {
		double t = (v - SLIDER_MIN) / (double)(SLIDER_MAX - SLIDER_MIN);
		t = Math.pow(t,  pow);
		double val = min + t * (max - min);
		return MathUtils.round(val, precision);
	}
	
	private int valueToSlider(double v) {
		double t = (v - min) / (max-min);
		t = Math.pow(t, 1/pow);
		return (int)Math.round(SLIDER_MIN + t * (SLIDER_MAX - SLIDER_MIN));
	}

	private int clip(int sliderValue){
		return Math.max(SLIDER_MIN, Math.min(SLIDER_MAX, sliderValue));
	}

        /**
         * clips <pre>val</pre> between <pre>min</pre> and <pre>max</pre>.
         */
        private double clipValue(double val){
                return Math.max(min, Math.min(max, val));
        }
	
        /**
         * Formats <pre>n</pre> using <pre>precision</pre> fractional digits.
         */
        private String niceFormat(double n){
          return String.format("%." + precision + "f", n);
        }
	
	private static double getDoubleValue(Object o, Field f) {
		f.setAccessible(true);
		Class t = f.getType();
		try {
			if (t == double.class)
				return ((Double)f.get(o)).doubleValue();
			else if (t == float.class)
				return ((Float)f.get(o)).doubleValue();
			else if (t == int.class)
				return ((Integer)f.get(o)).doubleValue();
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	private void setDoubleValue(Object o, Field f, double value) {
		f.setAccessible(true);
		Class t = f.getType();
		try {
			if (t == double.class)
				f.set(o, value);
			else if (t == float.class)
				f.set(o, (float)value);
			else if (t == int.class)
				f.set(o, (int)value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
