package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileChangeType;
import de.adito.git.api.data.IFileDiff;
import de.adito.git.api.progress.IAsyncProgressFacade;
import de.adito.git.gui.Constants;
import de.adito.git.gui.dialogs.DialogResult;
import de.adito.git.gui.dialogs.IDialogProvider;
import de.adito.git.gui.icon.IIconLoader;
import io.reactivex.Observable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author m.kaspera 12.10.2018
 */
class DiffToHeadAction extends AbstractTableAction
{

  private final IAsyncProgressFacade progressFacade;
  private Observable<Optional<IRepository>> repository;
  private IDialogProvider dialogProvider;
  private Observable<Optional<List<IFileChangeType>>> selectedFilesObservable;

  @Inject
  DiffToHeadAction(IIconLoader pIconLoader, IDialogProvider pDialogProvider, IAsyncProgressFacade pProgressFacade,
                   @Assisted Observable<Optional<IRepository>> pRepository,
                   @Assisted Observable<Optional<List<IFileChangeType>>> pSelectedFilesObservable)
  {
    super("Show Diff");
    putValue(Action.SMALL_ICON, pIconLoader.getIcon(Constants.DIFF_ACTION_ICON));
    progressFacade = pProgressFacade;
    repository = pRepository;
    dialogProvider = pDialogProvider;
    selectedFilesObservable = pSelectedFilesObservable;
  }

  @Override
  public void actionPerformed(ActionEvent pEvent)
  {
    progressFacade.executeInBackground("Creating Diff", pHandle -> {
      List<File> files = selectedFilesObservable.blockingFirst()
          .orElse(Collections.emptyList())
          .stream()
          .map(iFileChangeType -> new File(iFileChangeType.getFile().getPath()))
          .collect(Collectors.toList());
      List<IFileDiff> fileDiffs = repository.blockingFirst().orElseThrow(() -> new RuntimeException("no valid repository found")).diff(files, null);

      //Show Dialog in EDT -> Handle gets finished
      SwingUtilities.invokeLater(() -> {
        DialogResult dialogResult = dialogProvider.showDiffDialog(fileDiffs, null, true);
        if (dialogResult.isPressedOk())
        {
          for (IFileDiff fileDiff : fileDiffs)
          {
            _saveFileDiffChanges(fileDiff);
          }
        }
      });
    });
  }

  private void _saveFileDiffChanges(IFileDiff pFileDiff)
  {
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(
            new File(repository.blockingFirst().orElseThrow().getTopLevelDirectory(), pFileDiff.getFilePath()), false)))
    {
      StringBuilder fileDiffContents = new StringBuilder();
      pFileDiff.getFileChanges().getChangeChunks().blockingFirst()
          .getNewValue().forEach(pChangeChunk -> fileDiffContents.append(pChangeChunk.getBLines()));
      // -1 on the substring because there is one newLine too many
      writer.write(fileDiffContents.substring(0, fileDiffContents.length() - 1));
    }
    catch (IOException pE)
    {
      throw new RuntimeException(pE);
    }
  }

}
