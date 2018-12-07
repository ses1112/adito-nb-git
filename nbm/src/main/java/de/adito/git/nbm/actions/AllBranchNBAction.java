package de.adito.git.nbm.actions;

import com.google.inject.Injector;
import de.adito.git.api.IRepository;
import de.adito.git.gui.actions.IActionProvider;
import de.adito.git.nbm.IGitConstants;
import io.reactivex.Observable;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

import java.util.Optional;

/**
 * An action class for NetBeans to show all branches.
 *
 * @author a.arnold, 22.10.2018
 */
@ActionID(category = "System", id = "de.adito.git.nbm.actions.AllBranchNBAction")
@ActionRegistration(displayName = "LBL_ShowAllBranchesNBAction_Name")
@ActionReference(path = IGitConstants.TOOLBAR_ACTION_PATH, position = 500)
public class AllBranchNBAction extends NodeAction
{

  /**
   * @param pActivatedNodes The active nodes in NetBeans
   */
  @Override
  protected void performAction(Node[] pActivatedNodes)
  {
    Observable<Optional<IRepository>> repository = NBAction.findOneRepositoryFromNode(pActivatedNodes);
    Injector injector = IGitConstants.INJECTOR;
    IActionProvider actionProvider = injector.getInstance(IActionProvider.class);
    actionProvider.getShowAllBranchesAction(repository).actionPerformed(null);
  }

  /**
   * Checking the entry point of the class {@link AllBranchNBAction}
   *
   * @param pActivatedNodes The active nodes in Netbeans
   * @return returns true if the activated project has an repository, else false.
   */
  @Override
  protected boolean enable(Node[] pActivatedNodes)
  {
    return NBAction.findOneRepositoryFromNode(pActivatedNodes).blockingFirst().isPresent();
  }

  @Override
  protected boolean asynchronous()
  {
    return false;
  }

  /**
   * @return Returns the Name of the action.
   */
  @Override
  public String getName()
  {
    return NbBundle.getMessage(AllBranchNBAction.class, "LBL_ShowAllBranchesNBAction_Name");
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }
}
