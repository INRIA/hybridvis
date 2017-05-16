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

package fr.aviz.hybridvis.examples;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import org.gicentre.utils.colour.ColourTable;

import ca.ucalgary.innovis.NAryTree;
import ca.ucalgary.innovis.NAryTreeLoader;
import ca.ucalgary.innovis.NAryTreeNode;
import fr.aviz.hybridvis.HybridImageRenderer;

public class HybridTreemap extends HybridImageRenderer{

	private NAryTree tree;
	private final int TRADITIONAL_LAYOUT = 1;
	private final int SQUARIFIED_LAYOUT = 2;

	private final int WEIGHT_BY_CHILDREN = 0;
	private final int WEIGHT_BY_SUBTREE = 1;

	private final int COLOR_BY_LEVEL = 0;
	private final int COLOR_BY_RANK = 1; //don't use this. There are 18 ranks, too many to color
		
	Font maxLabelFontNear = new Font("Helvetica", Font.PLAIN, 50);
	Font maxLabelFontFar = new Font("Helvetica", Font.BOLD, 700);

	////////

	public int layout = SQUARIFIED_LAYOUT;
	public int colorType = COLOR_BY_LEVEL;
	public double weightType = 0.5; // when different from the two constants above, specify a weight between the two 

	public double frame = 0;

	public boolean cushion = false;

	HashSet<String> rankTypes = new HashSet<String>();
	ArrayList<String> ranks = new ArrayList<String>();
	Rectangle2D.Double tmpLblLimits = new Rectangle2D.Double();
	Rectangle2D.Double tmpLblBounds = new Rectangle2D.Double();
	Rectangle2D.Double tmpRect = new Rectangle2D.Double();

	static class NodeFontInfo {
		String label;
		Font maxFont = null;
		AffineTransform transform = null;
		boolean horizontal;
		double width;
		double height;
		double descent;
		Shape glyphshape = null;
	}
	Hashtable<String, NodeFontInfo> nodeFontInfos = new Hashtable<String, NodeFontInfo>();
	Rectangle2D.Double tmpRec = new Rectangle2D.Double();
	boolean fontsComputed = false;
	
	public HybridTreemap(){

		super();

		// Configure the HybridImage Renderer
		setDrawBackground(true);
		//
		setDrawNearImage(true);
		setHipassRadius(8);
		setTransparentHipass(true);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setNearImageOpacity(0.5f);
		//
		setDrawFarImage(true);
		setBlurRadius(40);
		setFarImageOpacity(0.4f);
		//
		setPostContrast(1.5);
		setPostBrightness(0.8);
		//
		setDrawPowerSpectrum(true);

		// Load tree
		System.out.println("Loading tree");
		//File xmlFile = new File("data/classif_A_03-04-16_mammals.ivc");
		File xmlFile = new File("./data/species/classif_A_03-04-16.ivc");
		//File xmlFile = new File("D:/Programming/MultiScaleExamples/data/classif_B_03-04-16.ivc");
		tree = NAryTreeLoader.loadTree(xmlFile);
		System.out.println("Loaded Tree of depth: " + tree.getDepth(false) + " and node count: " + tree.getNodeCount(false));

		calculateTreeMap();
		for(String s:rankTypes) {
			ranks.add(s);
		}
		
		// Initialize fonts
		System.out.print("Computing font info... ");
		BufferedImage bim = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bim.getGraphics();
		drawNearGraphics(g);
		drawFarGraphics(g);
		fontsComputed = true;
		System.out.println("Done.");
	}

	public static void main(String[] args) {
		HybridTreemap viewer = new HybridTreemap();
		viewer.showOnScreen();
	}


