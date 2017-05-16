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

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.aviz.hybridvis.HybridImageRenderer;
import fr.aviz.hybridvis.display.DisplayConfiguration;
import fr.aviz.hybridvis.utils.DisplayConfigurationReader;
import fr.aviz.hybridvis.utils.SwingProgressMonitor;

/**
 * Blends two images using the hybrid image method.
 * Adapted from the HybridMap example (but meant to be more generic). 
 * 
 * The near and far images do not need to be the same resolution, but they need to 
 * be aligned when scaled at the same resolution (you can use photoshop for that).
 * The near image image will be shown with scale 1:1 on the wall-sized display (centered), 
 * and the far image will then be scaled up to occupy the same space.
 * 
 * Interactions supported:
 * - Click on any location to zoom at a 1:1 scale.
 * - Click again to switch back to the overview mode.
 * - When transitioning between zoom levels, a preview will be shown consisting in the near and far content alpha-blended (no high-pass and blurring).
 * - Hit S to generate the full image and save it on the disk (this process will take about 10 min).
 * 
 * See HybridImageRenderer for more info.
 * 
 * @author dragice
 * @author rprimet
 * 
 */
public class HybridImageGenerator {
  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private JFrame mainFrame;
  private HybridImageViewer viewer;
  private JPanel distanceSlider;
  //Preferences appPrefs;
  private File projectPath = null;
  static final String PROJECT_PATH_PROPERTY="projectPath";

   public HybridImageGenerator(){
    //Make sure GUI initialization happens in the EDT and after the constructor 
    //returns.
    javax.swing.SwingUtilities.invokeLater(new Runnable(){
      public void run(){
        setupGui();
        mainFrame.setSize(800, 600);
        mainFrame.setVisible(true);
      }
    });
  }
 
  public static void main(String[] args) {
    HybridImageGenerator generator = new HybridImageGenerator();
  }

  protected void setupGui() {
     mainFrame = new JFrame("Hybrid Image Generator");
     viewer = new HybridImageViewer();
     viewer.setDrawPowerSpectrum(false);
     viewer.addPropertyChangeListener(WallRenderer.SIMULATED_DISPLAY_PROPERTY, new PropertyChangeListener(){
       public void propertyChange(PropertyChangeEvent evt){
         setupDisplayControls();
       }
     });
     viewer.addPropertyChangeListener(HybridImageRenderer.SETTINGS_PROPERTY, new PropertyChangeListener(){
       public void propertyChange(PropertyChangeEvent evt){
         viewer.loadNearImageAsync(viewer.settings.nearImagePath); //XXX
         viewer.loadFarImageAsync(viewer.settings.farImagePath); //XXX
         viewer.setDisplayConfiguration(viewer.settings.displayPath); //XXX
         viewer.setupSettingsControls();
       }
     });
     this.addPropertyChangeListener(PROJECT_PATH_PROPERTY, new PropertyChangeListener(){
       public void propertyChange(PropertyChangeEvent evt){
	       String title = projectPath.getName() + " - " + "Hybrid Image Generator";
         mainFrame.setTitle(title);
       }
     });
     JMenuBar jmb = makeMenuBar();
     mainFrame.setJMenuBar(jmb);
     mainFrame.getContentPane().add(viewer, BorderLayout.CENTER);
     setupDisplayControls();
     mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
   }

   protected void setupDisplayControls(){
     if(distanceSlider != null){
       distanceSlider.hide();
       mainFrame.getContentPane().remove(distanceSlider);
     }
     distanceSlider = viewer.getSimulatedDisplay().getControlPanel();
     mainFrame.getContentPane().add(distanceSlider, BorderLayout.NORTH);
   }

