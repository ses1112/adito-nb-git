package de.adito.git.gui.dialogs.panels.TextPanes;

import de.adito.git.api.data.*;
import de.adito.git.gui.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import java.util.List;

/**
 * @author m.kaspera, 13.12.2018
 */
public class ForkPointPaneWrapper implements IDiscardable
{

  private final JScrollPane textScrollPane;
  private final JEditorPane textPane;
  private final IMergeDiff mergeDiff;
  private final Disposable disposable;
  private final _PaneDocumentListener paneDocumentListener = new _PaneDocumentListener();
  private int caretPosition;

  public ForkPointPaneWrapper(IMergeDiff pMergeDiff)
  {
    mergeDiff = pMergeDiff;
    textPane = new NonWrappingTextPane();
    textScrollPane = new JScrollPane(textPane);
    // disable manual text input for now, also no need for document listener as long as textPane not editable
    textPane.setEditable(false);
    //textPane.getDocument().addDocumentListener(paneDocumentListener);
    disposable = Observable.zip(
        mergeDiff.getDiff(IMergeDiff.CONFLICT_SIDE.YOURS).getFileChanges().getChangeChunks(),
        mergeDiff.getDiff(IMergeDiff.CONFLICT_SIDE.THEIRS).getFileChanges().getChangeChunks(), _ListPair::new)
        .subscribe(this::_refreshContent);
  }

  public JScrollPane getPane()
  {
    return textScrollPane;
  }

  public JEditorPane getTextPane()
  {
    return textPane;
  }

  private void _refreshContent(_ListPair pChangeChunkLists)
  {
    if (pChangeChunkLists.doUpdate)
    {
      paneDocumentListener.disable();
      caretPosition = textPane.getCaretPosition();
      int scrollBarVal = textScrollPane.getVerticalScrollBar().getModel().getValue();
      textScrollPane.getVerticalScrollBar().getModel().setValueIsAdjusting(true);
      // OLD because the content of the ForkPointTextPane is the version of the forkPoint (i.e. the old version in all cases since forkPoint
      // predates both commits)
      TextHighlightUtil.insertColoredText(textPane, pChangeChunkLists.yourVersion, pChangeChunkLists.theirVersion,
                                          IFileChangeChunk::getALines, pFileChangeChunk -> "");
      SwingUtilities.invokeLater(() -> {
        textPane.setCaretPosition(caretPosition);
        textScrollPane.getVerticalScrollBar().getModel().setValue(scrollBarVal);
        textScrollPane.getVerticalScrollBar().getModel().setValueIsAdjusting(false);
      });
      paneDocumentListener.enable();
    }
  }

  @Override
  public void discard()
  {
    disposable.dispose();
  }

  /**
   * DocumentListener to check for user-input in the fork-point version of the merge conflict
   * Can be manually dis-/enabled if text is input by the code, this is done by calling the
   * methods named disable/enable
   */
  private class _PaneDocumentListener implements DocumentListener
  {
    private boolean isActive = true;
    
    @Override
    public void insertUpdate(DocumentEvent pEvent)
    {
      if (isActive)
      {
        try
        {
          // get the information about what text and where before the invokeLater(), else the information can be outdated
          final String insertedText = pEvent.getDocument().getText(pEvent.getOffset(), pEvent.getLength());
          final int insertOffset = pEvent.getOffset();
          SwingUtilities.invokeLater(() -> mergeDiff.insertText(insertedText, insertedText.length(), insertOffset, true));
        }
        catch (BadLocationException e1)
        {
          throw new RuntimeException(e1);
        }
      }
    }

    @Override
    public void removeUpdate(DocumentEvent pEvent)
    {
      if (isActive)
      {
        final int removeOffset = pEvent.getOffset();
        SwingUtilities.invokeLater(() -> mergeDiff.insertText("", pEvent.getLength(), removeOffset, false));
      }
    }

    @Override
    public void changedUpdate(DocumentEvent pEvent)
    {
      // changed only triggers on metadata change which is not interesting for this use-case
    }

    /**
     * Enables the processing of events for this listener
     */
    void enable()
    {
      isActive = true;
    }

    /**
     * Disables the processing of events for this listener. Acts as if the listener wouldn't be here if disabled
     */
    void disable()
    {
      isActive = false;
    }
  }

  private static class _ListPair
  {
    List<IFileChangeChunk> yourVersion;
    List<IFileChangeChunk> theirVersion;
    boolean doUpdate;

    _ListPair(IFileChangesEvent yourVersion, IFileChangesEvent theirVersion)
    {
      this.yourVersion = yourVersion.getNewValue();
      this.theirVersion = theirVersion.getNewValue();
      doUpdate = yourVersion.isUpdateUI() && theirVersion.isUpdateUI();
    }
  }
}