	protected void calculateTreeMap(){
		NAryTreeNode root = (NAryTreeNode) tree.getRoot();
		int tileWidth = getSimulatedDisplay().getTileXResolution();
		int tileHeight = getSimulatedDisplay().getTileYResolution();
		int tileCountX = getSimulatedDisplay().getXTiles();
		int tileCountY = getSimulatedDisplay().getYTiles();
		root.setNodeSize(tileWidth * tileCountX, tileHeight * tileCountY);
		root.setXPosition(0);
		root.setYPosition(0);


		if(layout==TRADITIONAL_LAYOUT) {
			root.setWeight(1.0);
			traditionalLayout(root);
		}
		else if(layout == SQUARIFIED_LAYOUT) {
			root.setWeight(root.getWidth() * root.getHeight());
			rectangle.setRect(root.getXPosition(), root.getYPosition(), root.getWidth(), root.getHeight());
			updateArea(root, rectangle);
			squarifiedLayout(root, rectangle);
		}

		System.out.println("Tree Layout calculated");
	}


	private LinkedList<NAryTreeNode> children = new LinkedList<NAryTreeNode>();
	private LinkedList<NAryTreeNode> row = new LinkedList<NAryTreeNode>();
	private Rectangle2D rectangle = new Rectangle2D.Double();

	protected void squarifiedLayout(NAryTreeNode node, Rectangle2D rect) {

		rankTypes.add(node.getType());

		//first get the children and sort according to size
		LinkedList<NAryTreeNode> kids = node.getChildren();
		//and while we're at it we calculate the area
		double nodeArea = node.getWidth() * node.getHeight();
		double subTreeSize = node.getSubTreeSize();

		for(NAryTreeNode n:kids) {

			if(weightType == WEIGHT_BY_SUBTREE){
				n.setWeight(nodeArea * (n.getSubTreeSize() + 1) / subTreeSize);
			}
			else if(weightType == WEIGHT_BY_CHILDREN){
				if(n.getParent() == null){
					n.setWeight(nodeArea);
				}
				else{
					n.setWeight(1.0/node.getChildCount() * node.getWeight());
				}
			}
			else {
				double subtree_weight = nodeArea * (n.getSubTreeSize() + 1) / subTreeSize;
				double children_weigth = n.getParent() == null ? nodeArea : 1.0/node.getChildCount() * node.getWeight();
				n.setWeight(subtree_weight * weightType + children_weigth * (1 - weightType));
			}

			children.add(n);


		}
		//this is supposed to help the algorithm perform well
		Collections.sort(kids,NAryTreeNode.weightComparator);

		//now do the actual algorithm
		double w = Math.min(rect.getWidth(), rect.getHeight());
		squarify(children,row,w,rect);

		children.clear();

		//continue through recursion
		for(NAryTreeNode n:kids) {
			if ( n.getChildCount() > 0 && n.getWeight() > 0 ) {
				updateArea(n,rect);
				squarifiedLayout(n, rect);
			}
		}
	}

	private void updateArea(NAryTreeNode n, Rectangle2D r) {
		//Rectangle2D b = n.getBounds();
		if ( frame == 0.0 ) {

			// if no framing, simply update bounding rectangle
			r.setRect(n.getXPosition(),n.getYPosition(),n.getWidth(),n.getHeight());
			return;
		}

		// compute area loss due to frame
		double dA = 2.0 * frame * (n.getWidth() + n.getHeight() - 2.0 * frame);
		double A = n.getWeight() - dA;

		// compute renormalization factor
		double s = 0;
		for(NAryTreeNode child:n.getChildren()) {
			s += child.getWeight();
		}
		double t = A/s;

		// re-normalize children areas
		for(NAryTreeNode child:n.getChildren()) {
			child.setWeight(child.getWeight() * t);
		}

		// set bounding rectangle and return
		r.setRect(n.getXPosition() + frame, n.getYPosition() + frame, n.getWidth() - 2 * frame, n.getHeight() - 2 * frame);
		return;
	}

