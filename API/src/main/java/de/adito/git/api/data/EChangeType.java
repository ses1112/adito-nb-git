package de.adito.git.api.data;

/**
 * Enum that describes the type of change that happened to a file
 *
 * @author m.kaspera 24.09.2018
 */
public enum EChangeType {
    /** Add a new file to the project */
    ADD,

    /** Modify an existing file in the project (content and/or mode) */
    MODIFY,

    /** Delete an existing file from the project */
    DELETE,

    /** Rename an existing file to a new location */
    RENAME,

    /** Copy an existing file to a new location, keeping the original */
    COPY
}
