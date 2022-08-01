// Copyright (C) 2021-2022 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;

import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.quicklauncher.TeamToolsVersionManager;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class FileBundleMigrator {

    public static void migrate(File directory,
            FileResourceCollectionStrategy strategy, FileBundleMode bundleMode)
            throws IOException, LockFailureException {
        if (!FileBundleUtils.isBundledDir(directory))
            new FileBundleMigrator(directory, strategy, getDirType(strategy),
                    MigrationDirection.Bundle, bundleMode, true).migrate();
    }


    public static void unmigrate(File directory,
            FileResourceCollectionStrategy strategy)
            throws IOException, LockFailureException {
        FileBundleMode bundleMode = FileBundleUtils.getBundleMode(directory);
        if (bundleMode != null)
            new FileBundleMigrator(directory, strategy, getDirType(strategy),
                    MigrationDirection.Unbundle, bundleMode, true).migrate();
    }


    private static DirType getDirType(FileResourceCollectionStrategy strategy) {
        return (strategy instanceof DashboardInstanceStrategy
                ? DirType.Dashboard
                : DirType.WBS);
    }



    private enum MigrationDirection {
        Bundle, Unbundle
    }



    private enum DirType {

        Dashboard {
            void writeCompatibilityFiles(FileBundleMigrator fbm)
                    throws IOException {
                fbm.writeDashboardCompatibilityFiles();
            }
        },


        WBS {
            void writeCompatibilityFiles(FileBundleMigrator fbm)
                    throws IOException {
                fbm.writeWBSCompatibilityFiles();
            }

            void postMigrate(FileBundleMigrator fbm)
                    throws IOException, LockFailureException {
                // automatically migrate the disseminate subdir if present
                File diss = new File(fbm.directory, "disseminate");
                if (diss.isDirectory())
                    new FileBundleMigrator(diss, TeamDataDirStrategy.INSTANCE,
                            Disseminate, fbm.direction, fbm.bundleMode,
                            false).migrate();
            }
        },


        Disseminate;


        void writeCompatibilityFiles(FileBundleMigrator fbm)
                throws IOException {}

        void postMigrate(FileBundleMigrator fbm)
                throws IOException, LockFailureException {}
    };



    private interface Migrator {

        public void bundle() throws IOException, LockFailureException;

        public void unbundle() throws IOException, LockFailureException;

        public void dispose();

    }



    private File directory;

    private FileResourceCollectionStrategy strategy;

    private DirType dirType;

    private MigrationDirection direction;

    private FileBundleMode bundleMode;

    private boolean enforceLocks;

    private Migrator migrator;

    private FileBundleMigrator(File directory,
            FileResourceCollectionStrategy strategy, DirType dirType,
            MigrationDirection direction, FileBundleMode bundleMode,
            boolean enforceLocks) {
        this.directory = directory;
        this.strategy = strategy;
        this.dirType = dirType;
        this.direction = direction;
        this.bundleMode = bundleMode;
        this.enforceLocks = enforceLocks;
    }

    private void migrate() throws IOException, LockFailureException {
        // create a migrator object to do the work
        this.migrator = createMigrator();

        try {
            if (this.direction == MigrationDirection.Bundle) {

                // migrate the files into the bundle directories
                migrator.bundle();

                // remove the now-migrated files from the target directory
                cleanupLegacyFiles();

                // write a minimal set of files into the target directory to
                // steer legacy clients away from accessing the data
                dirType.writeCompatibilityFiles(this);

            } else {

                // remove read-only flags from compatibility files
                makeTargetFilesWriteable();

                // migrate the files out of the bundle directories
                migrator.unbundle();
            }

            // perform any post-migration follow-up steps
            dirType.postMigrate(this);

        } finally {
            // allow the migrator object to unwind resources
            migrator.dispose();
        }
    }

    private Migrator createMigrator() {
        if (bundleMode == FileBundleMode.Local)
            return new Local(directory, strategy, enforceLocks);

        throw new IllegalArgumentException("Unrecognized bundle mode");
    }

    private void cleanupLegacyFiles() throws IOException {
        // delete all the legacy files that were written by the dashboard
        FilenameFilter filter = strategy.getFilenameFilter();
        List<String> filenames = FileUtils.listRecursively(directory, filter);
        for (String filename : filenames) {
            File oneFile = new File(directory, filename);
            if (!oneFile.delete())
                oneFile.deleteOnExit();
        }

        // the logic above will have deleted files, but not subdirectories.
        // clean the (now-empty) directories as a separate step
        for (File oneFile : directory.listFiles()) {
            if (oneFile.isDirectory()) {
                if (filter.accept(directory, oneFile.getName() + "/"))
                    FileUtils.deleteDirectory(oneFile, true);
            }
        }
    }

    private void makeTargetFilesWriteable() {
        // find all files that match the filter, and ensure writability
        FilenameFilter filter = strategy.getFilenameFilter();
        List<String> filenames = FileUtils.listRecursively(directory, filter);
        for (String filename : filenames) {
            File oneFile = new File(directory, filename);
            oneFile.setWritable(true);
        }
    }

    private void writeDashboardCompatibilityFiles() throws IOException {
        // write a "stub" pspdash.ini file that forces a minimum dashboard
        // software version, to prevent older clients from accessing the data
        writeFile("pspdash.ini", //
            property(DataVersionChecker.SETTING_NAME,
                "pspdash version " + getReqVersion("pspdash")),
            property(BUNDLE_MODE, bundleMode.getName()));

        // write a "dummy" global.dat file, so the Quick Launcher can recognize
        // the directory as one containing a dashboard dataset
        writeFile("global.dat", //
            "#include <bundle-support.txt>", //
            "= This dataset requires functionality added in dashboard version "
                    + getReqVersion("pspdash"));
    }

    private void writeWBSCompatibilityFiles() throws IOException {
        // write a "stub" user-settings.ini file that forces a minimum WBS
        // Editor version, to prevent older clients from accessing the data
        writeFile("user-settings.ini", //
            property(TeamToolsVersionManager.WBS_EDITOR_VERSION_REQUIREMENT,
                getReqVersion("teamToolsB")),
            property(BUNDLE_MODE, bundleMode.getName()));
    }

    private void writeFile(String filename, String... lines)
            throws IOException {
        File outFile = new File(directory, filename);
        Writer out = new OutputStreamWriter(
                FileBundleUtils.outputStream(outFile), "UTF-8");
        for (String line : lines) {
            out.write(line);
            out.write(System.getProperty("line.separator"));
        }
        out.close();
        outFile.setReadOnly();
    }

    private String property(String key, String value) {
        return key + "=" + value;
    }

    private String getReqVersion(String packageId) {
        return bundleMode.getMinVersions().get(packageId);
    }


    private static class Local extends BundledWorkingDirectoryLocal
            implements Migrator {

        private boolean enforceLocks;

        public Local(File targetDirectory,
                FileResourceCollectionStrategy strategy, boolean enforceLocks) {
            super(targetDirectory, strategy, null);
            setEnableBackgroundFlush(false);
            this.enforceLocks = enforceLocks;
        }

        @Override
        protected void createWorkingDirAndProcessLock(File wdp) {
            this.workingDirectory = this.targetDirectory;
        }

        @Override
        public void assertWriteLock() throws LockFailureException {
            if (enforceLocks)
                super.assertWriteLock();
        }

        public void bundle() throws IOException, LockFailureException {
            prepare();
            if (enforceLocks)
                acquireWriteLock();
            doBackup("before_bundle_migration");
            flushData();
            FileUtils.deleteDirectory(getMetadataDir(), true);
        }

        public void unbundle() throws IOException, LockFailureException {
            if (enforceLocks)
                acquireWriteLock();
            URL backupFile = doBackup("before_bundle_unmigration");
            prepare();
            backupBundleDirs(backupFile, "bundles", "heads");
            FileUtils.deleteDirectory(getMetadataDir(), true);
        }

        private void backupBundleDirs(URL backupFile, String... subdirNames) {
            File backupDir = new File(workingDirectory, "backup");
            String prefix = extractBackupPrefix(backupFile);
            for (String oneSubdirName : subdirNames) {
                File oneSubdir = new File(workingDirectory, oneSubdirName);
                File destDir = new File(backupDir, prefix + oneSubdirName);
                oneSubdir.renameTo(destDir);
            }
        }

        private String extractBackupPrefix(URL backupFile) {
            String result = backupFile.toString();
            int beg = result.lastIndexOf('/') + 1;
            int end = result.lastIndexOf('-') + 1;
            return result.substring(beg, end);
        }

        private void acquireWriteLock() throws LockFailureException {
            int retries = strategy instanceof TeamDataDirStrategy ? 5 : 1;
            while (true) {
                try {
                    acquireWriteLock(null, getClass().getName());
                    return;
                } catch (LockFailureException lfe) {
                    if (--retries == 0)
                        throw lfe;

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void dispose() {
            releaseLocks();
        }

    }

    private static final String BUNDLE_MODE = FileBundleUtils.BUNDLE_MODE_PROP;

}
