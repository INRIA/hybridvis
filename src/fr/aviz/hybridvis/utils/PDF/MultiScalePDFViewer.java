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

package fr.aviz.hybridvis.utils.PDF;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Extracts pages and Hierarchical structure from PDFs.
 * 
 * reads any PDF in dir data/pdf/read/, process them and adds them on a
 * wall-sized image
 * 
 * Options: debug prints intermediate images, blur blurs PDF pages and titles,
 * combine combines PDF pages and titles in a single image
 * 
 * In java2D until I can fix the applet memory issues
 * 
 * @author anab
 * 
 *         TODO: code needs cleaning after options are debugged TODO: treat some
 *         Exceptions coming from PDFbox
 */

public class MultiScalePDFViewer {

	// // Wall display info ////

	// wall arrangement
	public static final int SCREEN_COUNT_X = 8;
	public static final int SCREEN_COUNT_Y = 4;

	// wall tile size
	public static final int RESOLUTION_X = 2560;
	public static final int RESOLUTION_Y = 1600;

	// wall image size
	private static int tempWidth = RESOLUTION_X * SCREEN_COUNT_X;
	private static int tempHeight = RESOLUTION_Y * SCREEN_COUNT_Y;

	// // Wall sized image generated info ////

	// wall image container
	private static BufferedImage wallImg; /*
										 * where to draw next in wallImg (layout
										 * not tight). nextY always a multiple
										 * of RESOLUTION_Y. nextX calculated
										 * depending on width of image to right
										 */
	private static int nextX, nextY;

	protected static float opacity = 1.0f;

	// produce hybrid images for Pierre's filtering
	protected static boolean hybrid = true;
	public static BufferedImage closeImg;
	public static BufferedImage farImg;

	// // where to read and write files ////
	public static String readDir; // where to read PDFs from
	public static String debugDir; // where to put debug images

	// rendering options
	boolean autofitMode = true;
	boolean zoomed_in = false;
	boolean tiles = false;
	int canvasWidth, canvasHeight;
	Rectangle2D.Double wallWinBounds = new Rectangle2D.Double();

	// // Current PDF being processed info ////
	String PDFname; // PDF file name being read
	String debugImgName; // debug out of images being processed
	List<PDPage> pages; // pdf pages
	Map<PDPage, List<String>> pageTitles = new HashMap(); // titles
															// per page
	Font itemFont = new Font("Arial", Font.BOLD, 24);

	public static boolean titles_on = true;
	public static boolean clouds_on = false;

	PDDocument document;
	PDFTextStripper PDFts;

	// Window Rendering //
	public MultiScalePDFViewer() {
	}

	public void fitWallToCanvas() {
		double scalex = canvasWidth / (double) tempWidth;
		double scaley = canvasHeight / (double) tempHeight;
		if (scalex < scaley)
			wallWinBounds.setRect(0, (canvasHeight - tempHeight * scalex) / 2,
					canvasWidth, tempHeight * scalex);
		else
			wallWinBounds.setRect((canvasWidth - tempWidth * scaley) / 2, 0,
					tempWidth * scaley, canvasHeight);
	}

	public void zoom(int wallx, int wally, int winx, int winy) {
		wallWinBounds
				.setRect(winx - wallx, winy - wally, tempWidth, tempHeight);
	}

	public double xWallToWin(double x) {
		return x * wallWinBounds.width / tempWidth + wallWinBounds.x;
	}

	public double wWallToWin(double w) {
		return w * wallWinBounds.width / tempWidth;
	}

	public double yWallToWin(double y) {
		return y * wallWinBounds.height / tempHeight + wallWinBounds.y;
	}

	public double hWallToWin(double h) {
		return h * wallWinBounds.height / tempHeight;
	}

	public double xWinToWall(double x) {
		return (x - wallWinBounds.x) / wallWinBounds.width * tempWidth;
	}

	public double yWinToWall(double y) {
		return (y - wallWinBounds.y) / wallWinBounds.height * tempHeight;
	}

