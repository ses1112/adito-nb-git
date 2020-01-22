package de.adito.git.gui.actions;

import de.adito.git.api.IRepository;
import de.adito.git.api.data.*;
import de.adito.git.api.exception.AditoGitException;
import de.adito.git.api.prefs.IPrefStore;
import de.adito.git.api.progress.IProgressHandle;
import de.adito.git.gui.dialogs.IDialogProvider;
import de.adito.git.gui.dialogs.results.IStashChangesQuestionDialogResult;
import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * @author m.kaspera, 01.07.2019
 */
class ActionUtility
{

  /**
   * Asks the user if he wants to stash the changed files, discard the changes or abort the current action
   *
   * @param pPrefStore      PrefStore for saving the Stash commit id
   * @param pDialogProvider DialogProvider to get the stash dialog
   * @param pRepository     repository to retrieve the changed files
   * @param pStashKey       Key with which the stash commit id should be saved
   * @param pHandle         ProgressHandle to the the status to stashing, may be null
   * @return true if the user selected an option that allows the action to continue, false if the user aborted
   * @throws AditoGitException thrown when trying to discard the changes and either HEAD cannot be determined or some other error occurs during the reset
   */
  static boolean handleStash(@NotNull IPrefStore pPrefStore, @NotNull IDialogProvider pDialogProvider, @NotNull IRepository pRepository, @NotNull String pStashKey,
                             @Nullable IProgressHandle pHandle) throws AditoGitException
  {
    List<IFileChangeType> changedFiles = pRepository.getStatus().blockingFirst().map(IFileStatus::getUncommitted).orElse(List.of());
    IStashChangesQuestionDialogResult<?, Object> dialogResult =
        pDialogProvider.showStashChangesQuestionDialog(Observable.just(Optional.of(pRepository)), changedFiles,
                                                       pRepository.getTopLevelDirectory());
    if (dialogResult.isAbort())
      return false;
    if (dialogResult.isStashChanges())
    {
      if (pHandle != null)
        pHandle.setDescription("Stashing existing changes");
      pPrefStore.put(pStashKey, pRepository.stashChanges(null, true));
    }
    else if (dialogResult.isDiscardChanges())
    {
      ICommit head = pRepository.getCommit(null);
      if (head == null)
        throw new AditoGitException("Cannot determine HEAD, so the current changes can't be discarded");
      pRepository.reset(head.getId(), EResetType.HARD);
    }
    return true;
  }

}
