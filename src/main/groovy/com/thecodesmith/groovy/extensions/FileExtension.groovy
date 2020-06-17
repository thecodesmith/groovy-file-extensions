package com.thecodesmith.groovy.extensions

import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest

class FileExtension {

    /**
     * Returns permissions of the File as a String in the format like "rwxrw-r--".
     *
     * @param file
     * @return
     */
    static String getPermissions(final File file) {
        getPosixPermissions(file) ?: getFilePermissionsUsingSudo(file)
    }

    /**
     * Returns the file owner's name.
     *
     * @param file
     * @return
     */
    static String getOwner(final File file) {
        try {
            Files.readAttributes(file.toPath(), PosixFileAttributes).owner().name
        } catch (ignore) {
            getFileOwnerUsingSudo(file)
        }
    }

    /**
     * Returns the file owner's group.
     *
     * @param file
     * @return
     */
    static String getGroup(final File file) {
        try {
            Files.readAttributes(file.toPath(), PosixFileAttributes).group().name
        } catch (ignore) {
            getFileGroupUsingSudo(file)
        }
    }

    /**
     * Returns the MD5 checksum of the file as a String.
     *
     * @param file The File for which to calculate checksum
     * @return The MD5 checksum of the file
     */
    @CompileStatic
    static String getMd5(final File file) {
        def hash = MessageDigest.getInstance('MD5').with { md5 ->
            file.eachByte(8192) { byte[] buffer, int num -> update buffer, 0, num }
            md5.digest()
        }
        new BigInteger(1, hash).toString(16).padLeft(32, '0')
    }

    protected static String getPosixPermissions(final File file) {
        try {
            getModeString Files.getPosixFilePermissions(file.toPath())
        } catch (ignore) {
            return ''
        }
    }

    protected static String getFilePermissionsUsingSudo(final File file) {
        try {
            def info = getFileInfoUsingSudo(file)
            def permissions = info.split(/\s/).first().substring(1,10)

            return permissions
        } catch (ignore) {
            return 'Unknown: Insufficient permissions or file does not exist'
        }
    }

    protected static String getFileOwnerUsingSudo(final File file) {
        try {
            def info = getFileInfoUsingSudo(file)
            return info.split(/\s/)[2]
        } catch (ignore) {
            return 'Unknown: Unable to parse owner from file information'
        }
    }

    protected static String getFileGroupUsingSudo(final File file) {
        try {
            def info = getFileInfoUsingSudo(file)
            return info.split(/\s/)[3]
        } catch (ignore) {
            return 'Unknown: Unable to parse group from file information'
        }
    }

    protected static String getFileInfoUsingSudo(final File file) {
        if (System.getProperty('os.name').startsWith('Windows')) {
            return 'Unknown: Finding permissions is an invalid operation on Windows'
        } else {
            try {
                return ['sh', '-c', "sudo ls -ld '$file.canonicalPath'"].execute().text
            } catch (ignore) {
                return 'Unknown: Insufficient permissions or file does not exist'
            }
        }
    }

    protected static String getModeString(Set<PosixFilePermission> permissions) {
        def owner  = getModeForCategory 'OWNER', permissions
        def group  = getModeForCategory 'GROUP', permissions
        def others = getModeForCategory 'OTHERS', permissions
        "$owner$group$others"
    }

    protected static String getModeForCategory(String category, Set<PosixFilePermission> permissions) {
        getMode permissions.findAll { it.name().startsWith category } .inject(0) { a, p -> a + getMode(p) }
    }

    protected static String getMode(String mode) {
        (mode as List).collect { getMode(it as int) } .join ''
    }

    protected static String getMode(int mode) {
        def r = (mode & 0b100) ? 'r' : '-'
        def w = (mode & 0b010) ? 'w' : '-'
        def x = (mode & 0b001) ? 'x' : '-'
        "$r$w$x"
    }

    protected static int getMode(PosixFilePermission permission) {
        switch (permission.name() - ~/^.*_/) {
            case 'READ':    return 4
            case 'WRITE':   return 2
            case 'EXECUTE': return 1
            default: throw new IllegalArgumentException("Unrecognized permission: $permission")
        }
    }
}
