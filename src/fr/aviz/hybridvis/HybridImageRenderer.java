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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.jhlabs.image.ContrastFilter;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.HighPassFilter;

import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.display.WILDERDisplay;
import fr.aviz.hybridvis.models.FrequenciesAndAngles;
import fr.aviz.hybridvis.utils.DisplayConfigurationReader;
import fr.aviz.hybridvis.utils.GUIUtils;
import fr.aviz.hybridvis.utils.ImageStatistics;
import fr.aviz.hybridvis.utils.ProgressMonitor;
import fr.aviz.hybridvis.utils.TransparentHighPassFilter;
import fr.aviz.hybridvis.utils.settings.Settings.SettingsListener;

/**
 * Blends two visualizations using the hybrid image method:
 * http://hal.archives-ouvertes.fr/docs/00/84/48/78/PDF/HybridImageVisualization_CameraReady.pdf
 * 
 * To use this class, extend it and implement the methods drawNearGraphics and drawFarGraphics (see javadoc below).
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized image (key 'S').
 *  
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Right-click to switch back to the overview mode.
 * - Mouse drag to pan
 * - Mouse wheel to zoom in/out
 * - Hit S to generate the full image and save it on the disk (this process may take about 10 min).
 * - TODO (Wes): document other keyboard shortcuts.
 * 
 * @author dragice
 * 
 */ 

public abstract class HybridImageRenderer extends WallRenderer {

	HybridImageRendererSettings settings = null;
  JFrame settingsFrame = null;
  PowerSpectrumAnalyzer powerSpectrumAnalyzer = null;
	
	Font waitFont = new Font("Helvetica", 0, 12);

  private static DisplayConfiguration defaultConfiguration = new WILDERDisplay(); 

  public static String SETTINGS_PROPERTY = "settings";

	public HybridImageRenderer() {
          this(defaultConfiguration);
	}
	
