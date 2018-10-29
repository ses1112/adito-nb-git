package de.adito.git.gui.dialogs;

import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileChangeType;
import de.adito.git.api.data.IFileDiff;
import de.adito.git.api.data.IMergeDiff;
import io.reactivex.Observable;

import java.util.List;

/**
 * @author m.kaspera 26.10.2018
 */
interface IDialogFactory {

    MergeConflictDialog create(Observable<IRepository> pRepository, List<IMergeDiff> pMergeConflictDiffs);

    MergeConflictResolutionDialog create(IMergeDiff pMergeDiff);

    CommitDialog createCommitDialog(Observable<List<IFileChangeType>> pFilesToCommit);

    DiffDialog createDiffDialog(List<IFileDiff> pDiffs);

}
