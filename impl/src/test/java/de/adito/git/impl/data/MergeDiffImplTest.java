package de.adito.git.impl.data;

import de.adito.git.api.data.IFileChangeChunk;
import org.eclipse.jgit.diff.Edit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author m.kaspera 19.10.2018
 */
class MergeDiffImplTest
{

  private final int[] aStarts = {0, 5, 7, 15, 22, 28, 46};
  private final int[] aEnds = {5, 7, 15, 22, 28, 46, 54};
  private final String[] aLines = {"aa\nab\nac\nad\nae\n", "af\nag\n",
                                   "ah\nai\naj\nak\nal\nam\nan\nao\n", "ap\naq\nar\nas\nat\nau\nav\n",
                                   "aw\nax\nay\naz\na1\na2\n", "a3\n", "a4\n"};

  @Test
  void testInsertAffectedChunkIndizes()
  {
    final int[] aStart = {0, 4, 4, 8};
    final int[] aEnd = {4, 4, 8, 12};
    List<IFileChangeChunk> fileChangeChunkList = createFileChangeChunkList(aStart, aEnd);
    IFileChangeChunk toInsert = new FileChangeChunkImpl(new Edit(4, 4, 4, 8), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(toInsert, fileChangeChunkList);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertEquals(1, (int) affectedIndizes.get(0));
  }

  /**
   * Test if the correct chunk indizes are returned when several chunks are affected by the change
   */
  @Test
  void testAffectedChunkIndizesSeveral()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(4, 9, 4, 11), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(3, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.containsAll(Arrays.asList(0, 1, 2)));
  }