	/**
	 * 
	 */
	public HybridImageRenderer(DisplayConfiguration simulatedDisplay) {
		super(simulatedDisplay);

		// -- Keyboard shortcuts
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_D, this, "togglePowerSpectrum");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_V, this, "renderValueSettings");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_UP, this, "up");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_DOWN, this, "down");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_B, this, "blurRadius");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_H, this, "hipassRadius");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_C, this, "postContrast");
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_R, this, "postBrightness");

		// -- Create default settings and show control panel
		settings = new HybridImageRendererSettings();
		settings.drawBezels = drawTiles;
    addSettingsListener();
		setupSettingsControls();

		//create a power spectrum analyzer
    powerSpectrumAnalyzer = new PowerSpectrumAnalyzer(settings);
	}

  protected void setupSettingsControls(){
    if(settingsFrame != null){
      settingsFrame.setVisible(false);
      settingsFrame.dispose();
    }
    settingsFrame = settings.makeControlFrame();
  }

  /**
   * Sets the project settings for this renderer.
   */
  public void setSettings(HybridImageRendererSettings settings){
    this.settings = settings;
    addSettingsListener();
    firePropertyChange(SETTINGS_PROPERTY, null, settings);
  }

  protected void addSettingsListener(){
  		settings.addListener(new SettingsListener() {
			@Override
			public void settingsChangedFromControlPanel() {
				invalidateWallImage(false);
				drawTiles = settings.drawBezels;
				repaint();
			}
			@Override
			public void settingsChangedFromCode() {
				invalidateWallImage(false);
				drawTiles = settings.drawBezels;
				repaint();
			}
		});
  }

	/////// Abstract methods to implement

	// non abstract for backward-compatibility
	public void drawBackgroundGraphics(Graphics2D g) {
		g.setColor(Color.white);
		g.fillRect(0, 0, simulatedDisplay.getXResolution(), simulatedDisplay.getYResolution());
	}
	
	/**
	 * Draws the visual content that will be visible when the user is close to the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * The high-pass filter requires an opaque image, so make sure you first fill the background with an opaque color.
	 */
	public abstract void drawNearGraphics(Graphics2D g);

	/**
	 * Draws the visual content that will be visible when the user is far from the display.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * For best results, first fill the background with an opaque color. 
	 */
	public abstract void drawFarGraphics(Graphics2D g);

	///////// Hybrid image rendering parameters

	public void setDrawBackground(boolean draw) {
		settings.drawBackground = draw;
		settings.fireSettingsChangedFromCode();
	}
	
	/**
	 * Returns the opacity with which the near image is drawn on top of the background image, between 0 and 1.
	 * The default value is 1. 
	 */
	public double getNearImageOpacity() {
		return settings.nearImageOpacity;
	}

	/**
	 * Sets the opacity with which the near image is drawn on top of the background image, between 0 and 1.
	 * The default value is 1.0. 
	 */
	public void setNearImageOpacity(double opacity) {
		settings.nearImageOpacity = opacity;
		settings.fireSettingsChangedFromCode();
	}
	
	public void setDrawNearImage(boolean visible) {
		settings.drawNearImage = visible;
		settings.fireSettingsChangedFromCode();
	}
	
	/**
	 * Returns the radius of the high-pass function applied on the near image, in pixels in wall display coordinates.
	 * Returns 0 if high-pass filtering is disabled. The default value is 30 pixels.
	 */
	public double getHipassRadius() {
		return settings.hipassRadius;
	}

	/**
	 * Sets the radius of the high-pass function applied to the near image, in pixels in wall display coordinates.
	 * Setting the value to 0 disables high-pass filtering. The default value is 30 pixels.
	 */
	public void setHipassRadius(double hipassRadius) {
		settings.hipassRadius = (int)hipassRadius;
		settings.fireSettingsChangedFromCode();
	}

	public void setTransparentHipass(boolean transp) {
		settings.transparentHipass = transp;
	}
	
	/**
	 * Returns the amount of contrast enhancement applied to the near image after high-pass filtering.
	 * A value of 1 means the contrast is left unchanged. The default value is 1.5.
	 * No contrast enhancement is applied if high-pass filtering is disabled.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public double getHipassContrast() {
		return settings.hipassContrast;
	}

	/**
	 * Sets the amount of contrast enhancement to be applied to the near image after high-pass filtering.
	 * Setting the value to 1 leaves the contrast unchanged.  The default value is 1.5.
	 * No contrast enhancement is applied if high-pass filtering is disabled.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public void setHipassContrast(double hipassContrast) {
		settings.hipassContrast = hipassContrast;
		settings.fireSettingsChangedFromCode();
	}

	/**
	 * Returns the amount of brightness enhancement applied to the near image after high-pass filtering.
	 * A value of 1 means brightness is left unchanged.  The default value is 1.5.
	 * No brightness enhancement is applied if high-pass filtering is disabled.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public double getHipassBrightness() {
		return settings.hipassBrightness;
	}

	/**
	 * Sets the amount of brightness enhancement to be applied to the near image after high-pass filtering.
	 * A value of 1 leaves the brightness unchanged.  The default value is 1.5.
	 * No brightness enhancement is applied if high-pass filtering is disabled.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public void setHipassBrightness(double hipassBrightness) {
		settings.hipassBrightness = hipassBrightness;
		settings.fireSettingsChangedFromCode();
	}

	public void setDrawFarImage(boolean visible) {
		settings.drawFarImage = visible;
		settings.fireSettingsChangedFromCode();
	}
	
	/**
	 * Returns the radius of the low-pass (Gaussian blur) function applied to the far image, in pixels in wall display coordinates.
	 * A value of 0 means low-pass filtering is disabled. The default value is 30 pixels.
	 */
	public double getBlurRadius() {
		return settings.blurRadius;
	}

	/**
	 * Sets the radius of the low-pass (Gaussian blur) function to be applied to the far image, in pixels in wall display coordinates.
	 * Setting the value to 0 disables low-pass filtering. The default value is 30 pixels.
	 */
	public void setBlurRadius(double blurRadius) {
		settings.blurRadius = (int)blurRadius;
		settings.fireSettingsChangedFromCode();
	}

	/**
	 * Returns the opacity with which the far image is drawn on top of the near image, between 0 and 1.
	 * The default value is 0.5. 
	 */
	public double getFarImageOpacity() {
		return settings.farImageOpacity;
	}

	/**
	 * Sets the opacity with which the far image is drawn on top of the near image, between 0 and 1.
	 * The default value is 0.5. 
	 */
	public void setFarImageOpacity(double blurOpacity) {
		settings.farImageOpacity = blurOpacity;
		settings.fireSettingsChangedFromCode();
	}

	/**
	 * Returns the amount of final contrast enhancement applied after the hybrid image has been rendered.
	 * A value of 1 means the contrast is left unchanged. The default value is 2.0.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public double getPostContrast() {
		return settings.postContrast;
	}

	/**
	 * Sets the amount of final contrast enhancement to apply after the hybrid image has been rendered.
	 * Setting the value to 1 leaves contrast unchanged. The default value is 2.0.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public void setPostContrast(double postContrast) {
		settings.postContrast = postContrast;
		settings.fireSettingsChangedFromCode();
	}

	/**
	 * Returns the amount of final brightness enhancement applied after the hybrid image has been rendered.
	 * A value of 1 means the brightness is left unchanged. The default value is 1.77.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public double getPostBrightness() {
		return settings.postBrightness;
	}

	/**
	 * Sets the amount of final brightness enhancement to apply after the hybrid image has been rendered.
	 * Setting the value to 1 leaves brightness unchanged. The default value is 1.77.
	 * 
	 * Note: The function applied to color channels is f = f*brightness; f = (f-0.5f)*contrast+0.5f;
	 */
	public void setPostBrightness(double postBrightness) {
		settings.postBrightness = postBrightness;
		settings.fireSettingsChangedFromCode();
	}
	

	public void setDrawBezels(boolean draw) {
		settings.drawBezels = draw;
		settings.fireSettingsChangedFromCode();
	}

	public void setDrawPowerSpectrum(boolean draw) {
		settings.drawPowerSpectrum = draw;
		settings.fireSettingsChangedFromCode();
	}
	
	public void setDrawSettings(boolean draw) {
		settings.drawSettings = draw;
		settings.fireSettingsChangedFromCode();
	}

	/**
	 * Draws the visual content to be shown while the hybrid image is being computed, i.e., after a zoom-in or
	 * zoom-out operation.
	 */
	@Override
	public void drawPreview(Graphics2D g) {
		if (windowBuffer == null)
			return;
		g.drawImage(windowBuffer, (int)windowBoundsOnWall.getMinX(), (int)windowBoundsOnWall.getMinY(), (int)windowBoundsOnWall.getWidth(), (int)windowBoundsOnWall.getHeight(), null);
		// switch back to component coordinates
		g.setTransform(AffineTransform.getTranslateInstance(getX(), getY()));
		g.setFont(waitFont);
		g.setColor(new Color(0f, 0f, 0f, 0.6f));
		GUIUtils.drawText(g, "Rendering...", getWidth() - 6, getHeight() - 6, Double.MAX_VALUE, Double.MAX_VALUE, GUIUtils.HALIGN.Right, GUIUtils.VALIGN.Bottom, 0, new Color(1f, 1f, 1f, 0.2f), new Color(1f, 1f, 1f, 0.5f), 4);
	}

	/**
	 * Not used anymore by renderWallImage() in this subclass.
	 */
	@Override
	public void drawFinal(Graphics2D g) {

	}

	/////////////////////////////////////////////////////////

  /**
   * Renders a hybrid image
   * @param tmpNear - temporary image (for rendering the near image)
   * @param tmpFar - temporary image (for rendering the far image)
   * @param dst - destination
   * @param bounds
   * @param pm - progress manager
   */
  @Override
  protected void renderHybridImage(BufferedImage tmpNear, BufferedImage tmpFar, BufferedImage tmpFinal, Graphics2D dst, Rectangle2D bounds, ProgressMonitor pm, List<float[]> frequencyDists){
    final Graphics2D gNear = (Graphics2D)tmpNear.getGraphics();
    final Graphics2D gFar = (Graphics2D)tmpFar.getGraphics();
    final Graphics2D gFinal = (Graphics2D)tmpFinal.getGraphics();
    setDefaultRenderingHints(gNear);
    setDefaultRenderingHints(gFar);
    setDefaultRenderingHints(gFinal);
    setDefaultRenderingHints(dst);
    final double scale = bounds.getWidth() / getWallWidth();

    pm.setProgress(100);
    if(settings.drawNearImage){
      pm.setNote("Rendering near image");
      drawNearGraphics(gNear, bounds);
      if(settings.drawPowerSpectrum){
			  frequencyDists.add(ImageStatistics.get1dFrequencies(tmpNear));
      }
      if(settings.hipassRadius > 0){ 
        final HighPassFilter hipass = settings.transparentHipass ? new TransparentHighPassFilter() : new HighPassFilter();
			  hipass.setRadius((float)(getHipassRadius() * scale));
			  hipass.filter(tmpNear, tmpNear);
      }
      if(pm.isCanceled()){
        return;
      }
    	final ContrastFilter contrast = new ContrastFilter();
			contrast.setContrast((float)getHipassContrast());
			contrast.setBrightness((float)getHipassBrightness());
			contrast.filter(tmpNear, tmpNear);
      if(settings.drawPowerSpectrum){
			  frequencyDists.add(ImageStatistics.get1dFrequencies(tmpNear));
      }
    }

    if(pm.isCanceled()){
      return;
    }
    pm.setProgress(200);

    if(settings.drawFarImage){
      pm.setNote("Rendering far image");
			drawFarGraphics(gFar, bounds);
      if(settings.drawPowerSpectrum){
        frequencyDists.add(ImageStatistics.get1dFrequencies(tmpFar));
      }
      if(pm.isCanceled()){
        return;
      }
      if(getBlurRadius() > 0){
     	  BufferedImageOp blur = new GaussianFilter((float)(getBlurRadius() * scale));
			  blur.filter(tmpFar, tmpFar);
      }
      if(settings.drawPowerSpectrum){
			  frequencyDists.add(ImageStatistics.get1dFrequencies(tmpFar));
      }
    }

    if(pm.isCanceled()){
      return;
    }
    pm.setProgress(400);

    if(settings.drawBackground){
			drawBackgroundGraphics(gFinal, bounds);
    }

    pm.setNote("Compositing images");
    if(settings.drawNearImage){
    	gFinal.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)settings.nearImageOpacity));
  		gFinal.drawImage(tmpNear, 0, 0, null);
    }

    if(settings.drawFarImage){
			gFinal.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)settings.farImageOpacity));
			gFinal.drawImage(tmpFar, 0, 0, null);
    }

  	final ContrastFilter contrast2 = new ContrastFilter();
		contrast2.setContrast((float)getPostContrast());
		contrast2.setBrightness((float)getPostBrightness());
		contrast2.filter(tmpFinal, tmpFinal);

    if(settings.drawPowerSpectrum){
			frequencyDists.add(ImageStatistics.get1dFrequencies(tmpFinal));
    }

    if(pm.isCanceled()){
      return;
    }
    pm.setProgress(800);

    if(settings.drawSettings){
      drawSettings(gFinal);
    }

    dst.drawImage(tmpFinal, 0, 0, null);
  }

  protected void drawSettings(Graphics2D g){
    //drawing some output on the rendering values used
		String[] labels = new String[]{"hipassRadius","hipassContrast","hipassBrightness","blurRadius","blurOpacity","postContrast","postBrightness"};
		double[] values = new double[]{getHipassRadius(), getHipassContrast(), getHipassBrightness(), getBlurRadius(), getFarImageOpacity(), getPostContrast(), getPostBrightness()};
		Font f = new Font("SansSerif",Font.PLAIN,14);
		g.setFont(f);
		FontMetrics m = g.getFontMetrics(f);
		int fontHeight = m.getHeight();

		String referenceLabel = "hipassBrightness: xxx.xxx";

		g.setColor(Color.DARK_GRAY);
		int boxHeight = fontHeight * values.length + 10;
		g.fillRect(0, 0, m.stringWidth(referenceLabel) + 20, boxHeight); 

		g.setColor(Color.WHITE);
		int position = boxHeight - 5; 
		for(int i = 0; i < labels.length; ++i){
			g.drawString(labels[i] + ": " +values[i], 10, position);
			position -= fontHeight;
		}
  }

  @Override
  protected void drawPowerSpectrum(List<float[]> frequencyDists){
    if(settings.drawPowerSpectrum){
      final DisplayConfiguration clientDisplay = getClientDisplay();
      powerSpectrumAnalyzer.showPowerSpectrumPanel();
      powerSpectrumAnalyzer.drawPowerSpectrums(frequencyDists,
        new Color[]{
	        Color.getHSBColor(0.0f, 0.4f, 1f), //Faint red - unprocessed near
          Color.getHSBColor(0.0f, 1.0f, 0.85f), //Stronger red - processed near
          Color.getHSBColor(0.6f, 0.4f, 1f), //Faint blue - unprocessed far 
          Color.getHSBColor(0.6f, 1.0f, 0.85f),	//Stronger blue - processed far
          Color.black},	//Black - composite
        new int[]{1,1,1,1,2},
        getSimulatedDisplay(), clientDisplay,
        !((clientDisplay.getYResolution() == simulatedDisplay.getYResolution()) && (clientDisplay.getXResolution() == simulatedDisplay.getXResolution()))); //a shortcut to tell if we're rendering the full-size image 
    } else {
      powerSpectrumAnalyzer.hidePowerSpectrumPanel();
    }
  }

	/**
	 * Renders the hybrid image.
	 */
  //XXX implement logic in renderHybridImage, then delete this method
