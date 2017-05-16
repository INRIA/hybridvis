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

package fr.aviz.hybridvis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.models.FrequenciesAndAngles;
import fr.aviz.hybridvis.utils.ImageStatistics;

public class PowerSpectrumAnalyzer {

	protected JFrame window = null;
	protected JPanel overlayPanel = null;
	protected BufferedImage powerSpectrumImage = null;

	//state
	protected List<float[]> frequencyDists; 
	protected Color[] frequencyColors;
	protected int[] frequencyWidths;
	protected DisplayConfiguration simulatedDisplay;
	protected DisplayConfiguration clientDisplay;
	protected boolean rescaleToSimulated;
	protected HybridImageRendererSettings rendererSettings;


	public PowerSpectrumAnalyzer(HybridImageRendererSettings settings){
		rendererSettings = settings;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


  /**
   * Hides the power spectrum panel.
   */
  public void hidePowerSpectrumPanel(){
          if(window != null){
                  window.setVisible(false);
          }
  }


	/**
	 * Display the power spectrum panel. 
	 */
	public void showPowerSpectrumPanel(){
		if(window != null){
        window.setVisible(true);
        return;
    };

		//Window is a JFrame with a custom paint method
		window = new JFrame(){
			@Override
			public void paint(Graphics g){
				//check for size changes
				Dimension windowSize = window.getContentPane().getSize();
				if(powerSpectrumImage == null || 
						powerSpectrumImage.getWidth() != windowSize.width || 
						powerSpectrumImage.getHeight() != windowSize.height){ 
					powerSpectrumImage = new BufferedImage(windowSize.width, 
							windowSize.height, BufferedImage.TYPE_INT_ARGB);
				}

				//render if all info has been provided
				if(frequencyDists != null && simulatedDisplay != null && clientDisplay != null){
					double[] highlightBand = !rendererSettings.highlightFrequencyBand ? null :
							new double[]{rendererSettings.frequencyBandMidpoint - rendererSettings.frequencyBandWidth,
							rendererSettings.frequencyBandMidpoint + rendererSettings.frequencyBandWidth};
					
					ImageStatistics.drawPowerSpectrums(powerSpectrumImage, 
							frequencyDists, frequencyColors, frequencyWidths, 
							simulatedDisplay, clientDisplay, 
							rescaleToSimulated, rendererSettings.logScalePowerSpectrumY, 
							highlightBand, 0, 
							0, powerSpectrumImage.getWidth(), powerSpectrumImage.getHeight());
					g.drawImage(powerSpectrumImage, window.getInsets().left,
							window.getInsets().top,	null);
				}
			}
		};
		window.setSize(500, 500);
		window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		window.setVisible(true);
		overlayPanel = new JPanel();
		window.add(overlayPanel);
		setupMouseListeners();
	}



	protected void setupMouseListeners() {

		window.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) { }

			@Override
			public void mousePressed(MouseEvent e) { 
				//use mouse clicks to set the center point for the band-pass overlay
				double binCount = frequencyDists.get(0).length; //number of bins in frequency distribution
				double mouseX = (double)e.getX() - window.getInsets().left; 
				double clickFraction = mouseX / powerSpectrumImage.getWidth();
				double lineFrequency = Math.pow(10.0,clickFraction*Math.log10(binCount));
				double cyclesPerPixel;
				if(rescaleToSimulated){
					double cyclesPerDegree = FrequenciesAndAngles.cyclesPerPixelToCyclesPerDegree((lineFrequency / (2 * binCount)), clientDisplay);
					cyclesPerPixel = FrequenciesAndAngles.cyclesPerDegreeToCyclesPerPixel(cyclesPerDegree, simulatedDisplay);
				}else{
					cyclesPerPixel = lineFrequency / (2 * binCount);
				}
				double pixelsPerCycle = 1.0 / cyclesPerPixel;
				
				rendererSettings.frequencyBandMidpoint = (int)pixelsPerCycle;
				rendererSettings.highlightFrequencyBand = true;
				rendererSettings.settingsChangedFromCode();
			}

			@Override
			public void mouseExited(MouseEvent e) { }

			@Override
			public void mouseEntered(MouseEvent e) { }

			@Override
			public void mouseClicked(MouseEvent e) { }
		});
	}

	/**
	 * Sets up a frame to draw power spectra
	 * @param frequencyDists
	 * @param frequencyColors
	 * @param simulatedDisplay
	 * @param clientDisplay
	 * @param rescaleToSimulated
	 */
	public void drawPowerSpectrums(List<float[]> frequencyDists, 
			Color[] frequencyColors, int[] frequencyWidths,
			DisplayConfiguration simulatedDisplay, DisplayConfiguration clientDisplay, 
			boolean rescaleToSimulated){

		this.frequencyDists = frequencyDists;
		this.frequencyColors = frequencyColors;
		this.frequencyWidths = frequencyWidths;
		this.simulatedDisplay = simulatedDisplay;
		this.clientDisplay = clientDisplay;
		this.rescaleToSimulated = rescaleToSimulated;

		if(window == null) return;
		window.repaint();
	}
}
