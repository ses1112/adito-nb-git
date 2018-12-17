package de.adito.git.gui;

import de.adito.git.api.data.EChangeSide;
import de.adito.git.api.data.EChangeType;
import de.adito.git.api.data.IFileChangeChunk;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Handles inserting lists of IFileChangeChunks into textPanes and highlighting parts of the text
 * The highlighted parts are determined by the IFileChangeChunks, and the highlighting is done
 * in a layered manner, so text selection is still possible
 *
 * @author m.kaspera 22.10.2018
 */
public class TextHighlightUtil
{

  private TextHighlightUtil()
  {
  }

  /**
   * @param pTextPane         JTextPane that should be filled with text and colored
   * @param pFileChangeChunks IFileChangeChunks determining the text and highlighting
   * @param pGetLines         Function that retrieves the fitting String from the IFileChangeChunk (A or B side)
   * @param pGetParityLines   Function that retrieves the fitting parity lines from the IFileChangeChunk.
   *                          Return "" in the function if parity lines should be ignored
   */
  public static void insertColoredText(JTextPane pTextPane, List<IFileChangeChunk> pFileChangeChunks, Function<IFileChangeChunk, String> pGetLines,
                                       Function<IFileChangeChunk, String> pGetParityLines)
  {
    _insertColoredText(pTextPane, pFileChangeChunks, pGetLines, pGetParityLines, new ArrayList<>());
  }

  /**
   * Combines the highlighting of two List of IFileChangeChunks, text is assumed to be identical
   *
   * @param pTextPane              JTextPane that should be filled with text and colored
   * @param pYourFileChangeChunks  IFileChangeChunks determining the text and highlighting
   * @param pTheirFileChangeChunks IFileChangeChunks determining the text and highlighting
   * @param pGetLines         Function that retrieves the fitting String from the IFileChangeChunk (A or B side)
   * @param pGetParityLines   Function that retrieves the fitting parity lines from the IFileChangeChunk.
   *                          Return "" in the function if parity lines should be ignored
   */
  public static void insertColoredText(JTextPane pTextPane, List<IFileChangeChunk> pYourFileChangeChunks,
                                       List<IFileChangeChunk> pTheirFileChangeChunks,
                                       Function<IFileChangeChunk, String> pGetLines,
                                       Function<IFileChangeChunk, String> pGetParityLines)
  {
    _insertColoredText(pTextPane, pYourFileChangeChunks, pGetLines, pGetParityLines,
                       _getHighlightSpots(pTheirFileChangeChunks, pGetLines, pGetParityLines));
  }

  /**
   * @param pFileChangeChunks List of IFileChangeChunks for which the highlighted areas should be determined
   * @param pGetLines         Function that retrieves the normal lines for an IFileChangeChunk
   * @param pGetParityLines   Function that retrieves the parity lines for an IFileChangeChunk
   * @return List of _Highlight
   */
  private static List<_Highlight> _getHighlightSpots(List<IFileChangeChunk> pFileChangeChunks, Function<IFileChangeChunk, String> pGetLines,
                                                     Function<IFileChangeChunk, String> pGetParityLines)
  {
    List<_Highlight> highlightSpots = new ArrayList<>();
    int currentIndex = 0;
    int currentLen;
    for (IFileChangeChunk changeChunk : pFileChangeChunks)
    {
      currentLen = pGetLines.apply(changeChunk).length();
      currentLen += pGetParityLines.apply(changeChunk).length();
      if (changeChunk.getChangeType() != EChangeType.SAME)
      {
        highlightSpots.add(new _Highlight(currentIndex, currentIndex + currentLen,
                                          new DefaultHighlighter.DefaultHighlightPainter(changeChunk.getChangeType().getDiffColor())));
      }
      currentIndex += currentLen;
    }
    return highlightSpots;
  }

  /**
   * @param pTextPane         JTextPane that should be filled with text and colored
   * @param pFileChangeChunks List of IFileChangeChunks providing the text and the information about which areas to highlight
   * @param pGetLines         Function that retrieves the normal lines for an IFileChangeChunk
   * @param pGetParityLines   Function that retrieves the parity lines for an IFileChangeChunk
   * @param pHighlightSpots   List of _Highlight determining which areas get colored and the color of the areas
   */
  private static void _insertColoredText(JTextPane pTextPane, List<IFileChangeChunk> pFileChangeChunks, Function<IFileChangeChunk, String> pGetLines,
                                         Function<IFileChangeChunk, String> pGetParityLines, List<_Highlight> pHighlightSpots)
  {
    StringBuilder paneContentBuilder = new StringBuilder();
    int currentIndex = 0;
    int currentLen;
    for (IFileChangeChunk changeChunk : pFileChangeChunks)
    {
      paneContentBuilder.append(pGetLines.apply(changeChunk));
      currentLen = pGetLines.apply(changeChunk).length();
      paneContentBuilder.append(pGetParityLines.apply(changeChunk));
      currentLen += pGetParityLines.apply(changeChunk).length();
      if (changeChunk.getChangeType() != EChangeType.SAME)
      {
        pHighlightSpots.add(new _Highlight(currentIndex, currentIndex + currentLen,
                                           new DefaultHighlighter.DefaultHighlightPainter(changeChunk.getChangeType().getDiffColor())));
      }
      currentIndex += currentLen;
    }
    pTextPane.setText(paneContentBuilder.toString());
    _colorHighlights(pTextPane, pHighlightSpots);
  }