	protected void squarify(LinkedList<NAryTreeNode> nodes, LinkedList<NAryTreeNode> row, double w, Rectangle2D r) {
		double worst = java.lang.Double.MAX_VALUE, nworst; 
		int length;

		while ( (length=nodes.size()) > 0 ) {

			// add item to the row list, ignore if negative area
			NAryTreeNode node = nodes.get(length-1);

			double a = node.getWeight();
			if (a <= 0.0) {
				nodes.remove(length-1);
				continue;
			}

			row.add(node);

			nworst = worst(row, w);
			if ( nworst <= worst ) {
				nodes.remove(length-1);
				worst = nworst;
			} else {
				row.remove(row.size()-1); // remove the latest addition
				r = layoutRow(row, w, r); // layout the current row
				w = Math.min(r.getWidth(),r.getHeight()); // recompute w
				row.clear(); // clear the row
				worst = Double.MAX_VALUE;
			}
		}
		if ( row.size() > 0 ) {
			r = layoutRow(row, w, r); // layout the current row
			row.clear(); // clear the row
		}
	}


	protected Rectangle2D layoutRow(LinkedList<NAryTreeNode> row, double w, Rectangle2D r) {

		double s = 0; // sum of row areas
		Iterator<NAryTreeNode> rowIter = row.iterator();

		for(NAryTreeNode n:row)
			s += n.getWeight();


		double x = r.getX();
		double y = r.getY();
		double d = 0;
		double h = w == 0 ? 0 : s/w;
		boolean horiz = (w == r.getWidth());

		// set node positions and dimensions
		for(NAryTreeNode n:row) {

			NAryTreeNode p = (NAryTreeNode) n.getParent();

			if ( horiz ) {
				if(x+d == Double.NaN) {
					n.setXPosition(p.getXPosition());
				}
				else {
					n.setXPosition(x + d);
				}

				if(y == Double.NaN) {
					n.setYPosition(p.getYPosition());
				}
				else {
					n.setYPosition(y);
				}
			} else {
				if(x == Double.NaN) {
					n.setXPosition(p.getXPosition());
				}
				else {
					n.setXPosition(x);
				}

				if(y + d == Double.NaN) {
					n.setYPosition(p.getYPosition());
				}
				else {
					n.setYPosition(y + d);
				}
			}

			double nw = n.getWeight() / h;
			if ( horiz ) {
				n.setWidth(nw);
				n.setHeight(h);
				d += nw;
			} else {
				n.setWidth(h);
				n.setHeight(nw);
				d += nw;
			}
		}
		// update space available in rectangle r
		if ( horiz )
			r.setRect(x,y+h,r.getWidth(),r.getHeight()-h);
		else
			r.setRect(x+h,y,r.getWidth()-h,r.getHeight());
		return r;
	}


	protected double worst(LinkedList<NAryTreeNode> row,double w) {
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		double s = 0;

		for(NAryTreeNode n:row) {
			double r = n.getWeight();
			min = Math.min(min, r);
			max = Math.max(max, r);
			s += r;
		}
		s = s * s;
		w = w * w;
		return Math.max(w * max / s , s / (w * min));
	}

	protected void traditionalLayout(NAryTreeNode node){

		//weight by subtree size
		NAryTreeNode parent = (NAryTreeNode) node.getParent();

		if(parent != null){

			if(weightType == WEIGHT_BY_SUBTREE) {
				double totalWeight = parent.getChildCount();
				for(int i = 0; i < parent.getChildCount(); ++i){
					NAryTreeNode sibling = (NAryTreeNode)parent.getChildAt(i);
					totalWeight+=sibling.getSubTreeSize();
				}

				node.setWeight((node.getSubTreeSize() + 1.0)/ totalWeight );
			}
			else if(weightType == WEIGHT_BY_CHILDREN) {
				node.setWeight(1.0 / parent.getChildCount());
			}

			int index = parent.getIndex(node);
			int level = parent.getLevel();

			if(level % 2 == 1){
				//we do a vertical split
				node.setWidth(parent.getWidth() * node.getWeight());
				node.setHeight(parent.getHeight());
			}
			else{
				node.setWidth(parent.getWidth());
				node.setHeight(parent.getHeight() * node.getWeight());
			}


			if(index == 0){
				node.setXPosition(parent.getXPosition());
				node.setYPosition(parent.getYPosition());
			}
			else{
				NAryTreeNode leftSibling = (NAryTreeNode) parent.getChildAt(index - 1);

				if(level % 2 == 1){
					node.setXPosition(leftSibling.getXPosition() + leftSibling.getWidth());
					node.setYPosition(leftSibling.getYPosition());
				}
				else{
					node.setXPosition(leftSibling.getXPosition());
					node.setYPosition(leftSibling.getYPosition()  + leftSibling.getHeight());
				}
			}
		}

		for(int i = 0; i < node.getChildCount(); ++i){
			traditionalLayout((NAryTreeNode) node.getChildAt(i));
		}
	}

