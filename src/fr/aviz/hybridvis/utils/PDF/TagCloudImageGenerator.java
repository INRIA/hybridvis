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
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.mcavallo.opencloud.filters.DictionaryFilter;
import org.mcavallo.opencloud.filters.Filter;
import org.mcavallo.opencloud.filters.MinLengthFilter;

import fr.aviz.hybridvis.utils.PDF.MyWordle.Word;

public class TagCloudImageGenerator {

	protected String tagCloudText;
	protected BufferedImage tagCloudImg;
	protected Font tagCloudMaxFont = new Font("Arial", Font.BOLD, 120);
	protected int tagCloudWidth;
	protected int tagCloudHeight;

	static final int maxTags = 20;
	static final double minFontRatio = .7;

	protected static String commonWordsFile = "data/commonWords.txt";

	// protected static ArrayList<String> commonWords =
	// readCommonWords(commonWordsFile);

	public TagCloudImageGenerator(String tagCloudText, Font tagCloudMaxFont,
			int tagCloudWidth, int tagCloudHeight) {
		this.tagCloudText = tagCloudText;
		this.tagCloudWidth = tagCloudWidth;
		this.tagCloudHeight = tagCloudHeight;

		if (tagCloudMaxFont != null)
			this.tagCloudMaxFont = tagCloudMaxFont;

		ArrayList<String> tagCloudWords = splitText(tagCloudText);
		lowerCase(tagCloudWords);
		removeCommonWords(tagCloudWords, commonWordsFile);

		createCloud(tagCloudWords);

		System.out.println("	Generating cloud of " + tagCloudWords.size()
				+ " words");
	}

	/*
	 * 2 Functions to read a file into a string array. Used for storing
	 * commonWords
	 */
	static ArrayList<String> readCommonWords(String filename) {
		try {
			return readLines(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static ArrayList<String> readLines(String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		ArrayList<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			String[] tmpStrings = line.split("\\s+");
			lines.addAll(Arrays.asList(tmpStrings));
		}
		bufferedReader.close();
		return lines;
	}

	void removeCommonWords(ArrayList<String> removeFrom, String filename) {
		ArrayList<String> removeWords = null;
		try {
			removeWords = readLines(filename);
			System.out.println(removeWords.size() + " words to remove");
		} catch (IOException e) {
			e.printStackTrace();
		}

		removeDuplicates(removeFrom, removeWords);
	}

	void removeDuplicates(ArrayList<String> A, ArrayList<String> B) {
		A.removeAll(B);
	}

	public static void lowerCase(List<String> strings) {
		ListIterator<String> iterator = strings.listIterator();
		while (iterator.hasNext()) {
			iterator.set(iterator.next().toLowerCase());
		}
	}

	/*
	 * Uses RegExpression split text into an array of words. Split is done by
	 * \\s+ whitespace chars, \W nonword character, \d digits
	 */
	ArrayList<String> splitText(String s) {
		if (!s.isEmpty()) {
			return new ArrayList<String>(Arrays.asList(s.split("[\\s\\W\\d]+")));
		} else
			return null;
	}

	/*
	 * Creates a filter that remove all occurences of words in file STOPWORDS
	 */
	protected Filter<Tag> getStopWordFilter(String STOPWORDS) {
		Filter<Tag> stopwords = null;
		try {
			stopwords = new DictionaryFilter(new FileInputStream(STOPWORDS));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stopwords;
	}

	void createCloud(ArrayList<String> tags) {
		Cloud cloud = new Cloud();
		cloud.setTagCase(Cloud.Case.LOWER);

		// // dictionary filter does not work
		// Filter<Tag> dicFilter = getStopWordFilter(commonWordsFile);
		// Filter<Tag> sizeFilter = new MinLengthFilter(3);
		// Filter<Tag> orFilter = new OrFilter<Tag>(dicFilter, sizeFilter);
		// // setup a dictionary of words to ignore, and words of size 2 or less
		// cloud.addInputFilter(orFilter);
		cloud.addInputFilter(new MinLengthFilter(3));

		// score threshold bellow which we do not show tags
		cloud.setThreshold(4.0);
		cloud.setRounding(Cloud.Rounding.ROUND); // rounds weights to ints
		cloud.setMaxTagsToDisplay(maxTags);
		cloud.setMaxWeight(maxTags);

		for (String s : tags)
			cloud.addTag(s);

		for (Tag pt : cloud.tags(new Tag.ScoreComparatorDesc()))
			System.out.print(" " + pt.getName() + ":" + pt.getWeight() + ":"
					+ pt.getScore() + " ");
		System.out.println("... done");

		int allTags = cloud.getMaxTagsToDisplay();
		System.out.println("Printing " + allTags);

		double minTagWeight = cloud.getMinWeight();
		double maxTagWeight = cloud.getMaxWeight();

		if (tagCloudMaxFont == null)
			System.out.println("Oops font problem");
		double fontIncrement = (1 - minFontRatio) * tagCloudMaxFont.getSize()
				/ (maxTagWeight - minTagWeight);

		System.out.println("MinW:" + minTagWeight + "MaxW:" + maxTagWeight
				+ " fontInc:" + fontIncrement);

		// BufferedImage img = new BufferedImage(tagCloudWidth, tagCloudHeight,
		// BufferedImage.TYPE_INT_ARGB);
		//
		// Graphics2D g = img.createGraphics();
		// g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		// RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		//
		// g.setColor(new Color(1f, 1f, 1f, 0f));
		// g.fillRect(0, 0, tagCloudWidth, tagCloudHeight);
		// g.setColor(new Color(0f, 0f, 0f, 1f));
		//
		//
		// double x = 0;
		// double y = (g.getFontMetrics(tagCloudMaxFont).getHeight());
		// double max_y = 0;
		//
		// for (Tag t : cloud.tags(new Tag.ScoreComparatorDesc())) {
		// Font f = tagCloudMaxFont
		// .deriveFont((float) (minFontRatio
		// * tagCloudMaxFont.getSize() + (t.getWeight() - minTagWeight)
		// * fontIncrement));
		// g.setFont(f);
		//
		// max_y = Math.max(max_y, g.getFontMetrics(f).getHeight());
		// if (x + g.getFontMetrics(f).stringWidth(t.getName() + " ") >
		// tagCloudWidth) {
		// x = 0;
		// y += max_y;
		// max_y = 0;
		// }
		//
		// g.drawString(t.getName() + " ", (float) x, (float) (y + g
		// .getFontMetrics(f).getHeight()));
		// x += g.getFontMetrics(f).stringWidth(t.getName() + " ");
		// }
		//
		// // releasing resources
		// g.dispose();
		//
		// tagCloudImg = img;

		MyWordle mw = new MyWordle();
		mw.setFontFaminly(tagCloudMaxFont.getFamily());
		mw.setBigSize(tagCloudMaxFont.getSize());
		mw.setSmallSize((int) (minFontRatio * tagCloudMaxFont.getSize()));
		mw.setUseArea(true);

		int counter = 0;
		for (Tag t : cloud.tags(new Tag.ScoreComparatorDesc())) {

			if (counter > 10)
				break;
			++counter;
			Word w = new Word(t.getName(), t.getWeightInt());
			w.setFill(Color.black);
			w.setStroke(Color.black);
			mw.add(w);
		}

		mw.doLayout();
		tagCloudImg = mw.returnImg();
	}

	public BufferedImage getCloudImage() {
		return tagCloudImg;
	}
}
