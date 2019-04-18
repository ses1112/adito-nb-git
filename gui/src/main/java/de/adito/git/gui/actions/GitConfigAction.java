package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IRepository;
import de.adito.git.gui.Constants;
import de.adito.git.gui.dialogs.DialogResult;
import de.adito.git.gui.dialogs.IDialogProvider;
import io.reactivex.Observable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Optional;

/**
 * @author m.kaspera, 24.12.2018
 */
class GitConfigAction extends AbstractTableAction
{

  private final IDialogProvider dialogProvider;
  private final Observable<Optional<IRepository>> repository;

  @Inject
  public GitConfigAction(IDialogProvider pDialogProvider, @Assisted Observable<Optional<IRepository>> pRepository)
  {
    super("Repository settings", _getIsEnabledObservable(pRepository));
    putValue(Action.SMALL_ICON, Constants.GIT_CONFIG_ICON);
    repository = pRepository;
    dialogProvider = pDialogProvider;
  }

  @Override
  public void actionPerformed(ActionEvent pEvent)
  {
    DialogResult<?, Map<String, String>> dialogResult = dialogProvider.showGitConfigDialog(repository);
    // only set sshKeyLocation for now since that is the only supported setting (for now)
    repository.blockingFirst().ifPresent(pRepo -> pRepo.getConfig().setSshKeyLocation(dialogResult.getInformation().get(Constants.SSH_KEY_KEY), null));
  }

  private static Observable<Optional<Boolean>> _getIsEnabledObservable(Observable<Optional<IRepository>> pRepository)
  {
    return pRepository.map(pRepo -> Optional.of(pRepo.isPresent()));
  }
}
