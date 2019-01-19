package de.adito.git.gui.dialogs.panels.BaseDiffPanel.TextPanes.DiffPane;

import de.adito.git.api.data.EChangeType;
import de.adito.git.api.data.IFileChangeChunk;
import de.adito.git.api.data.IFileChangesEvent;
import de.adito.git.gui.IDiscardable;
import de.adito.git.gui.dialogs.panels.BaseDiffPanel.DiffPanelModel;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel that contains buttons for accepting and discarding changes of a text displayed in a JTextPane
 * Position
 *
 * @author m.kaspera 13.12.2018
 */
class ChoiceButtonPanel extends JPanel implements IDiscardable
{

  private final DiffPanelModel model;
  private final ImageIcon discardIcon;
  private final ImageIcon acceptIcon;
  private final int acceptChangeIconXVal;
  private final int discardChangeIconXVal;
  private final Disposable disposable;
  private Rectangle cachedViewRectangle;
  private List<IconInfo> iconInfoList = new ArrayList<>();
  private List<IconInfo> iconInfosToDraw = new ArrayList<>();

  /**
   * @param pModel         DiffPanelModel that contains functions that retrieve information, such as start/end line, of an IFileChangeChunk
   * @param pEditorPane    EditorPane that contains the text for which the buttons should be drawn
   * @param pDisplayedArea Observable with the Rectangle that defines the viewPort on the EditorPane
   * @param pAcceptIcon    icon used for the accept action
   * @param pDiscardIcon   icon used for the discard option. Null if the panel should allow the accept action only
   * @param pDoOnAccept    Consumer that defines what is done when the accept icon is clicked
   * @param pDoOnDiscard   Consumer that defines what is done when the discard icon is clicked. May be null if no discardIcon is used
   * @param pOrientation   String with the orientation (as BorderLayout.EAST/WEST) of this panel, determines the order of accept/discardButtons
   */
  ChoiceButtonPanel(@NotNull DiffPanelModel pModel, JEditorPane pEditorPane, Observable<Rectangle> pDisplayedArea,
                    @NotNull ImageIcon pAcceptIcon, @Nullable ImageIcon pDiscardIcon, @NotNull Consumer<IFileChangeChunk> pDoOnAccept,
                    @Nullable Consumer<IFileChangeChunk> pDoOnDiscard, String pOrientation)
  {
    model = pModel;
    discardIcon = pDiscardIcon;
    acceptIcon = pAcceptIcon;
    setPreferredSize(new Dimension(pAcceptIcon.getIconWidth() + (pDiscardIcon != null ? pDiscardIcon.getIconWidth() : 0), 1));
    setBackground(new Color(0xff313335, true));
    acceptChangeIconXVal = BorderLayout.WEST.equals(pOrientation) || pDiscardIcon == null ? 0 : pDiscardIcon.getIconWidth();
    discardChangeIconXVal = BorderLayout.WEST.equals(pOrientation) ? pAcceptIcon.getIconWidth() : 0;
    disposable = Observable.combineLatest(
        pModel.getFileChangesObservable(), pDisplayedArea, FileChangesRectanglePair::new)
        .subscribe(
            pPair -> SwingUtilities.invokeLater(() -> {
              _calculateButtonViewCoordinates(pEditorPane, pPair.getFileChangesEvent(), pPair.getRectangle());
              repaint();
            }));
    addMouseListener(new IconPressMouseAdapter(pAcceptIcon.getIconWidth(), pDoOnAccept, pDoOnDiscard, () -> iconInfosToDraw,
                                               BorderLayout.WEST.equals(pOrientation)));
  }

  @Override
  public void discard()
  {
    disposable.dispose();
  }

  @Override
  protected void paintComponent(Graphics pGraphics)
  {
    super.paintComponent(pGraphics);
    _paintIcons(pGraphics, iconInfosToDraw);
  }

