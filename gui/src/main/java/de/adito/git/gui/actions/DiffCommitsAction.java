package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.ICommit;
import de.adito.git.api.data.IFileDiff;
import de.adito.git.api.exception.AditoGitException;
import de.adito.git.api.progress.IAsyncProgressFacade;
import de.adito.git.gui.dialogs.IDialogProvider;
import io.reactivex.Observable;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author m.kaspera, 31.01.2019
 */
public class DiffCommitsAction extends AbstractTableAction
{

  private final IDialogProvider dialogProvider;
  private final IAsyncProgressFacade progressFacade;
  private final Observable<Optional<IRepository>> repository;
  private final Observable<Optional<List<ICommit>>> selectedCommitObservable;

  @Inject
  DiffCommitsAction(IDialogProvider pDialogProvider, IAsyncProgressFacade pProgressFacade,
                    @Assisted Observable<Optional<IRepository>> pRepository,
                    @Assisted Observable<Optional<List<ICommit>>> pSelectedCommitObservable)
  {
    super("Show Diff", _getIsEnabledObservable(pSelectedCommitObservable));
    dialogProvider = pDialogProvider;
    progressFacade = pProgressFacade;
    repository = pRepository;
    selectedCommitObservable = pSelectedCommitObservable;
  }

  @Override
  public void actionPerformed(ActionEvent pEvent)
  {
    progressFacade.executeInBackground("Creating Diff", pHandle -> {
      List<ICommit> commitList = selectedCommitObservable.blockingFirst().orElse(Collections.emptyList());
      if (!commitList.isEmpty())
      {
        ICommit selectedCommit = commitList.get(0);
        ICommit oldestSelectedCommit = commitList.get(commitList.size() - 1);
        List<IFileDiff> fileDiffs = repository.blockingFirst().map(pRepository -> {
          try
          {
            return pRepository.diff(selectedCommit, oldestSelectedCommit.getParents().get(0));
          }
          catch (AditoGitException pE)
          {
            throw new RuntimeException(pE);
          }
        }).orElse(Collections.emptyList());
        dialogProvider.showDiffDialog(fileDiffs, false);
      }
    });
  }

  private static Observable<Optional<Boolean>> _getIsEnabledObservable(Observable<Optional<List<ICommit>>> pSelectedCommitObservable)
  {
    return pSelectedCommitObservable.map(pICommits -> pICommits.map(pCommitList -> {
      if (pCommitList.isEmpty())
        return true;
      return pCommitList.get(pCommitList.size() - 1).getParents().size() == 1;
    }));
  }
}