  /**
   * @param pLineNumberingPane JTextPane that should display the line numbers
   * @param pChangeChunkList   List of IFileChangeChunks determining the line numbers and colored areas
   * @param pChangeSide        which part of the IFileChangeChunks (A-/B-lines) should be used
   */
  public static void insertColoredLineNumbers(JTextPane pLineNumberingPane, List<IFileChangeChunk> pChangeChunkList,
                                              EChangeSide pChangeSide)
  {
    Function<IFileChangeChunk, Integer> getNumLines;
    Function<IFileChangeChunk, String> getParityLines;
    if (pChangeSide == EChangeSide.NEW)
    {
      getNumLines = pIFileChangeChunk -> pIFileChangeChunk.getBEnd() - pIFileChangeChunk.getBStart();
      getParityLines = IFileChangeChunk::getBParityLines;
    }
    else
    {
      getNumLines = pIFileChangeChunk -> pIFileChangeChunk.getAEnd() - pIFileChangeChunk.getAStart();
      getParityLines = IFileChangeChunk::getAParityLines;
    }
    insertColoredLineNumbers(pLineNumberingPane, pChangeChunkList, getNumLines, getParityLines);

  }

  /**
   * @param pLineNumberingPane JTextPane that should display the line numbers
   * @param pChangeChunkList   List of IFileChangeChunks determining the line numbers and colored areas
   * @param pGetNumLines       Function that retrieves the normal lines for an IFileChangeChunk
   * @param pGetParityLines    Function that retrieves the parity lines for an IFileChangeChunk
   */
  public static void insertColoredLineNumbers(JTextPane pLineNumberingPane, List<IFileChangeChunk> pChangeChunkList,
                                              Function<IFileChangeChunk, Integer> pGetNumLines,
                                              Function<IFileChangeChunk, String> pGetParityLines)
  {
    List<_Highlight> highlightSpots = new ArrayList<>();
    StringBuilder lineNumberingBuilder = new StringBuilder();
    int lineNum = 1;
    int currentIndex = 0;
    int numNewLines;
    int currentLen;
    for (IFileChangeChunk changeChunk : pChangeChunkList)
    {
      numNewLines = pGetNumLines.apply(changeChunk);
      String lineNums = _getLineNumString(lineNum, numNewLines);
      lineNumberingBuilder.append(lineNums);
      currentLen = lineNums.length();
      lineNum += numNewLines;
      // parity lines should only contain newlines anyway, so no filtering or counting newlines should be necessary
      lineNumberingBuilder.append(pGetParityLines.apply(changeChunk));
      currentLen += pGetParityLines.apply(changeChunk).length();
      if (changeChunk.getChangeType() != EChangeType.SAME)
      {
        highlightSpots.add(new _Highlight(currentIndex, currentIndex + currentLen,
                                          new DefaultHighlighter.DefaultHighlightPainter(changeChunk.getChangeType().getDiffColor())));
      }
      currentIndex += currentLen;
    }
    pLineNumberingPane.setText(lineNumberingBuilder.toString());
    _colorHighlights(pLineNumberingPane, highlightSpots);
  }

  /**
   * @param pStart number of the first line
   * @param pCount number of lines
   * @return String with the lineNumbers from pStart to pStart + pCount
   */
  private static String _getLineNumString(int pStart, int pCount)
  {
    StringBuilder lineNumStringBuilder = new StringBuilder();
    for (int index = 0; index < pCount; index++)
    {
      lineNumStringBuilder.append(pStart + index).append("\n");
    }
    return lineNumStringBuilder.toString();
  }

  private static void _colorHighlights(JTextPane pTextPane, List<_Highlight> pHighlightSpots)
  {
    LineHighlighter highlighter = new LineHighlighter();
    pTextPane.setHighlighter(highlighter);
    try
    {
      for (_Highlight highlight : pHighlightSpots)
      {
        highlighter.addBackgroundHighlight(highlight.getStartIndex(), highlight.getEndOffset(), highlight.getPainter());
      }
    }
    catch (BadLocationException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * class to store the diverse spots that should be highlighted after
   * the IFileChangeChunks are inserted.
   */
  private static class _Highlight
  {

    private final int startIndex;
    private final int endOffset;
    private final DefaultHighlighter.DefaultHighlightPainter painter;

    _Highlight(int startIndex, int endOffset, DefaultHighlighter.DefaultHighlightPainter painter)
    {
      this.startIndex = startIndex;
      this.endOffset = endOffset;
      this.painter = painter;
    }

    int getStartIndex()
    {
      return startIndex;
    }

    int getEndOffset()
    {
      return endOffset;
    }

    DefaultHighlighter.DefaultHighlightPainter getPainter()
    {
      return painter;
    }
  }
}
