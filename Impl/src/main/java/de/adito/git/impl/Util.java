package de.adito.git.impl;

import org.eclipse.jgit.api.Git;

import java.io.File;
/**
 * @author a.arnold
 */
class Util {

    /**
     * Checking the directory is empty and existing.
     *
     * @param file The directory to check
     * @return true - The directory is empty. false - the directory isn't empty.
     */
    static boolean isDirEmpty(File file) {
        if (file.exists()) {
            return file.list().length == 0;
        }
        return true;
    }

    /**
     * Get the relative path of a file.
     *
     * @param file the file where the dir is needed
     * @param git  the git for checking the base dir
     * @return gives the dir from file
     */
    static String getRelativePath(File file, Git git) {
        String base = git.getRepository().getDirectory().getParent();
        String path = file.getAbsolutePath();
        String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();
        return relative;
    }
}