  /**
   * @param pEditorPane       JEditorPane that contains the text of the IFileChangeChunks
   * @param pFileChangesEvent most recent IFileChangesEvent
   * @param pDisplayedArea    Coordinates of the viewPort window, in view coordinates
   */
  private void _calculateButtonViewCoordinates(@NotNull JEditorPane pEditorPane, IFileChangesEvent pFileChangesEvent, Rectangle pDisplayedArea)
  {
    if (cachedViewRectangle == null || cachedViewRectangle.width != pDisplayedArea.width || pDisplayedArea.equals(cachedViewRectangle))
    {
      List<IconInfo> iconInfos = new ArrayList<>();
      try
      {
        View view = pEditorPane.getUI().getRootView(pEditorPane);
        int lineNumber = 0;
        for (IFileChangeChunk fileChange : pFileChangesEvent.getNewValue())
        {
          if (fileChange.getChangeType() != EChangeType.SAME)
          {
            Element lineElement = pEditorPane.getDocument().getDefaultRootElement().getElement(lineNumber);
            if (lineElement == null)
              throw new BadLocationException("lineElement for line was null", lineNumber);
            int characterStartOffset = lineElement.getStartOffset();
            int yViewCoordinate = view.modelToView(characterStartOffset, Position.Bias.Forward, characterStartOffset + 1,
                                                   Position.Bias.Forward, new Rectangle()).getBounds().y;
            iconInfos.add(new IconInfo(acceptIcon, yViewCoordinate + pEditorPane.getInsets().top, acceptChangeIconXVal, fileChange));
            if (discardIcon != null)
            {
              iconInfos.add(new IconInfo(discardIcon, yViewCoordinate + pEditorPane.getInsets().top, discardChangeIconXVal, fileChange));
            }
          }
          lineNumber += (model.getGetEndLine().apply(fileChange) - model.getGetStartLine().apply(fileChange))
              + model.getGetParityLines().apply(fileChange).length();
        }
        iconInfoList = iconInfos;
      }
      catch (BadLocationException pE)
      {
        throw new RuntimeException(pE);
      }
    }
    iconInfosToDraw = _calculateIconInfosToDraw(pEditorPane, pDisplayedArea, iconInfoList);
    cachedViewRectangle = pDisplayedArea;
  }

  /**
   *
   * @param pEditorPane JEditorPane that contains the text of the IFileChangeChunks
   * @param pDisplayedArea Coordinates of the viewPort window, in view coordinates
   * @param pIconInfos List of all IconInfos of this Panel
   * @return List of IconInfos filtered by "do they have to be drawn"
   */
  private List<IconInfo> _calculateIconInfosToDraw(@NotNull JEditorPane pEditorPane, Rectangle pDisplayedArea, List<IconInfo> pIconInfos)
  {
    List<IconInfo> filteredIconInfos = new ArrayList<>();
    for (IconInfo iconInfo : pIconInfos)
    {
      if (pDisplayedArea.intersects(iconInfo.getIconCoordinates()))
      {
        filteredIconInfos.add(new IconInfo(iconInfo.getImageIcon(), iconInfo.getIconCoordinates().y - pDisplayedArea.y + pEditorPane.getInsets().top,
                                           iconInfo.getIconCoordinates().x, iconInfo.getFileChangeChunk()));
      }
    }
    return filteredIconInfos;
  }

  /**
   * Extracted to separate method to take advantage of the fact that java gives us a reference to the list we pass. If the list itself is never
   * changed and instead the list is only exchanged with another one (like above) this should mean that there are no concurrentModificationExceptions
   * and we do not need any locks or copied immutable lists
   *
   * @param pGraphics     Graphics object to paint with
   * @param pIconInfoList the list buttons/icons to be drawn
   */
  private void _paintIcons(Graphics pGraphics, List<IconInfo> pIconInfoList)
  {
    for (IconInfo iconInfo : pIconInfoList)
    {
      iconInfo.getImageIcon().paintIcon(this, pGraphics, iconInfo.getIconCoordinates().x, iconInfo.getIconCoordinates().y);
    }
  }
}
