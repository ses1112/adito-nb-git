package de.adito.git.gui.dialogs.panels;

import de.adito.git.gui.IDiscardable;
import de.adito.git.gui.TextHighlightUtil;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import javax.swing.*;
import java.util.Collections;

/**
 * @author m.kaspera 13.12.2018
 */
public class LineNumPanel implements IDiscardable
{

  private final JTextPane lineNumPane = new JTextPane();
  private final JScrollPane lineNumberingScrollPane = new JScrollPane(lineNumPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  private final Disposable disposable;

  LineNumPanel(DiffPanelModel pModel)
  {
    lineNumberingScrollPane.setBorder(null);
    lineNumPane.setEnabled(false);
    disposable = pModel.getFileDiff().switchMap(pFileDiff -> pFileDiff
        .map(pDiff -> pDiff.getFileChanges().getChangeChunks())
        .orElse(Observable.just(Collections.emptyList()))).subscribe(
        pFileChanges -> TextHighlightUtil.insertColoredLineNumbers(lineNumPane, pFileChanges, pModel.getGetNumLines(),
                                                                   pModel.getGetParityLines()));
  }

  JScrollPane getContentScrollPane()
  {
    return lineNumberingScrollPane;
  }

  @Override
  public void discard()
  {
    disposable.dispose();
  }
}