package org.zstack.test.integration.authentication.checkfile

import org.zstack.authentication.checkfile.FileVerification
import org.zstack.authentication.checkfile.FileVerificationFacadeImpl
import org.zstack.authentication.checkfile.FileVerificationRecordsVO
import org.zstack.authentication.checkfile.FileVerificationRecordsVO_
import org.zstack.authentication.checkfile.FileVerificationVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.sdk.FileVerificationInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.ShellUtils
import org.zstack.utils.path.PathUtil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileVerificationLocalRestoreCase extends SubCase {
    EnvSpec env
    Path fixtureRoot
    Path protectedFile
    String fileUuid
    FileVerificationFacadeImpl facade
    DatabaseFacade dbf

    static String quote(String path) {
        return "'" + path.replace("'", "'\\''") + "'"
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            facade = bean(FileVerificationFacadeImpl.class)
            dbf = bean(DatabaseFacade.class)
            fixtureRoot = Files.createTempDirectory(Paths.get(System.getProperty("user.dir"), "target"), "file-verification-")
            Path protectedDir = Files.createDirectory(fixtureRoot.resolve("protected directory"))
            protectedFile = protectedDir.resolve("verified ' file")
            Files.write(protectedFile, "original content".bytes)
            ShellUtils.run("chown root:root -- ${quote(protectedDir.toString())} ${quote(protectedFile.toString())}")
            ShellUtils.run("chmod 555 -- ${quote(protectedDir.toString())}")
            ShellUtils.run("chmod 755 -- ${quote(protectedFile.toString())}")
            assert !protectedDir.toFile().canWrite()
            addVerificationFile {
                node = FileVerification.NODE_MANAGEMENT_NODE
                path = protectedFile.toString()
                hexType = "md5"
                category = "other"
            }
            FileVerificationInventory inventory = queryVerificationFile {
                conditions = ["node=mn"]
            }.find { it.path == protectedFile.toString() }
            assert inventory != null: "verification file was not created for ${protectedFile}"
            fileUuid = inventory.uuid
            testRelativeSourcePath()
            testRestoreDeletedProtectedFile()
            testRestoreModifiedProtectedFile()
            testRefreshLegacyBackup()
            testRestoreSymbolicLink()
            testRestoreMissingParentDirectory()
            testMissingBackupReportsFailure()
        }
    }

    void checkLocalFile() {
        facade.checkLocalFiles([dbf.findByUuid(fileUuid, FileVerificationVO.class)])
    }

    void assertRestoredFile() {
        assert Files.exists(protectedFile)
        assert new String(Files.readAllBytes(protectedFile)) == "original content"
        assert Files.getOwner(protectedFile).name == "root"
        assert ((Files.getAttribute(protectedFile, "unix:mode") as int) & 0777) == 0755
        retryInSecs {
            def records = queryFileVerificationRecords {
                conditions = ["fileVerificationUuid=${fileUuid}"]
            }
            assert !records.empty
            assert records.every { it.recoverFlag }
        }
    }

    void testRestoreDeletedProtectedFile() {
        ShellUtils.run("rm -- ${quote(protectedFile.toString())}")
        checkLocalFile()
        assertRestoredFile()
    }

    void testRelativeSourcePath() {
        String relativeSource = Paths.get(System.getProperty("user.dir")).relativize(protectedFile).toString()
        Path copy = fixtureRoot.resolve("relative source copy")
        facade.backupRestoreLocalFile(relativeSource, copy.toString())
        assert new String(Files.readAllBytes(copy)) == "original content"
        assert Files.getOwner(copy).name == "root"
        assert ((Files.getAttribute(copy, "unix:mode") as int) & 0777) == 0755
    }

    void testRestoreModifiedProtectedFile() {
        ShellUtils.run("truncate -s 0 -- ${quote(protectedFile.toString())}")
        checkLocalFile()
        assertRestoredFile()
    }

    void testRefreshLegacyBackup() {
        Path backup = new File(PathUtil.join(PathUtil.getZStackHomeFolder(), "backupfiles", fileUuid)).toPath()
        ShellUtils.run("chown -- ${quote(System.getProperty('user.name'))} ${quote(backup.toString())}")
        ShellUtils.run("chmod 750 -- ${quote(backup.toString())}")
        removeVerificationFile { uuid = fileUuid }
        recoverVerificationFile { uuid = fileUuid }
        assert Files.getOwner(backup).name == "root"
        assert ((Files.getAttribute(backup, "unix:mode") as int) & 0777) == 0755
        testRestoreDeletedProtectedFile()
    }

    void testRestoreSymbolicLink() {
        Path unrelatedFile = fixtureRoot.resolve("unrelated file")
        Files.write(unrelatedFile, "unrelated content".bytes)
        ShellUtils.run("rm -- ${quote(protectedFile.toString())}")
        ShellUtils.run("ln -s -- ${quote(unrelatedFile.toString())} ${quote(protectedFile.toString())}")
        checkLocalFile()
        assertRestoredFile()
        assert !Files.isSymbolicLink(protectedFile)
        assert new String(Files.readAllBytes(unrelatedFile)) == "unrelated content"
    }

    void testRestoreMissingParentDirectory() {
        ShellUtils.run("rm -rf -- ${quote(protectedFile.parent.toString())}")
        checkLocalFile()
        assertRestoredFile()
    }

    void testMissingBackupReportsFailure() {
        String backup = PathUtil.join(PathUtil.getZStackHomeFolder(), "backupfiles", fileUuid)
        ShellUtils.run("rm -- ${quote(backup)} ${quote(protectedFile.toString())}")
        checkLocalFile()
        assert !Files.exists(protectedFile)
        retryInSecs {
            assert queryFileVerificationRecords {
                conditions = ["fileVerificationUuid=${fileUuid}", "recoverFlag=false"]
            }.size() == 1
        }
    }

    @Override
    void clean() {
        if (fileUuid != null) {
            deleteVerificationFile { uuid = fileUuid }
            SQL.New(FileVerificationRecordsVO.class)
                    .eq(FileVerificationRecordsVO_.fileVerificationUuid, fileUuid).delete()
            ShellUtils.run("rm -f -- ${quote(PathUtil.join(PathUtil.getZStackHomeFolder(), 'backupfiles', fileUuid))}")
        }
        if (fixtureRoot != null) {
            ShellUtils.run("rm -rf -- ${quote(fixtureRoot.toString())}")
        }
        env.delete()
    }
}