	private void paintShadow(Graphics2D g2, double shadowWidth, Rectangle2D shape) {

		//g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		//double smallestSide = Math.min(shape.getWidth(), shape.getHeight()) * 0.8;
		//if(shadowWidth > smallestSide) shadowWidth = smallestSide;


		//this is very slow
		/*
		double alphaMinusFactor = 255 / shadowWidth;
		Rectangle2D.Double newRect = new Rectangle2D.Double();

		for(int i = 0; i < shadowWidth; ++i) {
			g2.setColor(new Color(0,0,0, (int)(255 - i *alphaMinusFactor)));
			newRect.setFrame(shape.getX() + i,shape.getY()+i,shape.getWidth() - 2 * i,shape.getHeight() - 2 * i);
			g2.draw(newRect);
		} */

		//another approach maybe

		int xl = (int) shape.getX();
		int yo = (int) shape.getY();
		int xr = (int) (shape.getX() + shape.getWidth());
		int yu = (int) (shape.getY() + shape.getHeight());
		int sW = (int) shadowWidth; 

		int[] xpointstop = new int[] {xl, xl + sW, xr - sW, xr};
		int[] ypointstop = new int[] {yo, yo + sW, yo + sW, yo};
		Polygon topPolygon = new Polygon(xpointstop,ypointstop,4);

		int[] xpointsleft = new int[] {xl,xl,xl+sW,xl+sW};
		int[] ypointsleft = new int[] {yo,yu,yu-sW,yo+sW};
		Polygon leftPolygon = new Polygon(xpointsleft,ypointsleft,4);

		int[] xpointsbottom = new int[] {xl,xr,xr-sW,xl+sW};
		int[] ypointsbottom = new int[] {yu,yu,yu-sW,yu-sW};
		Polygon bottomPolygon = new Polygon(xpointsbottom,ypointsbottom,4);

		int[] xpointsright = new int[] {xr,xr,xr-sW,xr-sW};
		int[] ypointsright = new int[] {yu,yo,yo+sW,yu-sW};
		Polygon rightPolygon = new Polygon(xpointsright,ypointsright,4);

		Color outside = new Color(0,0,0,155);
		Color inside = new Color(0,0,0,0);

		GradientPaint paint = new GradientPaint(xl,yo,outside,xl,yo+sW,inside);
		g2.setPaint(paint);
		g2.fill(topPolygon);

		paint = new GradientPaint(xl,yo,outside,xl+sW,yo,inside);
		g2.setPaint(paint);
		g2.fill(leftPolygon);

		paint = new GradientPaint(xl,yu,outside,xl,yu-sW,inside);
		g2.setPaint(paint);
		g2.fill(bottomPolygon);

		paint = new GradientPaint(xr,yu,outside,xr-sW,yu,inside);
		g2.setPaint(paint);
		g2.fill(rightPolygon);

		/*
		Rectangle2D.Double newRect = new Rectangle2D.Double(shape.getX() + shadowWidth,shape.getY()+shadowWidth,shape.getWidth() - 2 * shadowWidth,shape.getHeight() - 2 * shadowWidth );



	    int sw = shadowWidth*2;
	    for (int i=sw; i >= 2; i-=2) {
	        float pct = (float)(sw - i) / (sw - 1);
	        g2.setColor(getMixedColor(Color.LIGHT_GRAY, pct,
	                                  Color.WHITE, 1.0f-pct));
	        g2.setStroke(new BasicStroke(i));
	        g2.draw(newRect);
	    } */
	}

	
	@Override
	public void drawBackgroundGraphics(Graphics2D g) {
		ColourTable ctable = new ColourTable();

		if(colorType == COLOR_BY_LEVEL) {
			ctable = ColourTable.getPresetColourTable(ColourTable.YL_OR_BR, 0, tree.getDepth(false));
		}
		else if(colorType == COLOR_BY_RANK) {
			ctable = ColourTable.getPresetColourTable(ColourTable.SET3_12,0,rankTypes.size());
		}

		drawNode(g, (NAryTreeNode) tree.getRoot(), ctable, null, 0, Integer.MAX_VALUE, -1, -1, -1, -1, false, true);		
	}
	
