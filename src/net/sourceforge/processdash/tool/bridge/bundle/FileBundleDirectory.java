// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.util.FileUtils;

public class FileBundleDirectory implements FileBundleManifestSource {

    private File bundleDir;

    private String deviceID;

    private FileBundleTimeFormat timeFormat;


    public FileBundleDirectory(File bundleDir) throws IOException {
        this.bundleDir = bundleDir;
        this.deviceID = DeviceID.get();
        this.timeFormat = new FileBundleTimeFormat(getDirTimeZone());
    }

    private String getDirTimeZone() throws IOException {
        // identify the file in the directory that contains the time zone ID
        File timezoneFile = new File(bundleDir, "timezone.txt");

        // try reading the time zone ID from the file
        String timezone = null;
        try {
            timezone = new String(FileUtils.slurpContents( //
                new FileInputStream(timezoneFile), true), "UTF-8").trim();
        } catch (IOException ioe) {
        }

        // if no time zone could be read, initialize it and write the file
        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
            Writer out = new OutputStreamWriter(
                    FileBundleUtils.outputStream(timezoneFile), "UTF-8");
            out.write(timezone);
            out.close();
        }

        return timezone;
    }



    /**
     * Create a new bundle based on the instructions in a
     * {@link FileBundleSpec}.
     * 
     * @param bundleSpec
     *            the spec describing the contents of the bundle
     * @return the FileBundleID assigned to the newly created bundle
     * @throws IOException
     *             if I/O errors are encountered
     */
    public FileBundleID storeBundle(FileBundleSpec bundleSpec)
            throws IOException {
        return storeBundle(bundleSpec.bundleName, bundleSpec.source,
            bundleSpec.filenames, bundleSpec.parents, bundleSpec.timestamp);
    }


    /**
     * Create a new bundle by gathering up files from a given directory.
     * 
     * @param bundleName
     *            the name of the bundle
     * @param source
     *            the resource collection where source files are located
     * @param filenames
     *            the names of the file in <tt>srcDir</tt> that should be placed
     *            in the bundle
     * @param parents
     *            the {@link FileBundleID}s of parent bundles
     * @param timestamp
     *            the timestamp to use for this bundle, or -1 for the current
     *            time
     * @return the FileBundleID assigned to the new bundle
     * @throws IOException
     *             if I/O errors are encountered
     */
    public FileBundleID storeBundle(String bundleName,
            ReadableResourceCollection source, List<String> filenames,
            List<FileBundleID> parents, long timestamp) throws IOException {
        // if no timestamp was supplied, use the current time
        if (timestamp <= 0)
            timestamp = System.currentTimeMillis();

        // generate an ID for the new bundle
        FileBundleID bundleID = new FileBundleID(timestamp, timeFormat,
                deviceID, bundleName);

        // write a ZIP file holding the data for the new bundle
        ResourceListing fileInfo = writeFilesToZip(bundleID, source, filenames);

        // write a manifest for the bundle
        FileBundleManifest manifest = new FileBundleManifest(bundleID, fileInfo,
                parents);
        manifest.write(bundleDir);

        // return the ID of the newly created bundle
        return bundleID;
    }

    private ResourceListing writeFilesToZip(FileBundleID bundleID,
            ReadableResourceCollection source, List<String> filenames)
            throws IOException {
        // if there are no files to write, abort without creating a ZIP file
        ResourceListing fileInfo = new ResourceListing();
        if (filenames.isEmpty())
            return fileInfo;

        // open an output stream to write ZIP data
        File zipFile = getZipFileForBundle(bundleID);
        ZipOutputStream zipOut = new ZipOutputStream(
                FileBundleUtils.outputStream(zipFile));

        // write each of the files into the ZIP
        for (String filename : filenames) {
            // retrieve the file modification time and checksum. Skip if missing
            long modTime = source.getLastModified(filename);
            Long cksum = source.getChecksum(filename);
            if (modTime == 0 || cksum == null)
                continue;

            // add a new entry to the ZIP file
            ZipEntry e = new ZipEntry(filename);
            if (isCompressedFile(filename)) {
                e.setMethod(ZipEntry.STORED);
                long[] checkData = FileUtils.computeChecksumAndSize(
                    source.getInputStream(filename), new CRC32(), true);
                e.setCrc(checkData[0]);
                e.setSize(checkData[1]);
                e.setCompressedSize(checkData[1]);
            }
            e.setTime(modTime);
            zipOut.putNextEntry(e);

            // copy the file into the ZIP
            InputStream in = source.getInputStream(filename);
            FileUtils.copyFile(in, zipOut);
            in.close();
            zipOut.closeEntry();

            // add the file to our resource listing
            fileInfo.addResource(filename, modTime, cksum);
        }

        // close the ZIP file
        zipOut.close();
        return fileInfo;
    }

    private boolean isCompressedFile(String filename) {
        return filename.endsWith(".pdash") || filename.endsWith(".zip");
    }



    /**
     * Extract files from a bundle into a target directory
     * 
     * @param bundleID
     *            the ID of the bundle to extract
     * @param target
     *            the resource collection where the files should be extracted
     * @return the list of resources in the extracted bundle
     * @throws IOException
     *             if any problems are encountered during the extraction
     */
    public ResourceCollectionInfo extractBundle(FileBundleID bundleID,
            ResourceCollection target) throws IOException {
        ResourceCollectionDiff diff = extractBundle(bundleID, target, null,
            true, false);
        return diff.getB();
    }


    /**
     * Extract files from a bundle into a target directory
     * 
     * @param bundleID
     *            the ID of the bundle to extract
     * @param target
     *            the resource collection where the files should be extracted
     * @param oldBundleID
     *            the ID of this same bundle that was previously extracted into
     *            the directory. Can be null for a fresh extraction
     * @param overwrite
     *            true if we should overwrite files in the directory with
     *            re-extracted versions; false if we should only extract files
     *            that are new/changed as compared to the old bundle
     * @param delete
     *            true if we should delete files that were in the old bundle,
     *            but are no longer present in the new bundle
     * @return an object describing the differences between the two bundles
     * @throws IOException
     *             if any problems are encountered during the extraction
     */
    public ResourceCollectionDiff extractBundle(FileBundleID bundleID,
            ResourceCollection target, FileBundleID oldBundleID,
            boolean overwrite, boolean delete) throws IOException {
        // get a diff between the new and old bundles
        ResourceCollectionInfo oldFiles = (oldBundleID == null
                ? EMPTY_COLLECTION
                : getManifest(oldBundleID).getFiles());
        ResourceCollectionInfo newFiles = getManifest(bundleID).getFiles();
        ResourceCollectionDiff diff = new ResourceCollectionDiff(oldFiles,
                newFiles);

        // extract files from the new bundle as requested
        List<String> filesToExtract;
        if (overwrite) {
            filesToExtract = newFiles.listResourceNames();
        } else {
            filesToExtract = new ArrayList<String>();
            filesToExtract.addAll(diff.getDiffering());
            filesToExtract.addAll(diff.getOnlyInB());
        }
        extractFilesFromZip(bundleID, target, newFiles, filesToExtract);

        // delete obsolete files if requested
        if (delete) {
            for (String oldFilename : diff.getOnlyInA()) {
                target.deleteResource(oldFilename);
            }
        }

        // return the diff
        return diff;
    }

    private void extractFilesFromZip(FileBundleID bundleID,
            ResourceCollection target, ResourceCollectionInfo fileInfo,
            List<String> filesToExtract) throws IOException {
        // if there are no files to be extracted, abort
        if (filesToExtract.isEmpty())
            return;

        // open the ZIP file for reading
        File zipFile = getZipFileForBundle(bundleID);
        ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry e;

        // scan the contents of the ZIP and extract files
        while ((e = zipIn.getNextEntry()) != null) {
            // if a file in the ZIP was not mentioned in the manifest, ignore
            // it. It could be some metadata item that was added by a later
            // version of the dashboard.
            String filename = e.getName();
            long lastMod = fileInfo.getLastModified(filename);
            if (lastMod <= 0)
                continue;

            // if this file is not in the list of files to extract, skip it
            if (!filesToExtract.contains(filename))
                continue;

            // copy the file to the target directory
            OutputStream out = target.getOutputStream(filename, lastMod);
            FileUtils.copyFile(zipIn, out);
            out.close();
        }

        // close the ZIP file
        zipIn.close();
    }

    private File getZipFileForBundle(FileBundleID bundleID) {
        return new File(bundleDir, bundleID.getToken() + ".zip");
    }



    public FileBundleManifest getManifest(FileBundleID bundleID)
            throws IOException {
        return new FileBundleManifest(bundleDir, bundleID);
    }


    private static final ResourceCollectionInfo EMPTY_COLLECTION = new ResourceCollectionInfo() {
        public List<String> listResourceNames() { return Collections.EMPTY_LIST; }
        public long getLastModified(String resourceName) { return 0; }
        public Long getChecksum(String resourceName) { return null; }
    };

}
