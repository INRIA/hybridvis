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
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.gicentre.utils.colour.ColourTable;

import ca.ucalgary.innovis.NAryTree;
import ca.ucalgary.innovis.NAryTreeLoader;
import ca.ucalgary.innovis.NAryTreeNode;
import fr.aviz.hybridvis.HybridImageRenderer;

public class HybridIcicleCladogram extends HybridImageRenderer{

	private NAryTree tree;
	private int TRADITIONAL_LAYOUT = 1;
	private int SQUARIFIED_LAYOUT = 2;
	
	private int COLOR_BY_LEVEL = 0;
	private int COLOR_BY_RANK = 1;

	public int layout = SQUARIFIED_LAYOUT;
	public int colorType = COLOR_BY_LEVEL;
	public double frame = 0;
	
	HashSet<String> rankTypes = new HashSet<String>();
	ArrayList<String> ranks = new ArrayList<String>();
	ColourTable ctable = new ColourTable();
	

	public HybridIcicleCladogram(){

		super();
		
		// Configure the HybridImage Renderer.
		setHipassRadius(10);
		setHipassContrast(1.5f);
		setHipassBrightness(1.5f);
		setBlurRadius(20);
		setFarImageOpacity(0.4);
		setPostContrast(1.1);
		setPostBrightness(1.0);

		System.out.println("Loading tree");
		File xmlFile = new File("data/species/classif_A_03-04-16_mammals.ivc");
		tree = NAryTreeLoader.loadTree(xmlFile);
		System.out.println("Loaded Tree of depth: " + tree.getDepth(false) + " and node count: " + tree.getNodeCount(false));

		calculateIciclePlot();
		for(String s:rankTypes) {
			ranks.add(s);
		}
	}

	public static void main(String[] args) {
		HybridIcicleCladogram viewer = new HybridIcicleCladogram();
		viewer.showOnScreen();
	}


	protected void calculateIciclePlot(){
		NAryTreeNode root = (NAryTreeNode) tree.getRoot();
		
		int tileWidth = getSimulatedDisplay().getTileXResolution();
		int tileHeight = getSimulatedDisplay().getTileYResolution();
		int tileCountX = getSimulatedDisplay().getXTiles();
		int tileCountY = getSimulatedDisplay().getYTiles();
		
		int icicleLevelHeight = (tileHeight * tileCountY) / (tree.getDepth(false) + 1);
		
		root.setNodeSize(tileWidth * tileCountX, icicleLevelHeight);
		root.setXPosition(0);
		root.setYPosition(0);

		traditionalLayout(root);
		
		System.out.println("Tree Layout calculated");
	}



	protected void traditionalLayout(NAryTreeNode node){

		//weight by subtree size
		NAryTreeNode parent = (NAryTreeNode) node.getParent();

		if(parent != null){

			double totalWeight = parent.getChildCount();
			for(int i = 0; i < parent.getChildCount(); ++i){
				NAryTreeNode sibling = (NAryTreeNode)parent.getChildAt(i);
				totalWeight+=sibling.getSubTreeSize();
			}

			//if(totalWeight == 0) totalWeight = 1.0;

			node.setWeight((node.getSubTreeSize() + 1.0)/ totalWeight );

			int index = parent.getIndex(node);
			int level = parent.getLevel();

			node.setWidth(parent.getWidth() * node.getWeight());
			node.setHeight(parent.getHeight());


			if(index == 0){
				node.setXPosition(parent.getXPosition());
				node.setYPosition(parent.getYPosition() + parent.getHeight());
			}
			else{
				NAryTreeNode leftSibling = (NAryTreeNode) parent.getChildAt(index - 1);

				node.setXPosition(leftSibling.getXPosition() + leftSibling.getWidth());
				node.setYPosition(leftSibling.getYPosition());
			}
		}

		for(int i = 0; i < node.getChildCount(); ++i){
			traditionalLayout((NAryTreeNode) node.getChildAt(i));
		}



	}
	
	/*
	public void drawVerticalLabel(){
		g2d.translate(x, y);
		g2d.rotate(-Math.PI/2);
		g2d.drawString(text, 0,0);
		g2d.rotate(Math.PI/2);
		g2d.translate(-x, -y);
	} */