  protected JMenuBar makeMenuBar(){
   JMenuBar retval = new JMenuBar();

   JMenu file = new JMenu("File");
   final JMenuItem op = new JMenuItem("Open Project"); 
   final JMenuItem sp = new JMenuItem("Save Project"); 
   final JMenuItem ex = new JMenuItem("Exit"); 
   file.add(op);
   file.add(sp);
   file.addSeparator();
   file.add(ex);

   JMenu project = new JMenu("Project");
   final JMenuItem sn = new JMenuItem("Set near image");
   final JMenuItem sf = new JMenuItem("Set far image");
   final JMenuItem sd = new JMenuItem("Set wall display");
   project.add(sn);
   project.add(sf);
   project.add(sd);

   JMenu generate = new JMenu("Generate");
   final JMenuItem gi = new JMenuItem("Generate composite image");
   generate.add(gi);

   retval.add(file);
   retval.add(project);
   retval.add(generate);

   op.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       openProject();
     }
   });

   sp.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       saveProject();
     }
   });

   ex.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       //Not a fan of this heavy-handed method.
       //We should rather mark the rendering threads as demons and 
       //dispose() of our windows in an orderly fasion.
       System.exit(0);
     }
   });

   sn.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       openNearImage();
     }
   });

   sf.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       openFarImage();
     }
   });

   sd.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       openDisplayConfiguration();
     }
   });

   gi.addActionListener(new ActionListener(){
     public void actionPerformed(ActionEvent ae){
       generateHybridImage(); 
     }
   });

   return retval;
  }

  private void setProjectPath(File projectPath){
    final File oldVal = this.projectPath;
    this.projectPath = projectPath;
    pcs.firePropertyChange(PROJECT_PATH_PROPERTY, oldVal, projectPath);
  }

  protected void openNearImage(){
    final JFileChooser jfc = new JFileChooser(new File(".")); 
    final FileFilter imageFilter = new FileNameExtensionFilter(
      "Image files", ImageIO.getReaderFileSuffixes());
    jfc.setFileFilter(imageFilter);
    jfc.setDialogTitle("Choose Near Image");
    if(jfc.showOpenDialog(viewer) == JFileChooser.APPROVE_OPTION){
      viewer.loadNearImageAsync(jfc.getSelectedFile().getAbsolutePath());
    }
  }

  protected void openFarImage(){
    final JFileChooser jfc = new JFileChooser(new File("."));
    final FileFilter imageFilter = new FileNameExtensionFilter(
      "Image files", ImageIO.getReaderFileSuffixes());
    jfc.setFileFilter(imageFilter);
    jfc.setDialogTitle("Choose Far Image");
    if(jfc.showOpenDialog(viewer) == JFileChooser.APPROVE_OPTION){
      viewer.loadFarImageAsync(jfc.getSelectedFile().getAbsolutePath());
    }
  }

  protected void openProject(){
    final JFileChooser jfc = new JFileChooser(new File("."));
    final FileFilter projectFilter = new FileNameExtensionFilter("Properties files", "properties");
    jfc.setFileFilter(projectFilter);
    if(jfc.showOpenDialog(viewer) == JFileChooser.APPROVE_OPTION){
      setProjectPath(jfc.getSelectedFile());
      viewer.openProject(projectPath);
    }
  }

  protected void saveProject(){
    if(projectPath != null && projectPath.exists()){
      viewer.saveProject(projectPath);
    } else {
      final JFileChooser jfc = new JFileChooser(new File("."));
      final FileFilter projectFilter = new FileNameExtensionFilter("Properties files", "properties");
      jfc.setFileFilter(projectFilter);
      if(jfc.showOpenDialog(viewer) == JFileChooser.APPROVE_OPTION){
	      setProjectPath(jfc.getSelectedFile());
        viewer.saveProject(projectPath);
      }
    }
  }

  protected void openDisplayConfiguration(){
    final JFileChooser jfc = new JFileChooser(new File("."));
    final FileFilter propFilter = new FileNameExtensionFilter("Properties files", "properties");
    jfc.setFileFilter(propFilter);
    if(jfc.showOpenDialog(viewer) == JFileChooser.APPROVE_OPTION){
      viewer.setDisplayConfiguration(jfc.getSelectedFile().getAbsolutePath());
    }
  }

  protected void generateHybridImage(){
    viewer.saveHybridImageAsyncInteractive(projectPath);
  }

  public void addPropertyChangeListener(PropertyChangeListener listener){
    pcs.addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener){
    pcs.addPropertyChangeListener(propertyName, listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener){
    pcs.removePropertyChangeListener(listener);
  }
}

