package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.INotifyUtil;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.diff.EChangeSide;
import de.adito.git.api.data.diff.IFileChangeType;
import de.adito.git.api.data.diff.IFileDiff;
import de.adito.git.api.exception.AditoGitException;
import de.adito.git.api.progress.IAsyncProgressFacade;
import de.adito.git.gui.Constants;
import de.adito.git.gui.dialogs.IDialogProvider;
import de.adito.git.gui.dialogs.results.IDiffDialogResult;
import de.adito.git.gui.icon.IIconLoader;
import io.reactivex.rxjava3.core.Observable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author m.kaspera 12.10.2018
 */
class DiffToHeadAction extends AbstractTableAction
{

  private final Logger logger = Logger.getLogger(DiffToHeadAction.class.getName());
  private final IAsyncProgressFacade progressFacade;
  private final INotifyUtil notifyUtil;
  private final Boolean isAsync;
  private final Observable<Optional<IRepository>> repository;
  private final IDialogProvider dialogProvider;
  private final Observable<Optional<List<IFileChangeType>>> selectedFilesObservable;

  @Inject
  DiffToHeadAction(IIconLoader pIconLoader, IDialogProvider pDialogProvider, IAsyncProgressFacade pProgressFacade, INotifyUtil pNotifyUtil,
                   @Assisted Observable<Optional<IRepository>> pRepository,
                   @Assisted Observable<Optional<List<IFileChangeType>>> pSelectedFilesObservable,
                   @Assisted Boolean pIsAsync)
  {
    super("Show Changes", _getIsEnabledObservable(pSelectedFilesObservable));
    notifyUtil = pNotifyUtil;
    isAsync = pIsAsync;
    putValue(Action.SMALL_ICON, pIconLoader.getIcon(Constants.DIFF_ACTION_ICON));
    putValue(Action.SHORT_DESCRIPTION, "Diff to HEAD");
    progressFacade = pProgressFacade;
    repository = pRepository;
    dialogProvider = pDialogProvider;
    selectedFilesObservable = pSelectedFilesObservable;
  }

  @Override
  public void actionPerformed(ActionEvent pEvent)
  {
    repository.blockingFirst().ifPresentOrElse(this::_diff, () -> logger.log(Level.SEVERE, () -> "Git: no valid repository found in DiffToHeadAction.actionPerformed"));
  }

  /**
   * Determines whether the diff is performed asynchronously or not, and then executes the diff in the determined way
   *
   * @param pRepo Repository that contains the file(s) to diff
   */
  private void _diff(IRepository pRepo)
  {
    if (isAsync)
    {
      progressFacade.executeInBackground("Creating Diff", pHandle -> {
        _performDiff(pRepo);
      });
    }
    else
    {
      try
      {
        _performDiff(pRepo);
      }
      catch (AditoGitException pE)
      {
        notifyUtil.notify(pE, "Exception while performing diff", false);
      }
    }
  }

  /**
   * uses the repository to perform the actual diff of the file, also shows the results in a dialog
   *
   * @param pRepo Repository that contains the file(s) to diff
   */
  private void _performDiff(IRepository pRepo) throws AditoGitException
  {
    List<File> files = selectedFilesObservable.blockingFirst()
        .orElse(Collections.emptyList())
        .stream()
        .map(iFileChangeType -> new File(iFileChangeType.getFile().getPath()))
        .collect(Collectors.toList());
    List<IFileDiff> fileDiffs = pRepo.diff(files, null);

    //Show Dialog in EDT -> Handle gets finished
    SwingUtilities.invokeLater(() -> {
      IDiffDialogResult<?, ?> dialogResult = dialogProvider.showDiffDialog(pRepo.getTopLevelDirectory(), fileDiffs, null, null, "HEAD", "Working dir", true, false);
      if (dialogResult.isPressedOkay())
      {
        for (IFileDiff fileDiff : fileDiffs)
        {
          _saveFileDiffChanges(fileDiff);
        }
      }
    });
  }

  private void _saveFileDiffChanges(IFileDiff pFileDiff)
  {
    String path = pFileDiff.getFileHeader().getAbsoluteFilePath();
    if (path == null)
      return; // File does not exist -> no changes needed

    logger.log(Level.INFO, () -> String.format("Git: encoding used for writing file %s to disk: %s", path, pFileDiff.getEncoding(EChangeSide.NEW)));
    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(path), false), pFileDiff.getEncoding(EChangeSide.NEW)))
    {
      writer.write(pFileDiff.getText(EChangeSide.NEW));
    }
    catch (IOException pE)
    {
      notifyUtil.notify(pE, "An error occurred while creating the diff. ", false);
    }
  }

  private static Observable<Optional<Boolean>> _getIsEnabledObservable(Observable<Optional<List<IFileChangeType>>> pSelectedFilesObservable)
  {
    return pSelectedFilesObservable.map(pSelectedFilesOpt -> pSelectedFilesOpt.map(pSelectedFiles -> !pSelectedFiles.isEmpty()));
  }

}
