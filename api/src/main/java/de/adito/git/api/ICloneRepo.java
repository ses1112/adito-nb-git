package de.adito.git.api;

import de.adito.git.api.data.IBranch;
import de.adito.git.api.progress.IProgressHandle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A helper interface for the clone wizard to get
 * control over the git commands
 *
 * @author a.arnold, 09.01.2019
 */
public interface ICloneRepo
{
  /**
   * Get the branches of one repository which is not cloned or downloaded yet.
   *
   * @param pUrl     The URL to check the branches
   * @param pSshPath The path of the SSH file (optional)
   * @param pSshKey  The SSH key (optional)
   * @return Returns a list of IBranches of the repository
   */
  @NotNull
  List<IBranch> getBranchesFromRemoteRepo(@NotNull String pUrl, String pSshPath, char[] pSshKey);

  /**
   * Clone the repository from a URL to the local path
   *
   * @param pProgressHandle The handler for the progress
   * @param pLocalPath      The local path to clone
   * @param pURL            The URL to clone
   * @param pSshPath        The path of the private SSH file (optional)
   * @param pSshKey         The SSH key (optional)
   * @param pBranch         The branch to checkout
   * @param pProjectName    the project name
   */
  void cloneProject(@NotNull IProgressHandle pProgressHandle, @NotNull String pLocalPath, @NotNull String pProjectName,
                    @NotNull String pURL, String pSshPath, char[] pSshKey, IBranch pBranch);
}
