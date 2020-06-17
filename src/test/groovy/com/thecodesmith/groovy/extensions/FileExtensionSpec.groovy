package com.thecodesmith.groovy.extensions

import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static java.nio.file.attribute.PosixFilePermission.*

class FileExtensionSpec extends Specification {

    @Shared file = new File('build.gradle')

    def 'get permissions on a file object'() {
        expect:
        file.permissions == 'rw-r--r--'
    }

    @IgnoreIf({ env['CI'] })
    def 'get owner on a file object'() {
        expect:
        file.owner == 'whoami'.execute().text.trim()
    }

    @IgnoreIf({ env['CI'] })
    def 'get group on a file object'() {
        expect:
        file.group == 'staff'
    }

    @Unroll
    def 'get mode string'() {
        expect:
        FileExtension.getModeString(permissions) == expected

        where:
        permissions                                     | expected
        [OWNER_READ] as Set                             | 'r--------'
        [OWNER_READ, OWNER_WRITE, OWNER_EXECUTE] as Set | 'rwx------'
        [OWNER_READ, GROUP_READ, OTHERS_READ] as Set    | 'r--r--r--'
    }

    @IgnoreIf({ env['CI'] })
    def 'generate MD5 sum for given file'() {
        given:
        def file = new File('build.gradle')

        and:
        def md5 = "/sbin/md5 $file.absolutePath".execute().text.split().last()

        expect:
        file.md5 == md5
    }
}