	@Override
	public void drawNearGraphics(Graphics2D g) {

		
		
		if(colorType == COLOR_BY_LEVEL) {
			ctable = ColourTable.getPresetColourTable(ColourTable.GREENS ,0, tree.getDepth(false));
		}
		else if(colorType == COLOR_BY_RANK) {
			ctable = ColourTable.getPresetColourTable(ColourTable.SET3_12,0,rankTypes.size());
		}
		
		// TODO Auto-generated method stub
		int w = getWallWidth();
		int h = getWallHeight();

		// fill the background (important)
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);

		
		// TODO Auto-generated method stub
		drawNearNode(g, (NAryTreeNode) tree.getRoot(), ctable);
	}
	
	
	
	public void drawNearNode(Graphics2D g, NAryTreeNode node, ColourTable ctable) {
		
		if(node.getWeight() < 0 || node.getHeight() < 0 || node.getWidth() < 0 ) return;
		
		g.setColor(Color.BLACK);
		
		int diameter = (int) Math.min(node.getWidth(), node.getHeight());
		diameter = (int) (0.5 * diameter);
		
		int xpos = (int)(node.getXPosition() + 0.5 * node.getWidth());
		int ypos = (int)(node.getYPosition() + 0.5 * node.getHeight());
		g.fillOval(xpos - (int)(diameter * 0.5), (int)(ypos - diameter * 0.5), diameter, diameter);
		
		g.setStroke(new BasicStroke(1.0f));
		
		if(node.getID() != "root") {
			int nodeIndex = node.getParent().getIndex(node);
			
			if(nodeIndex == 0 || (nodeIndex == node.getParent().getChildCount() - 1)) {
				NAryTreeNode parent = (NAryTreeNode) node.getParent();
				int parentCenterX = (int)(parent.getXPosition() + parent.getWidth() * 0.5);
				int parentCenterY = (int)(parent.getYPosition() + parent.getHeight() * 0.5);
				g.drawLine(xpos, ypos, xpos, parentCenterY );
				g.drawLine(xpos, parentCenterY,parentCenterX,parentCenterY);
			}
			else {
				NAryTreeNode parent = (NAryTreeNode) node.getParent();
				int parentCenterY = (int)(parent.getYPosition() + parent.getHeight() * 0.5);
				g.drawLine(xpos, ypos, xpos, parentCenterY );
			}
		}
		/*
		
		
		Font labelFont = new Font("SansSerif", Font.PLAIN, 500 - node.getLevel() * 10);
		g.setFont(labelFont);
		
		
		if(colorType == COLOR_BY_RANK) {
			g.setColor(new Color(ctable.findColour(ranks.indexOf(node.getType())),false));
		}
		else if(colorType == COLOR_BY_LEVEL) {
			g.setColor(new Color(ctable.findColour(tree.getDepth(false) - node.getLevel()),false));
		}
		
		g.setStroke(new BasicStroke(100-node.getLevel() * 10));
		
		g.drawRect((int)node.getXPosition(), (int)node.getYPosition(), (int)node.getWidth(), (int)node.getHeight());
		
		String label = node.getLabel();
		
		
		boolean fits = false;
		while(!fits) {
			FontMetrics metrics = g.getFontMetrics(labelFont);
			double stringWidth = metrics.getStringBounds(label,g).getWidth();
			if(stringWidth < node.getWidth()) {
				fits = true;
			}
			
			else {
				labelFont = new Font("SansSerif",Font.PLAIN,labelFont.getSize()-1);
				g.setFont(labelFont);
			}
			
			if(labelFont.getSize() == 0) break;
		}
		
		FontMetrics metrics = g.getFontMetrics(labelFont);
		int size = metrics.stringWidth(label);
		
		
		
		g.drawString(node.getLabel(),(int)( node.getXPosition() + 0.5 * node.getWidth() - 0.5 * size),(int)(node.getYPosition() + 0.5 * node.getHeight()));
	*/
		for(int i = 0; i < node.getChildCount(); ++i){
			drawNearNode(g, (NAryTreeNode) node.getChildAt(i),ctable);
		}
	}
	
	protected void drawFarNode(Graphics2D g, NAryTreeNode node, ColourTable ctable) {
		if(node.getWeight() < 0 || node.getHeight() < 0 || node.getWidth() < 0) return;
		
		if(node.getWidth() > 4) {
			g.setColor(Color.BLACK);
			g.drawRect((int)node.getXPosition(), (int)node.getYPosition(), (int)node.getWidth(), (int)node.getHeight());
		}
		
		if(colorType == COLOR_BY_RANK) {
			g.setColor(new Color(ctable.findColour(ranks.indexOf(node.getType())),false));
		}
		else if(colorType == COLOR_BY_LEVEL) {
			g.setColor(new Color(ctable.findColour(tree.getDepth(false) - node.getLevel()),false));
		}
		g.fillRect((int)node.getXPosition(), (int)node.getYPosition(), (int)node.getWidth(), (int)node.getHeight());

		for(int i = 0; i < node.getChildCount(); ++i){
			drawFarNode(g, (NAryTreeNode) node.getChildAt(i),ctable);
		}
	}

	@Override
	public void drawFarGraphics(Graphics2D g) {
		// TODO Auto-generated method stub

		// TODO Auto-generated method stub
		int w = getWallWidth();
		int h = getWallHeight();

		// fill the background (important)
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		
		if(colorType == COLOR_BY_LEVEL) {
			ctable = ColourTable.getPresetColourTable(ColourTable.ORANGES ,0, tree.getDepth(false));
		}
		else if(colorType == COLOR_BY_RANK) {
			ctable = ColourTable.getPresetColourTable(ColourTable.SET3_12,0,rankTypes.size());
		}
		
		drawFarNode(g, (NAryTreeNode) tree.getRoot(), ctable);

	}

}
