package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.INotifyUtil;
import de.adito.git.api.IRepository;
import de.adito.git.api.ISaveUtil;
import de.adito.git.api.data.*;
import de.adito.git.api.exception.AditoGitException;
import de.adito.git.api.exception.AuthCancelledException;
import de.adito.git.api.exception.MissingTrackedBranchException;
import de.adito.git.api.prefs.IPrefStore;
import de.adito.git.api.progress.IAsyncProgressFacade;
import de.adito.git.api.progress.IProgressHandle;
import de.adito.git.gui.actions.commands.StashCommand;
import de.adito.git.gui.dialogs.EButtons;
import de.adito.git.gui.dialogs.IDialogProvider;
import de.adito.git.gui.dialogs.results.IMergeConflictDialogResult;
import de.adito.git.gui.dialogs.results.IUserPromptDialogResult;
import de.adito.git.gui.sequences.MergeConflictSequence;
import de.adito.git.impl.Util;
import de.adito.git.impl.data.MergeDetailsImpl;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PullAction to pull from one branch.
 *
 * @author A.Arnold 11.10.2018
 */
class PullAction extends AbstractAction
{
  private static final String STASH_ID_KEY = "pull::stashCommitId";
  private static final String NO_VALID_REPO_MSG = "no valid repository found";
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final Observable<Optional<IRepository>> repository;
  private final IPrefStore prefStore;
  private final IDialogProvider dialogProvider;
  private final IActionProvider actionProvider;
  private final INotifyUtil notifyUtil;
  private final IAsyncProgressFacade progressFacade;
  private final ISaveUtil saveUtil;
  private final MergeConflictSequence mergeConflictSequence;
  private boolean doUnstash = true;

