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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.MouseInputListener;

import fr.aviz.hybridvis.display.ClientWindowConfiguration;
import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.display.WILDDisplay;
import fr.aviz.hybridvis.display.WILDERDisplay;
import fr.aviz.hybridvis.utils.GUIUtils;
import fr.aviz.hybridvis.utils.MathUtils;
import fr.aviz.hybridvis.utils.NoOpProgressMonitor;
import fr.aviz.hybridvis.utils.ProgressMonitor;
import fr.aviz.hybridvis.utils.SwingProgressMonitor;
import fr.aviz.hybridvis.utils.settings.Settings.SettingsListener;

/**
 * This class implements interruptible multithreaded rendering of wall-display graphics, quick preview,
 * pan and zoom interactions and screen capture.
 * 
 * Running this class:
 * - Use at least -Xmx512m for the preview mode and -Xmx4096m if you need to generate a wall-sized
 * image (key 'S').
 *  
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Right-click to switch back to the overview mode.
 * - Mouse drag to pan
 * - Mouse wheel to zoom in/out
 * - Hit S to generate the full image and save it on the disk (this process will take about 10 min).
 * 
 * @author dragice
 * 
 */

public abstract class WallRenderer extends JComponent implements ActionListener, MouseInputListener, MouseWheelListener {

	DisplayConfiguration simulatedDisplay;
  public static final String SIMULATED_DISPLAY_PROPERTY = "simulatedDisplay";

	ClientWindowConfiguration clientDisplay = new ClientWindowConfiguration(this, true);
	
	//PETRA ADDED - Bezel sizes from Olivier
	// FIXME: move those fields to DisplayConfiguration and initialize them in WILDDisplay
    protected int bezel_left = 92;
    protected int bezel_right = 92;
    protected int bezel_top = 118;
    protected int bezel_bottom = 96;
    protected int tileWidthMinusBezel, tileHeightMinusBezel;

	//////// Local variables

	protected boolean drawTiles = true;
	protected Rectangle2D.Double wallWinBounds = new Rectangle2D.Double();
	protected boolean autofitMode = true;
	protected volatile BufferedImage windowBuffer = null;
	protected volatile boolean wallImageRendered = false;
  // Preview window bounds, in wall coordinates
	Rectangle2D.Double windowBoundsOnWall = new Rectangle2D.Double();
	protected volatile boolean scheduleWallImageRendering = false;
	protected Thread renderThread = null;
	Rectangle2D.Double tmpRect = new Rectangle2D.Double();
	Point lastMousePos = null;
	boolean simulatedDisplayListenerEnabled = false;

  /**
   * Instantiates a WallRenderer using a default simulated display.
   */
	public WallRenderer() {
		this(new WILDERDisplay());
	}
	