class HybridImageViewer extends HybridImageRenderer {

  BufferedImage nearImage = null;
  BufferedImage farImage = null;
  Rectangle mapImageBounds = new Rectangle();

  public HybridImageViewer() {

    super();

    // Are those defaults sensible?
    setHipassRadius(12);
    setHipassContrast(1.7f);
    setHipassBrightness(1.4f);
    setBlurRadius(12);
    setFarImageOpacity(0.4);
    setPostContrast(2.3);
    setPostBrightness(0.77);
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
   * 
   */	@Override
  public void drawNearGraphics(Graphics2D g) {
    g.drawImage(nearImage, mapImageBounds.x, mapImageBounds.y, mapImageBounds.width, mapImageBounds.height, null);
  }

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
  @Override
  public void drawFarGraphics(Graphics2D g) {
    g.drawImage(farImage, mapImageBounds.x, mapImageBounds.y, mapImageBounds.width, mapImageBounds.height, null);
  }

  protected void computeImageBounds(){ 
    if(nearImage != null){
      // Place the hybrid map on the wall-sized display so its resolution is the same as the near image and it is centered.
      mapImageBounds.setRect(
        (getWallWidth() - nearImage.getWidth()) / 2,
        (getWallHeight() - nearImage.getHeight()) / 2,
        nearImage.getWidth(),
        nearImage.getHeight());	
    }
  }

  protected void setDisplayConfiguration(String path){
    try{
      DisplayConfiguration dc = DisplayConfigurationReader.fromFile(new File(path)); 
      setSimulatedDisplay(dc);
      settings.displayPath = path; //XXX
    } catch(IOException ioe){
      ioe.printStackTrace();
    }
    computeImageBounds();
    repaint();
  }

  /**
   * Loads near image from path and updates the UI.
   * This method is asynchronous and returns immediately.
   */
  public void loadNearImageAsync(final String path){
    new SwingWorker<BufferedImage, Void>(){
      @Override
      protected BufferedImage doInBackground(){
        try{
          return ImageIO.read(new File(path));
        } catch(IOException ioe){
          ioe.printStackTrace();
        }
        return null;
      }
      @Override
      protected void done(){
        try{
          setNearImage(get());
          settings.nearImagePath = path; //XXX
        } catch (Exception ex){
          Thread.currentThread().interrupt();
        }
      }
    }.execute();
  }

  /**
  * Loads far image from path and updates the UI.
  * This method is asynchronous and returns immediately.
  */
  public void loadFarImageAsync(final String path){
    new SwingWorker<BufferedImage, Void>(){
      @Override
      protected BufferedImage doInBackground(){
        try{
          return ImageIO.read(new File(path));
        } catch(IOException ioe){
          ioe.printStackTrace();
        }
        return null;
      }
      @Override
      protected void done(){
        try{ 
          setFarImage(get());
          settings.farImagePath = path; //XXX -- see setFarImage
        } catch (Exception ex){
          Thread.currentThread().interrupt();
        }
      }
    }.execute();
  }

  /**
   * Sets near image and updates the UI.
   */
  private void setNearImage(BufferedImage img){
    nearImage = img;
    computeImageBounds();
    invalidateWallImage(false);
    repaint();
  }

  /**
   * Sets far image from path and updates the UI.
   */
  private void setFarImage(BufferedImage img){
    farImage = img;
    // Images should be aligned: we do not recompute bounds when setting the far image (?)
    invalidateWallImage(false);
    repaint();
  }

  void openProject(File projectFile){
    try{
      HybridImageRendererSettings settings = HybridImageRendererSettings.fromFile(projectFile);
      setSettings(settings);
    }catch(IOException ioe){
      ioe.printStackTrace();
    }
  }

  void saveProject(File projectFile){
    try{
      settings.persist(projectFile);
    }catch(IOException ioe){
      ioe.printStackTrace();
    }
  }
}
