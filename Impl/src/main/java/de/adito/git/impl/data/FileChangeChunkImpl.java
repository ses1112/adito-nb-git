package de.adito.git.impl.data;

import de.adito.git.api.data.EChangeType;
import de.adito.git.api.data.IFileChangeChunk;
import org.eclipse.jgit.diff.Edit;
import org.jetbrains.annotations.Nullable;

/**
 * @author m.kaspera 05.10.2018
 */
public class FileChangeChunkImpl implements IFileChangeChunk {

    private final Edit edit;
    private final String oldString;
    private final String newString;
    private final EChangeType changeType;

    FileChangeChunkImpl(Edit pEdit, String pOldString, String pNewString){
        this(pEdit, pOldString, pNewString, null);
    }

    FileChangeChunkImpl(Edit pEdit, String pOldString, String pNewString, @Nullable EChangeType pChangeType){
        edit = pEdit;
        oldString = pOldString;
        newString = pNewString;
        changeType = pChangeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAStart() {
        return edit.getBeginA();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAEnd() {
        return edit.getEndA();
    }

    @Override
    public int getBStart() {
        return edit.getBeginB();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBEnd() {
        return edit.getEndB();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EChangeType getChangeType() {
        if(changeType != null){
            return changeType;
        }
        switch (edit.getType())
        {
            case REPLACE:
                return EChangeType.MODIFY;
            case INSERT:
                return EChangeType.ADD;
            case DELETE:
                return EChangeType.DELETE;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getALines() {
        return oldString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBLines() {
        return newString;
    }
}