	@Override
	public void drawNearGraphics(Graphics2D g) {

		// fill the background (important for the hi-pass filter)
		g.setColor(Color.white);
		g.fillRect(0, 0, getSimulatedDisplay().getXResolution(), getSimulatedDisplay().getYResolution());

//		drawBackgroundGraphics(g);	
		drawNode(g, (NAryTreeNode) tree.getRoot(), null, maxLabelFontNear, -1, -1, 3, Integer.MAX_VALUE, 3, Integer.MAX_VALUE, false, true);		
	}
	
	@Override
	public void drawFarGraphics(Graphics2D g) {
		
		drawNode(g, (NAryTreeNode) tree.getRoot(), null, maxLabelFontFar, -1, -1, 1, 4, 2, 2, true, true);
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////

	protected void drawNode(Graphics2D g, NAryTreeNode node, ColourTable ctable, Font maxFont, int minFillLevel, int maxFillLevel, int minBorderLevel, int maxBorderLevel, int minLabelLevel, int maxLabelLevel, boolean labelOutline, boolean testClipping) {
		if(node.getWeight() < 0 || node.getHeight() < 0 || node.getWidth() < 0) return;

		tmpRect.setRect(node.getXPosition(), node.getYPosition(), node.getWidth(), node.getHeight());
		
		// test clipping
		if (testClipping && g.getClip() != null) {
			Rectangle clip = g.getClipBounds();
			if (!clip.intersects(tmpRect))
				return;
			testClipping = !clip.contains(tmpRect);
		}
		
		// give up if the zoom level or other settings changed
		if (renderingInterrupted()) return;
		
		int level = node.getLevel();
		
		Color color = Color.BLACK;

		// ---- Fill the square
		
		if (level >= minFillLevel && level <= maxFillLevel) {
			
			if(colorType == COLOR_BY_RANK) {
				color = new Color(ctable.findColour(ranks.indexOf(node.getType())),false);
			}
			else if(colorType == COLOR_BY_LEVEL) {
				color = new Color(ctable.findColour(node.getLevel() - 1),false);
			}
			
			if(!cushion){
				g.setColor(color);
				g.fill(tmpRect);
			}
			else{
				/* poor man's cushion
				Point2D.Double left = new Point2D.Double(node.getXPosition(),node.getYPosition());
				Point2D.Double center = new Point2D.Double(node.getXPosition() + 0.5 * node.getWidth(), node.getYPosition());
	
				GradientPaint gradient = new GradientPaint(left, Color.BLACK, center, color);
				g.setPaint(gradient);
				 */
	
				//prettier cushion
				//paintBorderGlow(g,tree.getDepth(false) - node.getLevel() + 3,rect);
	
				//Rectangle2D.Double smallerRect = new Rectangle2D.Double(node.getXPosition() + shadowWidth, node.getYPosition() + shadowWidth, node.getWidth() - 2 * shadowWidth, node.getHeight() - 2 * shadowWidth);
				g.setColor(color);
				g.fill(tmpRect);
	
				if(node.isLeaf()) {
					double shadowWidth = 0.2 * Math.min(node.getWeight(), node.getHeight()); //tree.getDepth(false) - node.getLevel() + 1;
				//	paintShadow(g, shadowWidth, tmpRect);
				}
			}
		}

		// ---- Draw border

		if (level >= minBorderLevel && level <= maxBorderLevel) {
			//float thickness = 0.075f + 400 / (float)Math.pow(3, level);
			double thickness = 0.075f + 1.0/32.0 * getSimulatedDisplay().getYResolution() * getSimulatedDisplay().getYTiles() / (float)Math.pow(3, level);
			g.setStroke(new BasicStroke((float) thickness));
			g.setColor(Color.BLACK);
			g.draw(tmpRect);
		}

		// ---- Draw label
		
		if (level >= minLabelLevel && level <= maxLabelLevel) {
			g.setColor(Color.BLACK);
			if (fontsComputed) {
				// fonts have been already computed, so the label needs to be drawn if there font info for this node
				if (nodeFontInfos.containsKey(node.getID())) {
					drawNodeLabel(g,maxFont,node, labelOutline);
				}
			} else {
				// fonts have not been computed yet, so let's decide intelligently whether we should draw the label 
				if (node.isLeaf() || level == maxLabelLevel) {
					drawNodeLabel(g,maxFont,node, labelOutline);
				} else {
					// figure out if we should draw the label now because direct children will be too small
					final double maxNodeSizePerCharacterForLabelling = 3; // remember labels can be trimmed 
					double minNodeSizePerCharacter = Double.MAX_VALUE;
					int n = node.getChildCount();
					for (int i=0; i<n; i++) {
						NAryTreeNode child = (NAryTreeNode)(node.getChildAt(i));
						double size = Math.min(child.getWidth(), child.getHeight());
						double sizePerChar = size / child.getLabel().length();
						if (sizePerChar < minNodeSizePerCharacter) minNodeSizePerCharacter = sizePerChar;
					}
					if (minNodeSizePerCharacter < maxNodeSizePerCharacterForLabelling) {
						// Time to draw the label
						drawNodeLabel(g,maxFont,node, labelOutline);
						// Don't draw more labels for children
						minLabelLevel = -1;
						maxLabelLevel = -1;
					}
				}
			}
		}

		// ---- Draw children
		
		if (level >= maxBorderLevel && level >= maxFillLevel && level >= maxLabelLevel)
			return;
		
		for(int i = 0; i < node.getChildCount(); ++i){
			drawNode(g, (NAryTreeNode) node.getChildAt(i), ctable, maxFont, minFillLevel, maxFillLevel, minBorderLevel, maxBorderLevel, minLabelLevel, maxLabelLevel, labelOutline, testClipping);
			// give up if the zoom level or other settings changed
			if (renderingInterrupted()) return;
		}
	}

	public void drawNodeLabel(Graphics2D g, Font maxFont, NAryTreeNode node, boolean outline) {
		String label = node.getLabel().trim();
		
		if (label.length() == 0)
			return;
				
		// This will activate more precise (but also more ugly) font rendering when zoomed out
		if (g.getTransform().getScaleX() != 1)
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		
		if (!nodeFontInfos.containsKey(node.getID())) {
		
			if (fontsComputed)
				return; // this node has no label displayed (space was too small)
			
			// first determine if we draw horizontally or vertically
			boolean horizontal = node.getWidth() > node.getHeight();
	
			// horizontal margin should be smaller for long labels
			double margin = 1.5 / label.length();
			
			// max space allowed
			double maxLabelHeight = 0.9 * (horizontal ? node.getHeight() : node.getWidth());
			double maxLabelWidth = (1.0 - margin) * (horizontal ? node.getWidth() : node.getHeight());
	
			FontMetrics metrics = null;
			int fontSize = maxFont.getSize();

			// regress until the font height fits the smaller side
			boolean fits = false;
			
			while(!fits) {
				
				metrics = g.getFontMetrics(maxFont);
				Rectangle2D stringBounds = metrics.getStringBounds(label, g);
	
				if (stringBounds.getHeight() < maxLabelHeight) {
					fits = true;
				} else {
					fontSize -= 0.5 + fontSize / 10;
					if (fontSize < 9)
						return; //  height does not fit
					maxFont = new Font(maxFont.getFontName(), 0, fontSize);
				}
			}
	
			// regress until the string width fits the larger side
			
			fits = false;
			boolean trimLabel = false; 
			
			while(!fits) {
				
				if (!trimLabel)
					metrics = g.getFontMetrics(maxFont);
				Rectangle2D stringBounds = metrics.getStringBounds(label, g);
				
				if (stringBounds.getWidth() <= maxLabelWidth) {
					fits = true;
				} else {
					if (!trimLabel) {
						fontSize -= 1 + fontSize / 10;
						if (fontSize < 9) {
							fontSize = 9;
							label += "\u2026";
							trimLabel = true;
						}
						maxFont = new Font(maxFont.getFontName(), 0, fontSize);
					} else {
						if (label.length() < 6)
							return;
						label = label.substring(0, label.length() - 2) + "\u2026";
					}
				}	
			}

			// Compute label bounds
			metrics = g.getFontMetrics(maxFont);
			Rectangle2D bounds = metrics.getStringBounds(label, g);
			double width = bounds.getWidth();
			double height = bounds.getHeight();
			double descent = metrics.getDescent();
			
			// Compute label position and orientation
			double x, y, rotation;
			if (horizontal) {
				x = node.getXPosition() + 0.5 * node.getWidth() - 0.5 * width;
				y = node.getYPosition() + 0.5 * node.getHeight() + 0.5 * height - descent;
				rotation = 0;
				// Avoid bezels
				tmpLblBounds.setRect(x, y + descent - height, width, height);
				tmpLblLimits.setRect(node.getXPosition(), node.getYPosition(), node.getWidth(), node.getHeight());
				moveFromBehindBezels(tmpLblBounds, 70, tmpLblLimits);
				x = tmpLblBounds.x;
				y = tmpLblBounds.y - descent + height;
			} else {
				x = node.getXPosition() + 0.5 * node.getWidth() + 0.5 * height - descent;
				y = node.getYPosition() + 0.5 * node.getHeight() + 0.5 * width;
				rotation = -Math.PI / 2;
				// Avoid bezels
				tmpLblBounds.setRect(x + descent - height, y, height, width);
				tmpLblLimits.setRect(node.getXPosition(), node.getYPosition(), node.getWidth(), node.getHeight());
				moveFromBehindBezels(tmpLblBounds, 70, tmpLblLimits);
				x = tmpLblBounds.x - descent + height;
				y = tmpLblBounds.y;
			}
			
			
			AffineTransform transform = new AffineTransform();
			transform.translate(x, y);
			transform.rotate(rotation);
			
			NodeFontInfo info = new NodeFontInfo();
			info.label = label;
			info.maxFont = maxFont;
			info.transform = transform;
			info.horizontal = horizontal;
			info.width = width;
			info.height = height;
			info.descent = metrics.getDescent();
			if (outline) {
				FontRenderContext frc = g.getFontRenderContext();
		        GlyphVector gv = maxFont.createGlyphVector(frc, label);
		        info.glyphshape = gv.getOutline(0, 0);
			}
			nodeFontInfos.put(node.getID(), info);
		}

		
		////////////
		
		NodeFontInfo info = nodeFontInfos.get(node.getID());
		AffineTransform at0 = g.getTransform();
		
		double x = info.transform.getTranslateX();
		double y = info.transform.getTranslateY();
		g.transform(info.transform);
		drawString(g, info, outline);
		g.setTransform(at0);
	}
	
	// Draw string with an optional outline
	protected void drawString(Graphics2D g, NodeFontInfo info, boolean outline) {
		if (!outline) {
			g.setFont(info.maxFont);
	        g.setColor(Color.black);
			g.drawString(info.label, 0, 0);
		} else {
	        g.setColor(Color.black);
	        g.fill(info.glyphshape);
	        g.setColor(Color.white);
	        float strokeWidth = (float)(info.maxFont.getSize() / 40.0);
	        g.setStroke(new BasicStroke(strokeWidth));
	        g.draw(info.glyphshape);
		}
	}
}
