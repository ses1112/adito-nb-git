package de.adito.git.gui.actions;

import de.adito.git.api.IRepository;
import de.adito.git.api.data.EChangeType;
import de.adito.git.api.data.IFileChangeType;
import io.reactivex.Observable;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author m.kaspera 11.10.2018
 */
public class IgnoreAction extends AbstractTableAction {

    private IRepository repository;
    private Observable<List<IFileChangeType>> selectedFilesObservable;

    public IgnoreAction(IRepository pRepository, Observable<List<IFileChangeType>> pSelectedFilesObservable) {
        super("Ignore");
        selectedFilesObservable = pSelectedFilesObservable;
        repository = pRepository;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<IFileChangeType> fileChanges = selectedFilesObservable.blockingFirst();
        List<File> files = new ArrayList<>();
        for (IFileChangeType fileChangeType : fileChanges) {
            files.add(fileChangeType.getFile());
        }
        try {
            repository.ignore(files);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Only enabled if all selected files are not in the index yet, i.e. have status
     * NEW, MODIFY or MISSING
     */
    @Override
    protected boolean isEnabled0() {
        List<IFileChangeType> fileChanges = selectedFilesObservable.blockingFirst();
        if (fileChanges == null)
            return false;
        return fileChanges.stream()
                .allMatch(row ->
                        row.getChangeType().equals(EChangeType.NEW)
                                || row.getChangeType().equals(EChangeType.MODIFY)
                                || row.getChangeType().equals(EChangeType.MISSING));
    }
}