  /**
   * Test if the correct chunk index is returned when exactly one chunk is affected
   */
  @Test
  void testAffectedChunkIndizesExactlyOne()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(5, 7, 4, 12), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.contains(1));
  }

  /**
   * Test if the correct chunk index is returned when exactly one chunk is affected
   */
  @Test
  void testAffectedChunkIndizesInsertOneLine()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(5, 5, 5, 6), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.contains(1));
  }

  /**
   * Test if the correct chunk index is returned when only part of one chunk is affected
   */
  @Test
  void testAffectedChunkIndizesPartsOfOne()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(8, 10, 4, 11), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.contains(2));
  }

  /**
   * Test if the correct chunk index is returned when only part of one chunk is affected
   */
  @Test
  void testAffectedChunkIndizesBeginningPartOfOne()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(5, 6, 4, 11), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.contains(1));
  }

  /**
   * Test if the correct chunk index is returned when only part of one chunk is affected
   */
  @Test
  void testAffectedChunkIndizesStartOnBoundary()
  {
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(8, 10, 4, 11), "", "");
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    Assertions.assertEquals(1, affectedIndizes.size());
    Assertions.assertTrue(affectedIndizes.contains(2));
  }

  /**
   * Test if the added lines (and thus higher/lower line numbers) are propagated through the list if lines were added
   */
  @Test
  void testPropagateAdditionalLinesPositive()
  {
    int indexFrom = 2;
    int numAddedLines = 5;
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    MergeDiffImpl.propagateAdditionalLines(changeChunks, indexFrom, numAddedLines);
    for (int index = indexFrom; index < changeChunks.size(); index++)
    {
      Assertions.assertEquals(aStarts[index] + numAddedLines, changeChunks.get(index).getAStart());
      Assertions.assertEquals(aEnds[index] + numAddedLines, changeChunks.get(index).getAEnd());
    }
  }

  /**
   * Test if the added lines (and thus higher/lower line numbers) are propagated through the list if lines were removed
   */
  @Test
  void testPropagateAdditionalLinesNegative()
  {
    int indexFrom = 2;
    int numAddedLines = -5;
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    MergeDiffImpl.propagateAdditionalLines(changeChunks, indexFrom, numAddedLines);
    for (int index = indexFrom; index < changeChunks.size(); index++)
    {
      Assertions.assertEquals(aStarts[index] + numAddedLines, changeChunks.get(index).getAStart());
      Assertions.assertEquals(aEnds[index] + numAddedLines, changeChunks.get(index).getAEnd());
    }
  }

  /**
   * Test if the added lines (and thus higher/lower line numbers) are propagated through the list if no change occurred
   * This is almost certainly won't occur in reality, however as an edge case the reaction is tested here
   */
  @Test
  void testPropagateAdditionalLinesNeutral()
  {
    int indexFrom = 2;
    int numAddedLines = 0;
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds);
    MergeDiffImpl.propagateAdditionalLines(changeChunks, indexFrom, numAddedLines);
    for (int index = indexFrom; index < changeChunks.size(); index++)
    {
      Assertions.assertEquals(aStarts[index] + numAddedLines, changeChunks.get(index).getAStart());
      Assertions.assertEquals(aEnds[index] + numAddedLines, changeChunks.get(index).getAEnd());
    }
  }

  /**
   * Tests if the correct lines are inserted/removed when doing a replace operation that adds more lines than are removed
   */
  @Test
  void testApplyChangesReplaceMore()
  {
    String linesBeforeChange = "ae\naf\nag\nah\nai\naj\nak\n";
    String linesAfterChange = "ax\nax\nax\nax\nax\nax\nax\nax\n";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(4, 11, 4, 12), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertFalse(changedLines.toString().contains(linesBeforeChange));
    Assertions.assertEquals(originalLines.toString().split("\n").length
                                + (linesAfterChange.split("\n").length - linesBeforeChange.split("\n").length),
                            changedLines.toString().split("\n").length);
  }

  /**
   * Tests if the correct lines are inserted/removed when doing a replace operation that removes more lines than are added
   */
  @Test
  void testApplyChangesReplaceLess()
  {
    String linesBeforeChange = "ae\naf\nag\nah\nai\naj\nak\n";
    String linesAfterChange = "ax\nax\nax\nax\nax";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(4, 11, 4, 9), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertFalse(Arrays.stream(linesBeforeChange.split("\n")).anyMatch(partFromBefore -> changedLines.toString().contains(partFromBefore)));
    Assertions.assertEquals(originalLines.toString().split("\n").length
                                + (linesAfterChange.split("\n").length - linesBeforeChange.split("\n").length),
                            changedLines.toString().split("\n").length);
  }

  /**
   * Tests if the correct lines are inserted/removed when doing a replace operation that removes more lines than are added
   * in one chunk instead of two/several like in testApplyChangesReplaceLess
   */
  @Test
  void testApplyChangesReplaceLessOneChunk()
  {
    String linesBeforeChange = "ah\nai\naj\nak\n";
    String linesAfterChange = "ax\nax\nax\nax\nax";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(7, 11, 4, 9), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertFalse(Arrays.stream(linesBeforeChange.split("\n")).anyMatch(partFromBefore -> changedLines.toString().contains(partFromBefore)));
    Assertions.assertEquals(originalLines.toString().split("\n").length
                                + (linesAfterChange.split("\n").length - linesBeforeChange.split("\n").length),
                            changedLines.toString().split("\n").length);
  }

  /**
   * Tests if the correct lines are inserted/removed when doing a replace operation one one line
   */
  @Test
  void testApplyChangesReplaceLine()
  {
    String linesBeforeChange = "ae\n";
    String linesAfterChange = "ax\n";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(4, 5, 4, 5), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertFalse(changedLines.toString().contains(linesBeforeChange));
    Assertions.assertEquals(originalLines.toString().split("\n").length
                                + (linesAfterChange.split("\n").length - linesBeforeChange.split("\n").length),
                            changedLines.toString().split("\n").length);
  }

  /**
   * Tests if the correct lines are inserted/removed when doing an insert operation
   */
  @Test
  void testApplyChangesInsert()
  {
    String linesBeforeChange = "";
    String linesAfterChange = "ax\nax\nax\nax\nax\n";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(10, 10, 4, 9), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertEquals(originalLines.toString().split("\n").length + linesAfterChange.split("\n").length,
                            changedLines.toString().split("\n").length);
  }

  /**
   * Tests if the correct lines are inserted/removed when doing a delete operation
   */
  @Test
  void testApplyChangesDelete()
  {
    String linesBeforeChange = "ae\naf\nag\nah\nai\naj\nak\n";
    String linesAfterChange = "";
    StringBuilder originalLines = new StringBuilder();
    StringBuilder changedLines = new StringBuilder();
    List<IFileChangeChunk> changeChunks = createFileChangeChunkList(aStarts, aEnds, aLines);
    IFileChangeChunk changeChunk = new FileChangeChunkImpl(new Edit(4, 11, 4, 4), linesBeforeChange, linesAfterChange);
    List<Integer> affectedIndizes = MergeDiffImpl.affectedChunkIndices(changeChunk, changeChunks);
    for (Integer num : affectedIndizes)
    {
      changeChunks.set(num, MergeDiffImpl.applyChange(changeChunk, changeChunks.get(num)));
    }
    for (String originalLine : aLines)
    {
      originalLines.append(originalLine);
    }
    for (IFileChangeChunk chunk : changeChunks)
    {
      changedLines.append(chunk.getALines());
    }
    Assertions.assertTrue(changedLines.toString().contains(linesAfterChange));
    Assertions.assertFalse(changedLines.toString().contains(linesBeforeChange));
    Assertions.assertEquals(originalLines.toString().split("\n").length
                                + (0 - linesBeforeChange.split("\n").length),
                            changedLines.toString().split("\n").length);
  }

  /**
   * Helper method that created a list of FileChangeChunks, so it isn't coded separately in each test
   *
   * @param aStarts array of integers denoting the starts of the chunks
   * @param aEnds   array of integers denoting the end of the chunks
   * @return a list of IFileChangeChunks
   */
  @NotNull
  private List<IFileChangeChunk> createFileChangeChunkList(@NotNull int[] aStarts, @NotNull int[] aEnds)
  {
    List<IFileChangeChunk> changeChunks = new ArrayList<>();
    for (int index = 0; index < aStarts.length; index++)
    {
      changeChunks.add(new FileChangeChunkImpl(new Edit(aStarts[index], aEnds[index], 0, 0), "", ""));
    }
    return changeChunks;
  }

  /**
   * Helper method that created a list of FileChangeChunks, so it isn't coded separately in each test
   *
   * @param aStarts array of integers denoting the starts of the chunks
   * @param aEnds   array of integers denoting the end of the chunks
   * @param aLines  contents of the lines on the a-side (fork-point side)
   * @return a list of IFileChangeChunks
   */
  @NotNull
  private List<IFileChangeChunk> createFileChangeChunkList(@NotNull int[] aStarts, @NotNull int[] aEnds, @NotNull String[] aLines)
  {
    List<IFileChangeChunk> changeChunks = new ArrayList<>();
    for (int index = 0; index < aStarts.length; index++)
    {
      changeChunks.add(new FileChangeChunkImpl(new Edit(aStarts[index], aEnds[index], 0, 0), aLines[index], ""));
    }
    return changeChunks;
  }
}
