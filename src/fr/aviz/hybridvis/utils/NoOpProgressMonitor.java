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

/**
 * A no-op progress monitor.
 * Don't need a UI? Want some sensible default ProgressMonitor? Use this.
 *
 * @author rprimet
 */
public class NoOpProgressMonitor implements ProgressMonitor {
  /**
   * Returns the minimum value -- the lower end of the progress value.
   */
  public int getMinimum(){
    return 0;
  }
  /**
   * Returns the maximum value -- the higher end of the progress value.
   */
  public int getMaximum(){
    return 100;
  }
  /**
   * Indicate the progress of the operation being monitored.
   */
  public void setProgress(int nv){}
  /**
   * Indicate that the operation is complete.
   */
  public void close(){}
  /**
   * Returns true if the user hits the Cancel button in the progress dialog.
   */
  public boolean isCanceled(){
    return false;
  }
  /**
   * Specifies the additional note that is displayed along with the progress message.
   */
  public void setNote(String note){}
}
