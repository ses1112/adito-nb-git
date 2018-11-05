package de.adito.git.gui.actions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileChangeType;
import de.adito.git.api.data.IFileDiff;
import de.adito.git.gui.dialogs.IDialogProvider;
import io.reactivex.Observable;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.kaspera 12.10.2018
 */
@Singleton
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
        try {
            List<File> files = selectedFilesObservable.blockingFirst().stream().map(IFileChangeType::getFile).collect(Collectors.toList());
            fileDiffs = repository.diff(files);
            dialogProvider.showDiffDialog(fileDiffs);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    protected Observable<Boolean> getIsEnabledObservable() {
        return selectedFilesObservable.map(selectedFiles -> true);
    }
}
