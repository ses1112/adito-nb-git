package de.adito.git;

import de.adito.git.api.data.EChangeType;
import de.adito.git.api.data.EFileType;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.FileMode;
import org.jetbrains.annotations.NotNull;

/**
 * @author m.kaspera 24.09.2018
 */
public class EnumMappings {

    public static EChangeType _toEChangeType(@NotNull DiffEntry.ChangeType pChangeType) {
        switch (pChangeType) {
            case MODIFY:
                return EChangeType.MODIFY;
            case ADD:
                return EChangeType.ADD;
            case DELETE:
                return EChangeType.DELETE;
            case RENAME:
                return EChangeType.RENAME;
            case COPY:
                return EChangeType.COPY;
            default:
                return null;
        }
    }

    public static DiffEntry.ChangeType _fromEChangeType(@NotNull EChangeType pEChangeType) {
        switch (pEChangeType) {
            case MODIFY:
                return DiffEntry.ChangeType.MODIFY;
            case ADD:
                return DiffEntry.ChangeType.ADD;
            case DELETE:
                return DiffEntry.ChangeType.DELETE;
            case RENAME:
                return DiffEntry.ChangeType.RENAME;
            case COPY:
                return DiffEntry.ChangeType.COPY;
            default:
                return null;
        }
    }

    public static EFileType _toEFileType(@NotNull FileMode pFileMode) {
        switch (pFileMode.getBits()) {
            case FileMode.TYPE_TREE:
                return EFileType.TREE;
            case FileMode.TYPE_MISSING:
                return EFileType.MISSING;
            case FileMode.TYPE_GITLINK:
                return EFileType.GITLINK;
            case FileMode.TYPE_SYMLINK:
                return EFileType.SYMLINK;
            // {@see org.eclipse.jgit.lib.FileMode}
            case 0100644:
                return EFileType.FILE;
            // {@see org.eclipse.jgit.lib.FileMode}
            case 0100755:
                return EFileType.EXECUTABLE_FILE;
            default:
                return null;
        }
    }

    public static FileMode _fromEFileType(@NotNull EFileType pFileType) {
        switch (pFileType){
            case TREE:
                return FileMode.TREE;
            case FILE:
                return FileMode.REGULAR_FILE;
            case EXECUTABLE_FILE:
                return FileMode.EXECUTABLE_FILE;
            case GITLINK:
                return FileMode.GITLINK;
            case MISSING:
                return FileMode.MISSING;
            case SYMLINK:
                return FileMode.SYMLINK;
            default:
                return null;
        }
    }

}
