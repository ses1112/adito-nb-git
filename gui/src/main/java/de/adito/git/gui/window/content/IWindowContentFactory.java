package de.adito.git.gui.window.content;

import de.adito.git.api.IRepository;
import de.adito.git.api.data.ICommit;
import io.reactivex.Observable;

import java.util.List;

/**
 * @author m.kaspera 29.10.2018
 */
interface IWindowContentFactory {

    StatusWindowContent createStatusWindowContent(Observable<IRepository> pRepository);

    BranchListWindowContent createBranchListWindowContent(Observable<IRepository> pRepository);

    CommitHistoryWindowContent createCommitHistoryWindowContent(List<ICommit> pCommits);

}