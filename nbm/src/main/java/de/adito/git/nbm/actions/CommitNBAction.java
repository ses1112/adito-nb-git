package de.adito.git.nbm.actions;

import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileChangeType;
import de.adito.git.gui.actions.IActionProvider;
import de.adito.git.nbm.IGitConstants;
import io.reactivex.Observable;
import io.reactivex.subjects.*;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.util.*;

/**
 * An action class which opens the commit dialog, responsible for the action in the right-click menu
 *
 * @author a.arnold, 25.10.2018
 */
@ActionID(category = "System", id = "de.adito.git.nbm.actions.CommitNBAction")
@ActionRegistration(displayName = "LBL_CommitNBAction_Name")
@ActionReference(path = IGitConstants.RIGHTCLICK_ACTION_PATH, position = 700)

public class CommitNBAction extends NBAction
{

  /**
   * open the commit dialog if the repository is notNull.
   *
   * @param pActivatedNodes the activated nodes in NetBeans
   */
  @Override
  protected void performAction(Node[] pActivatedNodes)
  {
    Observable<Optional<IRepository>> repository = findOneRepositoryFromNode(pActivatedNodes);
    IActionProvider actionProvider = IGitConstants.INJECTOR.getInstance(IActionProvider.class);
    Subject<Optional<List<IFileChangeType>>> listNodes;

    if (pActivatedNodes.length == 0)
    {
      listNodes = BehaviorSubject.createDefault(Optional.empty());
    }
    else
    {
      listNodes = BehaviorSubject.createDefault(getUncommittedFilesOfNodes(pActivatedNodes, repository));
    }
    actionProvider.getCommitAction(repository, listNodes).actionPerformed(null);
  }

  @Override
  protected String iconResource()
  {
    return NbBundle.getMessage(PushNBAction.class, "ICON_CommitNBAction_Path");
  }

  /**
   * @param pActivatedNodes the activated nodes in NetBeans
   * @return return true if the nodes have one repository and there are files which are not committed.
   */
  @Override
  protected boolean enable(Node[] pActivatedNodes)
  {
    if (pActivatedNodes != null)
    {
      Observable<Optional<IRepository>> repository = NBAction.findOneRepositoryFromNode(pActivatedNodes);
      return !getUncommittedFilesOfNodes(pActivatedNodes, repository).orElse(Collections.emptyList()).isEmpty();
    }
    return false;
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(CommitNBAction.class, "LBL_CommitNBAction_Name");
  }
}
