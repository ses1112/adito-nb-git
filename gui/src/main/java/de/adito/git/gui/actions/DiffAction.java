package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileChangeType;
import de.adito.git.api.data.IFileDiff;
import de.adito.git.gui.IDialogDisplayer;
import de.adito.git.gui.dialogs.IDialogProvider;
import io.reactivex.Observable;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author m.kaspera 12.10.2018
 */
class DiffAction extends AbstractTableAction {

    private IRepository repository;
    private IDialogProvider dialogProvider;
    private Observable<List<IFileChangeType>> selectedFilesObservable;

    @Inject
    DiffAction(@Assisted Observable<IRepository> pRepository, IDialogProvider pDialogProvider,
                      @Assisted Observable<List<IFileChangeType>> pSelectedFilesObservable){
        super("Show Diff");
        repository = pRepository.blockingFirst();
        dialogProvider = pDialogProvider;
        selectedFilesObservable = pSelectedFilesObservable;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<IFileDiff> fileDiffs;
        List<IFileChangeType> fileChanges = selectedFilesObservable.blockingFirst();
        try {
            List<File> files = new ArrayList<>();
            for (IFileChangeType fileChangeType : fileChanges) {
                files.add(fileChangeType.getFile());
            }
            fileDiffs = repository.diff(files);
            dialogProvider.showDiffDialog(fileDiffs);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    protected boolean isEnabled0() {
        return true;
    }
}