//	@Override
//	protected void renderWallImage() {
//
//		// -- If desired, draw a band-passed version of the image
//		if(settings.highlightFrequencyBand){
//		 
//		  //shift the actual bounds of the band pass to approximate the pass on the actual display.
//		  double bandMax = settings.frequencyBandMidpoint + settings.frequencyBandWidth;
//		  double bandMin = settings.frequencyBandMidpoint - settings.frequencyBandWidth;
//		  bandMax = 1f/FrequenciesAndAngles.getEquivalentCyclesPerPixel(1f/bandMax, simulatedDisplay, clientDisplay);
//		  bandMin = 1f/FrequenciesAndAngles.getEquivalentCyclesPerPixel(1f/bandMin, simulatedDisplay, clientDisplay);
//	      
//		  //get a band-passed copy of the final image and overlay it on top of the finished one
//		  BufferedImage wallImage = tmpWallImage.getSubimage(
//				  Math.max(0,(int)wallWinBounds.x), Math.max(0,(int)wallWinBounds.y),
//				  (int)(Math.min(tmpWallImage.getWidth(), wallWinBounds.width + wallWinBounds.x) - Math.max(0, wallWinBounds.x)),
//				  (int)(Math.min(tmpWallImage.getHeight(), wallWinBounds.height + wallWinBounds.y) - Math.max(0, wallWinBounds.y)));
//		  BufferedImage bandPassOverlay = ImageStatistics.getBandPassedOverlay(wallImage, bandMin, bandMax);
//      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
//      g.drawImage(bandPassOverlay, Math.max(0,(int)wallWinBounds.x), Math.max(0,(int)wallWinBounds.y), null);
//      if (renderingInterrupted()) return; // interrupt if need to re-render the image again with different parameters
//		}
//		
//	}

  /**
   * @param bounds - 
   */
	protected void drawBackgroundGraphics(Graphics2D g, Rectangle2D bounds) {
		AffineTransform at0 = g.getTransform();
		Shape oldClip = g.getClip();
		//Rectangle2D newClip = bounds.createIntersection(new Rectangle(0, 0, canvasWidth, canvasHeight));
		g.clip(bounds);
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth() / getWallWidth(), bounds.getHeight() / getWallHeight());
		drawBackgroundGraphics(g);		
		g.setTransform(at0);
		g.setClip(oldClip);
	}

	
	/**
	 * Draws the visual content that will be visible when the user is close to the display.
	 * 
	 * The entire content of the wall-size display is translated and rescaled to fit the bounds provided.
	 * When the image is being saved on the disk, these bounds will be located at 0,0 and its
	 * dimensions will be equal to the wall-size display's dimensions in pixels. While the image is being previewed
	 * the computer screen, the bounds will be translated and scaled down.
   * @param bounds - rendered wall bounds in graphics (g) coordinates.
	 */
	protected void drawNearGraphics(Graphics2D g, Rectangle2D bounds) {
		AffineTransform at0 = g.getTransform();
		Shape oldClip = g.getClip();
	//	Rectangle2D newClip = bounds.createIntersection(new Rectangle(0, 0, canvasWidth, canvasHeight));
		g.clip(bounds);
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth() / getWallWidth(), bounds.getHeight() / getWallHeight());
		drawNearGraphics(g);		
		g.setTransform(at0);
		g.setClip(oldClip);
	}

	/**
	 * Draws the visual content that will be visible when the user is far from the display.
	 * 
	 * The entire content of the wall-size display is translated and rescaled to fit the bounds provided.
	 * When the image is being saved on the disk, these bounds will be located at 0,0 and its
	 * dimensions will be equal to the wall-size display's dimensions in pixels. While the image is being previewed
	 * the computer screen, the bounds will be translated and scaled down.
	 */
	protected void drawFarGraphics(Graphics2D g, Rectangle2D bounds) {
		AffineTransform at0 = g.getTransform();
		Shape oldClip = g.getClip();
	//	Rectangle2D newClip = bounds.createIntersection(new Rectangle(0, 0, canvasWidth, canvasHeight)); 
		g.clip(bounds);
		g.translate(bounds.getX(), bounds.getY());
		g.scale(bounds.getWidth() / getWallWidth(), bounds.getHeight() / getWallHeight());
		drawFarGraphics(g);
		g.setTransform(at0);
		g.setClip(oldClip);
	}

	private String currentSetting = "blurRadius";

	@Override 
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if(e.getActionCommand().equals("togglePowerSpectrum")){
			settings.drawPowerSpectrum = !settings.drawPowerSpectrum;
			settings.fireSettingsChangedFromCode();
			invalidateWallImage(false);
			repaint();
		}
		else if(e.getActionCommand().equals("renderValueSettings")) {
			settings.drawSettings = !settings.drawSettings;
			settings.fireSettingsChangedFromCode();
			invalidateWallImage(false);
			repaint();
		}
		else if(e.getActionCommand().equals("blurRadius")) currentSetting = "blurRadius";
		else if(e.getActionCommand().equals("hipassRadius")) currentSetting = "hipassRadius";
		else if(e.getActionCommand().equals("postBrightness")) currentSetting = "postBrightness";
		else if(e.getActionCommand().equals("postContrast")) currentSetting = "postContrast";
		else if(e.getActionCommand().equals("up") || e.getActionCommand().equals("down")){
			double upOrDown = e.getActionCommand().equals("up") ? 1 : -1;
			if(currentSetting == "blurRadius"){
				setBlurRadius(Math.max(0, getBlurRadius() + upOrDown*5));
				System.out.println("blurRadius=" + getBlurRadius());
			}
			else if(currentSetting == "hipassRadius"){
				setHipassRadius(Math.max(0, getHipassRadius() + upOrDown));
				System.out.println("hipassRadius=" + getHipassRadius());
			}
			else if(currentSetting == "postBrightness"){
				setPostBrightness(Math.max(0, getPostBrightness() + upOrDown * 0.01));
				System.out.println("postBrightness=" + getPostBrightness());
			}
			else if(currentSetting == "postContrast"){
				setPostContrast(Math.max(0, getPostContrast() + upOrDown * 0.01));
				System.out.println("postContrast=" + getPostContrast());
			}
			invalidateWallImage(false);
			repaint();
		}  
  }
}