  /**
   * The PullAction is an action to pull all commits from one branch. If no branch is chosen take an empty string for the master branch.
   *
   * @param pRepository the repository where the pull command should work
   */
  @Inject
  PullAction(IPrefStore pPrefStore, IDialogProvider pDialogProvider, IActionProvider pActionProvider, INotifyUtil pNotifyUtil, IAsyncProgressFacade pProgressFacade,
             ISaveUtil pSaveUtil, MergeConflictSequence pMergeConflictSequence, @Assisted Observable<Optional<IRepository>> pRepository)
  {
    prefStore = pPrefStore;
    dialogProvider = pDialogProvider;
    actionProvider = pActionProvider;
    notifyUtil = pNotifyUtil;
    progressFacade = pProgressFacade;
    saveUtil = pSaveUtil;
    mergeConflictSequence = pMergeConflictSequence;
    putValue(Action.NAME, Util.getResource(this.getClass(), "pullActionName"));
    putValue(Action.SHORT_DESCRIPTION, Util.getResource(this.getClass(), "pullTooltipMsg"));
    repository = pRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void actionPerformed(ActionEvent pEvent)
  {
    progressFacade.executeAndBlockWithProgress(Util.getResource(this.getClass(), "pullHandleTitleMsg"), this::_doRebase);
  }

  /**
   * Keeps calling the pull method of the repository until the result is either a success or the user
   * presses cancel on one of the Conflict resolution dialogs
   */
  private void _doRebase(@NotNull IProgressHandle pProgressHandle)
  {
    saveUtil.saveUnsavedFiles();
    pProgressHandle.setDescription(Util.getResource(this.getClass(), "pullGetRepoHandleMsg"));
    IRepository pRepo = repository.blockingFirst().orElseThrow(() -> new RuntimeException(NO_VALID_REPO_MSG));
    boolean doAbort = false;
    try
    {
      ICommit head = pRepo.getCommit(null);
      IRepositoryState repositoryState = pRepo.getRepositoryState().blockingFirst(Optional.empty()).orElse(null);
      if (repositoryState == null || repositoryState.getCurrentRemoteTrackedBranch() == null)
      {
        notifyUtil.notify(Util.getResource(this.getClass(), "pullAbortTitle"), Util.getResource(this.getClass(), "pullFailedRepoStateMsg"), false);
        return;
      }
      else
      {
        pProgressHandle.setDescription(Util.getResource(this.getClass(), "pullFetchHandleMsg"));
        if (pRepo.isUpToDate(repositoryState.getCurrentBranch(), repositoryState.getCurrentRemoteTrackedBranch()))
        {
          notifyUtil.notify(Util.getResource(this.getClass(), "pullBrancheUpToDateTitle"), Util.getResource(this.getClass(), "pullBrancheUpToDateMsg"), false);
          return;
        }
      }
      if (pRepo.getStatus().blockingFirst().map(pStatus -> !pStatus.getConflicting().isEmpty()).orElse(false))
      {
        notifyUtil.notify(Util.getResource(this.getClass(), "pullExistingConflictingFilesTitle"),
                          Util.getResource(this.getClass(), "pullExistingConflictingFilesMsg"), false);
        return;
      }
      if (!pRepo.getStatus().blockingFirst().map(pStatus -> pStatus.getUncommitted().isEmpty()).orElse(true))
      {
        if (!ActionUtility.handleStash(prefStore, dialogProvider, pRepo, STASH_ID_KEY, pProgressHandle))
          return;
      }
      if (_checkRebasingMergeCommit(pRepo))
      {
        return;
      }
      while (!doAbort)
      {
        pProgressHandle.setDescription(Util.getResource(this.getClass(), "pullRebaseHandleMsg"));
        IRebaseResult rebaseResult =
            pRepo.pull(false);
        if (rebaseResult.isSuccess())
        {
          if (rebaseResult.getResultType() != IRebaseResult.ResultType.UP_TO_DATE)
            notifyUtil.notify(Util.getResource(this.getClass(), "pullSuccessTitle"), Util.getResource(this.getClass(), "pullSuccessMsg"), false,
                              actionProvider.getDiffCommitToHeadAction(Observable.just(Optional.of(pRepo)),
                                                                       Observable.just(Optional.of(List.of(head))),
                                                                       Observable.just(Optional.empty())));
          else
          {
            notifyUtil.notify(Util.getResource(this.getClass(), "pullSuccessTitle"), Util.getResource(this.getClass(), "pullAlreadyUpToDate"), false);
          }
          break;
        }
        if (!rebaseResult.getMergeConflicts().isEmpty())
        {
          pProgressHandle.setDescription(Util.getResource(this.getClass(), "pullResolveConflictsHandleMsg"));
          // if the pull should be aborted, _handleConflictDialog returns true
          IMergeDetails mergeDetails = new MergeDetailsImpl(rebaseResult.getMergeConflicts(), "Local", "Remote");
          doAbort = _handleConflictDialog(pRepo, mergeDetails);
        }
        if (rebaseResult.getResultType() == null)
        {
          notifyUtil.notify(Util.getResource(this.getClass(), "pullUnclearRebaseStateTitle"),
                            Util.getResource(this.getClass(), "pullUnclearRebaseStateMsg"), true);
          _abortRebase(pRepo);
          doAbort = true;
        }
      }
    }
    catch (MissingTrackedBranchException pE)
    {
      notifyUtil.notify(pE, Util.getResource(this.getClass(), "pullFailedUpstreamBranchMsg"), false);
    }
    catch (AuthCancelledException pE)
    {
      notifyUtil.notify(Util.getResource(this.getClass(), "pullAbortTitle"), Util.getResource(this.getClass(), "pullAbortDueToAuthCancel"), false);
      logger.log(Level.WARNING, pE, () -> Util.getResource(this.getClass(), "authCancelledLogMessage"));
    }
    catch (AditoGitException e)
    {
      Throwable rootCause = de.adito.git.impl.util.Util.getRootCause(e);
      if (rootCause instanceof UnknownHostException)
      {
        notifyUtil.notify(e, MessageFormat.format(Util.getResource(FetchAction.class, "unknownHostMsg"), rootCause.getMessage()), false);
      }
      else
      {
        notifyUtil.notify(e, Util.getResource(this.getClass(), "pullFailedExceptionMsg"), false);
      }
    }
    catch (Exception e)
    {
      notifyUtil.notify(e, Util.getResource(this.getClass(), "pullFailedExceptionMsg"), false);
    }
    finally
    {
      if (doUnstash)
      {
        String stashedCommitId = prefStore.get(STASH_ID_KEY);
        if (stashedCommitId != null)
        {
          pProgressHandle.setDescription(Util.getResource(this.getClass(), "unstashChangesMessage"));
          logger.log(Level.INFO, () -> Util.getResource(this.getClass(), "unstashChangesMessage"));
          StashCommand.doUnStashing(mergeConflictSequence, stashedCommitId, Observable.just(Optional.of(pRepo)));
          prefStore.put(STASH_ID_KEY, null);
        }
      }
      doUnstash = true;
    }
  }

  /**
   * Handles showing the conflict dialog if a conflict occurred during the pull
   *
   * @param pMergeDetails MergeDetails containing the list of IMergeData for each file that is in a conflicting state and info about the origins of the conflicting
   *                      versions
   * @return true if the user pressed cancel and the pull should be aborted, false otherwise
   * @throws AditoGitException if an error occurred during the pull
   */
  private boolean _handleConflictDialog(@NotNull IRepository pRepo, @NotNull IMergeDetails pMergeDetails) throws AditoGitException
  {
    IMergeConflictDialogResult<?, ?> dialogResult = mergeConflictSequence.performMergeConflictSequence(Observable.just(Optional.of(pRepo)), pMergeDetails, true);
    IUserPromptDialogResult<?, ?> promptDialogResult = null;
    if (dialogResult.isFinishMerge())
    {
      return false;
    }
    else if (!dialogResult.isAbortMerge())
    {
      promptDialogResult = dialogProvider.showMessageDialog(Util.getResource(this.getClass(), "mergeSaveStateQuestion"),
                                                            List.of(EButtons.SAVE, EButtons.ABORT),
                                                            List.of(EButtons.SAVE));
      if (promptDialogResult.isOkay())
      {
        doUnstash = false;
        notifyUtil.notify(Util.getResource(this.getClass(), "mergeSavedStateTitle"), Util.getResource(this.getClass(), "mergeSavedStateMsg"),
                          false);
      }
    }
    if (dialogResult.isAbortMerge() || (promptDialogResult != null && !promptDialogResult.isOkay()))
    {
      _abortRebase(pRepo);
    }
    return true;
  }

  /**
   * aborts the rebase
   *
   * @param pRepo Repository to call the aborted rebase on
   * @throws AditoGitException if an error occurs during the abort
   */
  private void _abortRebase(IRepository pRepo) throws AditoGitException
  {
    // user pressed cancel -> abort
    IRebaseResult abortedRebaseResult =
        pRepo.pull(true);
    if (abortedRebaseResult.getResultType() != IRebaseResult.ResultType.ABORTED)
    {
      String errorMessage = Util.getResource(this.getClass(), "pullAbortErrorMsg") + abortedRebaseResult.getResultType();
      RuntimeException pE = new RuntimeException(errorMessage);
      notifyUtil.notify(pE, errorMessage + ". ", false);
      return;
    }
    // abort was successful -> notify user
    notifyUtil.notify(Util.getResource(this.getClass(), "pullAbortTitle"), Util.getResource(this.getClass(), "pullAbortMsg"), true);
  }

  /**
   * checks if a merge-commit would be rebased, and if so asks the user if a merge should be performed instead.
   *
   * @param pRepo current repo
   * @return true if a merge-commit would be rebased, false otherwise
   * @throws AditoGitException if an error occurs while retrieving the unpushed commits
   */
  private boolean _checkRebasingMergeCommit(@NotNull IRepository pRepo) throws AditoGitException
  {
    Optional<IRepositoryState> repositoryState = pRepo.getRepositoryState().blockingFirst();
    if (repositoryState.isPresent())
    {
      IBranch currentBranch = repositoryState.get().getCurrentBranch();
      List<ICommit> unPushedCommits = pRepo.getUnPushedCommits();
      if (unPushedCommits.stream().anyMatch(pCommit -> pCommit.getParents().size() > 1) && currentBranch.getTrackedBranchStatus().getRemoteAheadCount() > 0)
      {
        if (repositoryState.get().getCurrentRemoteTrackedBranch() != null)
        {
          IUserPromptDialogResult<?, ?> dialogResult = dialogProvider.showMessageDialog(Util.getResource(this.getClass(), "rebaseMergeCommitsWarning"),
                                                                                        List.of(EButtons.MERGE_REMOTE, EButtons.CANCEL),
                                                                                        List.of(EButtons.MERGE_REMOTE));
          if (dialogResult.isOkay())
          {
            actionProvider.getMergeAction(Observable.just(Optional.of(pRepo)), Observable.just(Optional.of(repositoryState.get().getCurrentRemoteTrackedBranch())))
                .actionPerformed(null);
          }
        }
      }
      else
      {
        return false;
      }
    }
    return true;
  }
}