	/**
	 * 
	 */
	public WallRenderer(DisplayConfiguration simulatedDisplay_) {
		super();
		
		this.simulatedDisplay = simulatedDisplay_;
		
		tileWidthMinusBezel = simulatedDisplay.getTileXResolution() - bezel_left - bezel_right;
		tileHeightMinusBezel = simulatedDisplay.getTileYResolution() - bezel_top - bezel_bottom;
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				if (autofitMode) {
					fitWallToDimensions(getSize());
					invalidateWallImage(false);
					updateViewerDistance();
					repaint();
				}
			}
		});

    addPropertyChangeListener(SIMULATED_DISPLAY_PROPERTY, new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent evt){
        fitWallToDimensions(getSize());
        updateViewerDistance();
        invalidateWallImage(false);
        repaint();
      }
    });
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

    addDisplaySettingsListener();
		
		GUIUtils.addGlobalKeyListener(KeyEvent.VK_S, this, "save");
	}

  protected void addDisplaySettingsListener(){
		simulatedDisplay.addListener(new SettingsListener() {
			@Override
			public void settingsChangedFromControlPanel() {
				if (simulatedDisplayListenerEnabled) {
					double xangleRadians = simulatedDisplay.getHorizontalVisualAngleRadians();
					double yangleRadians = simulatedDisplay.getVerticalVisualAngleRadians();
					double w = clientDisplay.getXPixelsForVisualAngleRadians(xangleRadians);
					double h = clientDisplay.getYPixelsForVisualAngleRadians(yangleRadians);
					zoomToSize(w, h);
					autofitMode = false;
					invalidateWallImage(false);
					repaint();
				}
			}
			
			@Override
			public void settingsChangedFromCode() {
			}
		});
  }
	
	/////// Abstract methods to implement
	
	/**
	 * Draws the visual content to be shown while the high-res content is being rendered, i.e., after a zoom-in or
	 * zoom-out operation.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 */
	public abstract void drawPreview(Graphics2D g);

	/**
	 * Draws the final (hi-res) visual content.
	 * 
	 * The coordinates system is that of the wall-sized display:
	 * - The top left corner of the drawing canvas is at (0, 0),
	 * - The bottom right corner of the drawing canvas is (getWallWidth(), getWallHeight()),
	 * - A drawing unit is exactly 1 pixel.
	 * 
	 * This function must halt the rendering and return as quickly as possible when renderingInterrupted() returns true.
	 */
	public abstract void drawFinal(Graphics2D g);

	
	/////// Wall display parameters

	/**
	 * Returns the number of horizontal tiles (screens) on the wall-display.
	 * @return
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getTileCountX() {
		return simulatedDisplay.getXTiles();
	}

	/**
	 * Returns the number of vertical tiles (screens) on the wall-display.
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getTileCountY() {
		return simulatedDisplay.getYTiles();
	}

	/**
	 * Returns the width of a tile (screen) on the wall-display, in pixels.
	 * The default value is that of the WILD display.
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getTileWidth() {
		return simulatedDisplay.getTileXResolution();
	}

	/**
	 * Returns the height of a tile (screen) on the wall-display, in pixels.
	 * The default value is that of the WILD display.
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getTileHeight() {
		return simulatedDisplay.getTileYResolution();
	}

	/**
	 * Returns the total width of the wall-sized display in pixels, i.e., the number of horizontal
	 * tiles multiplied by tile width.
	 * The default value is that of the WILD display.
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getWallWidth() {
		return simulatedDisplay.getXResolution();
	}

	/**
	 * Returns the total width of the wall-sized display in pixels, i.e., the number of horizontal
	 * tiles multiplied by tile width.
	 * The default value is that of the WILD display.
	 * @deprecated Replaced by getSimulatedDisplay()
	 */
	@Deprecated
	public int getWallHeight() {
		return simulatedDisplay.getYResolution();
	}
	
	/////////////////////////////////////////////////////////
	
	/**
	 * Fit the wall-size display content to the canvas.
   * @param dim - target dimensions
	 */
	protected Rectangle2D fitWallToDimensions(final Dimension dim) {
		final int wallWidth = simulatedDisplay.getXResolution();
		final int wallHeight = simulatedDisplay.getYResolution();
		double scalex = dim.getWidth() / (double)wallWidth;
		double scaley = dim.getHeight() / (double)wallHeight;
		if (scalex < scaley){
			wallWinBounds.setRect(0, (dim.getHeight() - wallHeight*scalex) / 2, dim.getWidth(), wallHeight*scalex);
		} else { 
			wallWinBounds.setRect((dim.getWidth() - wallWidth*scaley) / 2, 0, wallWidth*scaley, dim.getHeight());
    }
    return wallWinBounds;
	}
	
	/**
	 * Zoom inside the wall-size display content.
	 */
	protected void zoomToPixelSize(int wallx, int wally, int winx, int winy) {
		wallWinBounds.setRect(winx - wallx, winy - wally, simulatedDisplay.getXResolution(), simulatedDisplay.getYResolution());
	}

	protected void zoomToSize(double newwidth, double newheight) {
		zoomToSize(newwidth, newheight, getWidth()/2, getHeight()/2);
	}
	
	protected void zoomToSize(double newwidth, double newheight, int winanchorx, int winanchory) {
		double wallCenterX = xWinToWall(winanchorx);
		double wallCenterY = yWinToWall(winanchory);
		wallWinBounds.setRect(0, 0, newwidth, newheight);
		wallWinBounds.setRect(winanchorx - xWallToWin(wallCenterX), winanchory - yWallToWin(wallCenterY), newwidth, newheight); 
	}
	
	protected void pan(int windx, int windy) {
		wallWinBounds.setRect(wallWinBounds.x + windx, wallWinBounds.y + windy, wallWinBounds.width, wallWinBounds.height);
	}
	
	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double xWallToWin(double x) {
		return x * wallWinBounds.width / simulatedDisplay.getXResolution() + wallWinBounds.x;
	}

	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double wWallToWin(double w) {
		return w * wallWinBounds.width / simulatedDisplay.getXResolution();
	}

	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double yWallToWin(double y) {
		return y * wallWinBounds.height / simulatedDisplay.getYResolution() + wallWinBounds.y;
	}

	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double hWallToWin(double h) {
		return h * wallWinBounds.height / simulatedDisplay.getYResolution();
	}
	
	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double xWinToWall(double x) {
		return (x - wallWinBounds.x) / wallWinBounds.width * simulatedDisplay.getXResolution();
	}

	/**
	 * Conversion of coordinates and sizes between the wall-size display coordinates systems and the preview
	 * window coordinates system. When rendering during screen capture, the coordinates are the same.
	 */
	protected double yWinToWall(double y) {
		return (y - wallWinBounds.y) / wallWinBounds.height * simulatedDisplay.getYResolution();
	}
	
	/**
	 * Avoid subclassing this method. Use the drawing helper methods instead.
	 */
	@Override
	public void paint(Graphics g_) {
		
		Graphics2D g = (Graphics2D)g_;

		// -- Draw dark background around the wall
		int w = getWidth();
		int h = getHeight();
		g.setColor(Color.darkGray);
		g.fillRect(0,  0, w, h);
		
		// -- Draw checker board (for transparent graphics)
		GUIUtils.drawCheckerBoard(g, wallWinBounds.createIntersection(new Rectangle(0, 0, w, h)), 8, Color.white, new Color(204, 204, 204));

		// -- Draw preview or rendered image
		if (!wallImageRendered) {
			drawPreview(g, wallWinBounds);
		} else {
			g.drawImage(windowBuffer, 0, 0, null);
		}
			
		// -- Tiles
		if (drawTiles) {
			float scale = (float)(wallWinBounds.width / simulatedDisplay.getXResolution());
			g.setColor(Color.lightGray);
			g.setStroke(new BasicStroke(80f * scale));
			paintTiles(g);
			g.setColor(Color.gray);
			g.setStroke(new BasicStroke(60f * scale));
			paintTiles(g);
		}
		
		simulatedDisplayListenerEnabled = true;
	}
		
	protected void paintTiles(Graphics2D g) {
		double tw = wallWinBounds.width / simulatedDisplay.getXTiles();
		double th = wallWinBounds.height / simulatedDisplay.getYTiles();
		for (int x=0; x<simulatedDisplay.getXTiles(); x++) {
			for (int y=0; y<simulatedDisplay.getYTiles(); y++) {
				tmpRect.setRect(wallWinBounds.x + x * tw, wallWinBounds.y + y * th, tw, th);
				g.draw(tmpRect);
			}
		}	
	}
	
	/**
	 * Draws the visual content to be shown while the hi-res image is being computed, i.e., after a zoom-in or
	 * zoom-out operation.
	 * 
	 * The entire content of the wall-size display is translated and rescaled to fit the bounds provided.
	 * When the image is being saved on the disk, these bounds will be located at 0,0 and its
	 * dimensions will be equal to the wall-size display's dimensions in pixels. While the image is being previewed
	 * the computer screen, the bounds will be translated and scaled down.
	 *
	 */
	protected void drawPreview(Graphics2D g, Rectangle2D.Double bounds) {
		AffineTransform at0 = g.getTransform();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width / simulatedDisplay.getXResolution(), bounds.height / simulatedDisplay.getYResolution());
		drawPreview(g);
		g.setTransform(at0);
	}

	/**
	 * Schedules the recomputation of the wall image.
	 */
	protected void invalidateWallImage(boolean clearImage) {
		
		// Already rendering?
		if (renderThread != null) {
			scheduleWallImageRendering = true;
			return;
		}
		
		if (getWidth() <= 0 || getHeight() <= 0) {
			return;
		}
		
		// Re-render
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		wallImageRendered = false;
		renderThread = new Thread(new Runnable() {
			@Override
			public void run() {
        BufferedImage tmpNear = null;
        BufferedImage tmpFar = null;
        BufferedImage tmpFinal = null;
        List<float[]> frequencyDists = null;
        
				do {
					scheduleWallImageRendering = false;
          tmpNear = resetImage(tmpNear);
          tmpFar = resetImage(tmpFar);
          tmpFinal = resetImage(tmpFinal);
          frequencyDists = new ArrayList<float[]>();
          renderHybridImage(tmpNear, tmpFar, tmpFinal, (Graphics2D)tmpFinal.getGraphics(), wallWinBounds, new NoOpProgressMonitor(), frequencyDists);
				} while(scheduleWallImageRendering);
				windowBoundsOnWall.setFrame(xWinToWall(0), yWinToWall(0), xWinToWall(getWidth()) - xWinToWall(0), yWinToWall(getHeight()) - yWinToWall(0));
				windowBoundsOnWall.setFrame(xWinToWall(0), yWinToWall(0), xWinToWall(getWidth()) - xWinToWall(0), yWinToWall(getHeight()) - yWinToWall(0));
        windowBuffer = tmpFinal; 
				wallImageRendered = true;
        // Draw power spectrum if required.
        drawPowerSpectrum(frequencyDists);
				repaint();
				renderThread = null;
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
    renderThread.setDaemon(true);
		renderThread.start();
	}
	
	protected void updateViewerDistance() {
		if (clientDisplay.isInitialized()) {
			clientDisplay.setResolution((int)wallWinBounds.width, (int)wallWinBounds.height);
			double angleRadians = clientDisplay.getHorizontalVisualAngleRadians();
			double distance = MathUtils.round(simulatedDisplay.getDistanceForHorizontalVisualAngleRadians(angleRadians), 2);
			simulatedDisplay.setViewerDistance(distance);
		}
	}
	
	/**
	 * Returns whether the rendering of the hi-res image needs to be interrupted.
	 * @return
	 */
	public boolean renderingInterrupted() {
		return scheduleWallImageRendering;
	}

  // This should be abstract; we leave an empty implementation for now
  protected void renderHybridImage(BufferedImage tmpNear, BufferedImage tmpFar, BufferedImage tmpFinal, Graphics2D dst, Rectangle2D bounds, ProgressMonitor pm, List<float[]> frequencyDists){
    throw new Error("pseudo-abstract method");
  }

  protected void drawPowerSpectrum(List<float[]> frequencyDists){
    //default implementation (no-op)
  }

  /**
   * Generates and saves a hybrid image.
   * If a project folder exists, attempts to save the image in the 'wall-images' subdirectory.
   * Otherwise, prompts for a destination folder.
   */
  protected void saveHybridImageAsyncInteractive(File projectPath){
    File dstFolder = null;
    if((projectPath != null) && projectPath.exists()){
      Path projectDir = projectPath.toPath().getParent();
      dstFolder = projectDir.resolve("wall-images").toFile();
    }
    if(dstFolder == null || !dstFolder.exists() || !dstFolder.isDirectory()){
      final JFileChooser jfc = new JFileChooser(new File("."));
      jfc.setDialogTitle("Choose output directory");
      jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
        dstFolder = jfc.getSelectedFile();
      } else {
        return;
      }
    }
    saveHybridImageAsync(dstFolder);
  }

  /**
   * Asynchronously renders and saves a hybrid image.
   */
  protected void saveHybridImageAsync(final File dstFolder){
    final SwingProgressMonitor pm = new SwingProgressMonitor(this, "Saving image", "Generating hybrid image", 0, 1000);
    pm.setMillisToDecideToPopup(100);
    pm.setMillisToPopup(200);
    new SwingWorker<Void, Void>(){
      protected Void doInBackground(){
        pm.setProgress(0);
        final int canvasWidth = simulatedDisplay.getXResolution();
        final int canvasHeight = simulatedDisplay.getYResolution();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0,0,canvasWidth,canvasHeight);
        final BufferedImage tmpNear = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage tmpFar = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage tmpFinal = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);

        renderHybridImage(tmpNear, tmpFar, tmpFinal, (Graphics2D)tmpFinal.getGraphics(), bounds, pm, new ArrayList<float[]>()); 
        if(pm.isCanceled()){
          return null;
        }

        try{
          pm.setNote("Writing to disk");
				  String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
				  ImageIO.write(tmpFinal, "png", new File(dstFolder, timeStamp+"_"+getClass().getName()+ ".png"));
        } catch(Exception ex){
          ex.printStackTrace();
        }

        pm.close();

        return null;
      }
    }.execute();
  }

  protected void setDefaultRenderingHints(Graphics2D g){
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  }

	protected BufferedImage resetImage(BufferedImage img) {
		if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
			img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		} else {
			makeImageTransparent(img);
		}
		return img;
	}
	
	protected static void makeImageTransparent(BufferedImage im) {
		Graphics2D g2 = (Graphics2D)im.getGraphics();
		Composite oldComposite = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
		Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, im.getWidth(), im.getHeight()); 
		g2.fill(rect);
		g2.setComposite(oldComposite);
	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("save")) {
      //saveHybridImageAsync();
		}
	}
	
	/**
	 * Make this renderer visible on the screen.
	 */
	public void showOnScreen() {
		JFrame win = new JFrame();
		
		JPanel distanceSlider = getSimulatedDisplay().getControlPanel();
		win.getContentPane().add(distanceSlider, BorderLayout.NORTH);
		win.getContentPane().add(this, BorderLayout.CENTER);
		
		GUIUtils.centerOnPrimaryScreen(win, 1000, 600);
		win.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		win.setVisible(true);
		
	}

	public DisplayConfiguration getSimulatedDisplay() {
		return simulatedDisplay;
	}
	
	public void setSimulatedDisplay(DisplayConfiguration display) {
		this.simulatedDisplay = display;
    addDisplaySettingsListener();
    firePropertyChange(SIMULATED_DISPLAY_PROPERTY, null, display);      
	}

	public DisplayConfiguration getClientDisplay() {
		return clientDisplay;
	}
	
	public void setClientDisplay(ClientWindowConfiguration display) {
		this.clientDisplay = display;
	}

  @Override
  public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		lastMousePos = e.getPoint();
	}
	
	@Override
	public void mouseDragged(MouseEvent arg0) {
		pan(arg0.getX() - lastMousePos.x, arg0.getY() - lastMousePos.y);
		invalidateWallImage(false);
		repaint();
		lastMousePos = arg0.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		lastMousePos = null;
	}

	boolean msgShown = false;
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (wallWinBounds.width == simulatedDisplay.getXResolution()) {
				return;
			}
			autofitMode = false;
			zoomToPixelSize((int)xWinToWall(e.getX()), (int)yWinToWall(e.getY()), e.getX(), e.getY());
		} else {
			autofitMode = true;
			fitWallToDimensions(getSize());
		}
		invalidateWallImage(false);
		updateViewerDistance();
		repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		double zoom = Math.pow(1.02, arg0.getWheelRotation());
		autofitMode = false;
		zoomToSize(wallWinBounds.width * zoom, wallWinBounds.height * zoom, arg0.getX(), arg0.getY());
		invalidateWallImage(false);
		updateViewerDistance();
	}

	protected void moveFromBehindBezels(Rectangle2D bounds, int bezelWidthPixels, Rectangle2D limits) {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		double x0 = bounds.getX();
		double x1 = x0 + w;
		double y0 = bounds.getY();
		double y1 = y0 + h;
		int tw = simulatedDisplay.getTileXResolution();
		int th = simulatedDisplay.getTileYResolution();
		double r = bezelWidthPixels / 2;
		
		// move horizontally
		if (w <= tw - r*2) {
			for (int b = 1; b < simulatedDisplay.getXTiles(); b++) {
				int bx = b * tw;
				if (x0 < bx + r && x1 > bx - r) {
					double dx; 
					if ((x0 + x1) / 2 < bx)
						dx = bx - r - x1;
					else
						dx = bx + r - x0;
					if (in(x0 + dx, limits.getMinX(), limits.getMaxX()) && in(x1 + dx, limits.getMinX(), limits.getMaxX())) {
						x0 += dx;
						x1 += dx;
					}
				}
			}
		}
		
		// move vertically
		if (h <= th - r*2) {
			for (int b = 1; b < simulatedDisplay.getYTiles(); b++) {
				int by = b * th;
				if (y0 < by + r && y1 > by - r) {
					double dy; 
					if ((y0 + y1) / 2 < by)
						dy = by - r - y1;
					else
						dy = by + r - y0;
					if (in(y0 + dy, limits.getMinY(), limits.getMaxY()) && in(y1 + dy, limits.getMinY(), limits.getMaxY())) {
						y0 += dy;
						y1 += dy;
					}
				}
			}
		}
		
		bounds.setRect(x0, y0, x1-x0, y1-y0);
	}
	
	private static boolean in(double x, double x0, double x1) {
		return (x >= x0 && x <= x1);
	}
	
}