	// PDF Processing constructors //
	public MultiScalePDFViewer(String name) {
		init(name, name);
	}

	public MultiScalePDFViewer(String name, String debugName) {
		init(name, debugName);
	}

	private void init(String name, String debugName) {
		PDFname = name;
		debugImgName = debugName;

		try {
			processPDF(PDFname);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param PDFname
	 *            Calls all the extraction functions for a PDF of name PDFname
	 */
	protected void processPDF(String PDFname) throws IOException {

		// load a pdf
		document = PDDocument.load(PDFname);
		PDFts = new PDFTextStripper();

		// extract pages, images, hierarchical info, text
		pdfGetStructure_PDFbox(document);

		pdf2png_PDFbox(document);
		// pdfGetImages_PDFbox(document);

		document.close();

	}

	/**
	 * @param PDDocument
	 *            Extracts each page in a PDF and saves it into a PNG image (PDF
	 *            passed as PDDocument)
	 */
	protected void pdf2png_PDFbox(PDDocument document) throws IOException {
		pages = document.getDocumentCatalog().getAllPages();
		System.out.println(PDFname + ": Total pages: " + pages.size());
		int pageCounter = 0;

		Iterator iter = pages.iterator();
		while (iter.hasNext()) {
			// get next page
			PDPage page = (PDPage) iter.next();
			++pageCounter;

			BufferedImage bImg = null;
			// generate an image from the page
			try {
				bImg = page.convertToImage();
			} catch (NullPointerException e) {
				System.err.println("Cannot convert Page to Img");
			}

			// draw into wallImg
			addOnWallImg(bImg, page);
		}
	}

	/**
	 * @param BufferedImage
	 *            src
	 * 
	 *            Add src to the big Wall Image at the next available locations
	 *            after the global nextX, nextY
	 */
	private void addOnWallImg(BufferedImage src) {
		hybrid = false;
		addOnWallImg(src, null);
	}

	private void addOnWallImg(BufferedImage src, PDPage key) {
		int w = src.getWidth();
		int h = src.getHeight();

		// current wall column
		int curX = (int) Math.floor((double) nextX / (double) RESOLUTION_X);

		if (nextX + w > tempWidth) {
			// go to next screen row
			nextX = 0;
			nextY += RESOLUTION_Y;
		}

		if (nextX + w > (curX + 1) * RESOLUTION_X) {
			if (w <= RESOLUTION_X) {
				// small image, but does not fit in current screen
				nextX = (curX + 1) * RESOLUTION_X;
			}
			// else w bigger than screen width, fit as you can
		}

		if (nextY > tempHeight) {
			// well, no more space ...
			return;
		}

		if (hybrid) {
			// close
			int pn = findPageNumber(document, key);

			System.out.println("Printing content in p. " + pn);

			if (closeImg == null)
				System.err.println("Image closeImg is null");

			Graphics2D g = closeImg.createGraphics();
			g.drawImage(src, nextX, nextY, w, h, null);

			// far, get titles and draw them
			BufferedImage itemImage;
			int titlesInPage = 0;
			List<String> titles = pageTitles.get(key);

			if (titles != null) {
				titlesInPage = titles.size();
				System.out.println("Printing titles in p. " + pn);
			}
			// local coords for titles in one page
			int pw = (int) key.getArtBox().getWidth() - 100;
			int ph = (int) key.getArtBox().getHeight() - 100;
			int px = (int) key.getArtBox().getLowerLeftX() + 50;
			int py = (int) key.getArtBox().getLowerLeftY() + 50;

			itemImage = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);

			if (titles_on && clouds_on) {
				itemImage = printTitlesAndCloudInImage(key, pn, titles,
						itemFont);
			} else if (titles_on) {
				if (titles != null)
					itemImage = printTitlesInImage(key, titles, itemFont);
			} else if (clouds_on) {
				itemImage = printCloudInImage(key, pn, titles, itemFont);
			}

			Graphics2D g2 = farImg.createGraphics();
			g2.drawImage(itemImage, nextX, nextY, w, h, null);
			g.dispose();
		}
		// } else {
		// Graphics2D g = wallImg.createGraphics();
		// g.drawImage(src, nextX, nextY, w, h, null);
		// g.dispose();
		// }

		nextX += w;
	}

	/**
	 * @param PDDocument
	 *            Extracts the PDF's structure (up to 2 levels) if available
	 *            (PDF passed as PDDocument). Create an image of the structure
	 *            for each of the 1st level titles
	 */
	protected void pdfGetStructure_PDFbox(PDDocument document)
			throws IOException {

		PDDocumentOutline root = document.getDocumentCatalog()
				.getDocumentOutline();
		PDOutlineItem item;

		try {
			item = root.getFirstChild();
		} catch (NullPointerException e) {
			System.out.println("No structure for pdf " + PDFname);
			return;
		}

		// fill map with titles per page
		while (item != null) {
			int pageNumber = findPageNumber(document,
					item.findDestinationPage(document));

			String conc = item.getTitle();
			if (conc.length() > 13)
				conc = conc.subSequence(0, 10) + "...";

			System.out.println("Item:" + conc + " at page " + pageNumber);

			if (pageTitles.containsKey(item.findDestinationPage(document)))

				pageTitles.get(item.findDestinationPage(document)).add(conc);
			else {
				pageTitles.put(item.findDestinationPage(document),
						new ArrayList<String>());
				pageTitles.get(item.findDestinationPage(document)).add(conc);
			}

			// do nothing with 2nd level children
			PDOutlineItem child = item.getFirstChild();
			while (child != null) {
				System.out.println("    Child:" + child.getTitle());
				child = child.getNextSibling();
			}
			item = item.getNextSibling();
		}

		int pn = 0;
		if (!hybrid) {
			BufferedImage itemImage;
			for (PDPage key : pages) {
				// for (PDPage key : pageTitles.keySet()) {
				++pn;

				int titlesInPage = 0;
				List<String> titles = null;
				if (pageTitles.containsKey(key)) {
					titles = pageTitles.get(key);
					titlesInPage = titles.size();
				}

				int w = (int) key.getArtBox().getWidth() - 200;
				int h = (int) key.getArtBox().getHeight() - 100;
				;
				int x = (int) key.getArtBox().getLowerLeftX() + 50;
				int y = (int) key.getArtBox().getLowerLeftY() + 50;

				// calling createGraphics() to get the Graphics2D and setup for
				// drawing titles
				itemImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

				if (titles_on && clouds_on) {
					itemImage = printTitlesAndCloudInImage(key, pn, titles,
							itemFont);
				} else if (titles_on) {
					if (titles != null)
						itemImage = printTitlesInImage(key, titles, itemFont);
				} else if (clouds_on) {
					itemImage = printCloudInImage(key, pn, titles, itemFont);
				}
			}

		}

	}

	private BufferedImage printTitlesInImage(PDPage key, List<String> titles,
			Font itemFont) {

		int titlesInPage = titles.size();

		// assuming dimensions of PDF page for drawing
		int w = (int) key.getMediaBox().getWidth() - 200;
		int h = (int) key.getMediaBox().getHeight() - 100;
		int x = (int) key.getArtBox().getLowerLeftX() + 50;
		int y = (int) key.getArtBox().getLowerLeftY() + 50;

		// no idea why this is needed
		w *= 2;
		h *= 2;

		BufferedImage itemImage;
		if (!hybrid) {
			// calling createGraphics() to get the Graphics2D and setup for
			// drawing titles. Completely transparent
			itemImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration()
					.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		} else {
			itemImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		Graphics2D g = itemImage.createGraphics();
		int currentTitle = titlesInPage;

		// background of title image transparent
		g.setColor(new Color(0f, 0f, 0f, opacity));
		for (String title : titles) {
			itemFont = scaleFontToFit(title, w - 100, g, itemFont);
			g.setFont(itemFont);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

			g.drawString(title, (float) x, (float) (y + (titlesInPage
					- currentTitle + 1)
					* h / (titlesInPage + 1)));
			--currentTitle;
		}
		// releasing resources
		g.dispose();

		return itemImage;

	}

	private BufferedImage printTitlesAndCloudInImage(PDPage key, int pageNo,
			List<String> titles, Font itemFont) {

		// assuming dimensions of PDF page for drawing
		int w = (int) key.getMediaBox().getWidth() - 200;
		int h = (int) key.getMediaBox().getHeight() - 100;
		int x = (int) key.getArtBox().getLowerLeftX() + 50;
		int y = (int) key.getArtBox().getLowerLeftY() + 50;

		// no idea why this is needed
		w *= 2;
		h *= 2;

		BufferedImage itemImage;
		if (!hybrid) {
			// calling createGraphics() to get the Graphics2D and setup for
			// drawing titles. Completely transparent
			itemImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration()
					.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		} else {
			itemImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		int originalFont = itemFont.getSize();

		Graphics2D g = itemImage.createGraphics();

		// background of title image transparent
		g.setColor(new Color(1f, 1f, 1f, 0f));
		g.fillRect(0, 0, w, h);
		g.setColor(new Color(0f, 0f, 0f, opacity));

		if (titles != null) {
			int titlesInPage = titles.size();
			int currentTitle = titlesInPage;

			for (String title : titles) {
				itemFont = scaleFontToFit(title, w - 100, g, itemFont);
				g.setFont(itemFont);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

				g.drawString(title, x, (float) ((titlesInPage
						- currentTitle + 1)
						* h * .5 / (titlesInPage + 1)));
				--currentTitle;
			}
		} else {
			System.out.println("PAGE NO TITLE");
		}

		String pageText;
		StringWriter pageTextWriter = new StringWriter();

		System.out.println("Printing text in p. " + pageNo);

		try {
			PDFts.setStartPage(pageNo);
			PDFts.setEndPage(pageNo);
			PDFts.writeText(document, pageTextWriter);
			pageText = pageTextWriter.toString();

			itemFont = itemFont.deriveFont(originalFont);
			g.setFont(itemFont);
			TagCloudImageGenerator tcig = new TagCloudImageGenerator(pageText,
					null, w, h / 2);

			int tw = tcig.getCloudImage().getWidth();
			int th = tcig.getCloudImage().getHeight();

			int y_drawing = h / 2 - 100;
			int x_drawing = 50;
			if (tw < w)
				x_drawing += .5 * (w - tw);

			if (titles == null)
				y_drawing = (int) (h / 4.0);

			g.drawImage(tcig.getCloudImage(), x_drawing, y_drawing, w,
					(h / 2), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// releasing resources
		g.dispose();

		return itemImage;
	}

	private BufferedImage printCloudInImage(PDPage key, int pageNo,
			List<String> titles, Font itemFont) {

		// assuming dimensions of PDF page for drawing
		int w = (int) key.getMediaBox().getWidth() - 200;
		int h = (int) key.getMediaBox().getHeight() - 100;
		int x = (int) key.getArtBox().getLowerLeftX() + 50;
		int y = (int) key.getArtBox().getLowerLeftY() + 50;

		// no idea why this is needed
		w *= 2;
		h *= 2;

		BufferedImage itemImage;
		if (!hybrid) {
			// calling createGraphics() to get the Graphics2D and setup for
			// drawing titles. Completely transparent
			itemImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration()
					.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		} else {
			itemImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		int originalFont = itemFont.getSize();

		Graphics2D g = itemImage.createGraphics();

		// background of title image transparent
		g.setColor(new Color(1f, 1f, 1f, 0f));
		g.fillRect(0, 0, w, h);
		g.setColor(new Color(0f, 0f, 0f, opacity));

		String pageText;
		StringWriter pageTextWriter = new StringWriter();

		System.out.println("Printing text in p. " + pageNo);

		try {
			PDFts.setStartPage(pageNo);
			PDFts.setEndPage(pageNo);
			PDFts.writeText(document, pageTextWriter);
			pageText = pageTextWriter.toString();

			itemFont = itemFont.deriveFont(originalFont);
			g.setFont(itemFont);
			TagCloudImageGenerator tcig = new TagCloudImageGenerator(pageText,
					null, w, 3 * h / 4);

			int tw = tcig.getCloudImage().getWidth();
			int th = tcig.getCloudImage().getHeight();

			int y_drawing = h / 4 - 100;
			int x_drawing = 50;
			if (tw < w)
				x_drawing += .5 * (w - tw);

			g.drawImage(tcig.getCloudImage(), x_drawing, y_drawing, w,
					3 * h / 4, 0, 0, tcig.getCloudImage().getWidth(), tcig
							.getCloudImage().getHeight(), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// releasing resources
		g.dispose();

		return itemImage;
	}

	private int findPageNumber(PDDocument doc, PDPage page) {
		int pageCounter = 0;

		Iterator iter = doc.getDocumentCatalog().getAllPages().iterator();
		while (iter.hasNext()) {
			// get next page
			PDPage current_page = (PDPage) iter.next();
			++pageCounter;

			if (current_page.equals(page))
				return pageCounter;
		}
		return -1;
	}

	/**
	 * Scales text of font pFont to a new font size so that it takes up width on
	 * the current g context
	 */
	private Font scaleFontToFit(String text, float width, Graphics g, Font pFont) {
		float fontSize = pFont.getSize();
		float fWidth = g.getFontMetrics(pFont).stringWidth(text);

		fontSize = (width / fWidth) * fontSize;
		return pFont.deriveFont(fontSize);
	}

	/**
	 * @param PDDocument
	 *            Extracts images embedded in a PDF and saves them in PNG
	 *            format, problem with embedded PDF images (PDF passed as
	 *            PDDocument)
	 */
	protected void pdfGetImages_PDFbox(PDDocument document) throws IOException {
		List<PDPage> pages = document.getDocumentCatalog().getAllPages();

		int pageCounter = 0;
		for (PDPage page : pages) {
			++pageCounter;

			// get pdf resources
			PDResources resources = page.getResources();
			Map<String, PDXObjectImage> imageResources = resources.getImages();
			resources.getGraphicsStates();

			System.out.println(resources.getImages().size()
					+ " images to be extracted");

			// int imageCounter = 0;
			for (String key : imageResources.keySet()) {
				PDXObjectImage objectImage = imageResources.get(key);
				System.out.printf("image key '%s': %d x %d, type %s%n", key,
						objectImage.getHeight(), objectImage.getWidth(),
						objectImage.getSuffix());
			}
		}
	}

	/*
	 * Initialization when methods used outside the class
	 */
	public void initialize(int W, int H) {

		hybrid = true;
		opacity = 1;

		titles_on = true;
		clouds_on = false;

		tempWidth = W;
		tempHeight = H;
		System.out.println("MS_PDF_Viewer Setup for wall size (" + W + "," + H
				+ ")");

		closeImg = new BufferedImage(tempWidth, tempHeight,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = closeImg.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, closeImg.getWidth(), closeImg.getHeight());

		farImg = new BufferedImage(tempWidth, tempHeight,
				BufferedImage.TYPE_INT_ARGB);
		// clear far image to white
		Graphics2D g2 = farImg.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, farImg.getWidth(), farImg.getHeight());

		int PDFcounter = 0;
		String currentPDFname;
		File dir = new File(readDir);
		File[] flist = dir.listFiles();
		String className = "";

		for (; PDFcounter < flist.length; PDFcounter++) {
			String newline = System.getProperty("line.separator");
			String nm = ".pdf";
			if (flist[PDFcounter].getName().toLowerCase()
					.endsWith(nm.toLowerCase())) {
				if (flist[PDFcounter].isFile()) {
					currentPDFname = flist[PDFcounter].getName();
					MultiScalePDFViewer myPDF = new MultiScalePDFViewer(readDir
							+ currentPDFname, debugDir + currentPDFname);
					className = myPDF.getClass().getName();
				}
			}
		}

	}
}
