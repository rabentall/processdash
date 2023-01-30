// Copyright (C) 2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.bridge.bundle.DeviceID;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.util.ComputerName;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.LockMessageHandler;

public class DeviceLockManager {

    // singleton instance of lock manager
    private static DeviceLockManager INSTANCE = null;

    private static final Logger logger = Logger
            .getLogger(DeviceLockManager.class.getName());


    static void setWorkingDirectory(WorkingDirectory workingDirectory) {
        // if the manager was already created, abort
        if (INSTANCE != null)
            return;

        // only create/monitor device locks for local working directories
        if (!(workingDirectory instanceof LocalWorkingDirectory))
            return;

        // find the target directory, ensure it exists
        File targetDir = workingDirectory.getTargetDirectory();
        if (targetDir == null || !targetDir.isDirectory())
            return;

        // find or create the lock subdirectory. Abort on failure
        File lockDir = new File(targetDir, LOCK_SUBDIR);
        if (!lockDir.isDirectory() && !lockDir.mkdir())
            return;

        // Create the device lock manager for this directory
        try {
            INSTANCE = new DeviceLockManager(lockDir);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create DeviceLockManager", e);
        }
    }


    static void writeLockFile(LockMessageHandler handler, String lockOwner) {
        if (INSTANCE != null)
            INSTANCE.writeLockFileImpl(handler, lockOwner);
    }


    static List<DeviceLock> getConflictingLocks() {
        if (INSTANCE != null)
            return INSTANCE.getOtherLocks();
        else
            return Collections.EMPTY_LIST;
    }


    static void ignore(DeviceLock lock) {
        if (INSTANCE != null && lock != null)
            lock.delete();
    }



    private File lockDir;

    private String selfLockFilename;

    private File selfLockFile;

    private DeviceLock selfLock;


    private DeviceLockManager(File lockDir) throws IOException {
        String deviceID = DeviceID.get();
        this.lockDir = lockDir;
        this.selfLockFilename = LOCK_FILE_PREFIX + deviceID + LOCK_FILE_SUFFIX;
        this.selfLockFile = new File(lockDir, selfLockFilename);
        this.selfLockFile.deleteOnExit();
    }

    private void writeLockFileImpl(LockMessageHandler handler,
            String lockOwner) {
        selfLock = new DeviceLock();
        selfLock.owner = lockOwner;
        selfLock.username = System.getProperty("user.name");
        selfLock.host = ComputerName.getName();
        selfLock.opened = new Date();
        try {
            selfLock.writeToFile(selfLockFile);
        } catch (IOException ioe) {
            logger.log(Level.WARNING,
                "Could not write device lock file " + selfLockFile, ioe);
        }
    }

    private List<DeviceLock> getOtherLocks() {
        List<DeviceLock> result = new ArrayList<DeviceLock>();
        String[] files = lockDir.list();
        if (files != null) {
            for (String oneFile : files) {
                if (oneFile.startsWith(LOCK_FILE_PREFIX) //
                        && oneFile.endsWith(LOCK_FILE_SUFFIX) //
                        && !oneFile.equalsIgnoreCase(selfLockFilename)) {
                    try {
                        DeviceLock oneLock = new DeviceLock();
                        oneLock.readFromFile(new File(lockDir, oneFile));
                        result.add(oneLock);
                    } catch (Exception e) {
                        logger.log(Level.FINE,
                            "Could not read device lock file " + oneFile, e);
                    }
                }
            }
        }
        Collections.sort(result);
        return result;
    }



    public class DeviceLock implements Comparable<DeviceLock> {

        public String owner, username, host;

        public Date opened;

        private File lockFile;

        private void writeToFile(File f) throws IOException {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(lockFile = f), "UTF-8"));

            out.write("<?xml version='1.0' standalone='yes'?>");
            out.newLine(); out.newLine();

            out.write("<" + LOCK_TAG);
            writeAttr(out, OWNER_ATTR, owner);
            writeAttr(out, USERNAME_ATTR, username);
            writeAttr(out, HOST_ATTR, host);
            writeAttr(out, OPENED_ATTR, XMLUtils.saveDate(opened));
            out.write("/>");
            out.newLine();

            out.close();
        }

        private void writeAttr(BufferedWriter out, String attr, String value)
                throws IOException {
            if (value != null) {
                out.newLine();
                out.write("    " + attr + "='");
                out.write(XMLUtils.escapeAttribute(value));
                out.write("'");
            }
        }

        private void readFromFile(File f) throws IOException, SAXException {
            Element xml = XMLUtils.parse(new FileInputStream(lockFile = f))
                    .getDocumentElement();
            owner = xml.getAttribute(OWNER_ATTR);
            username = xml.getAttribute(USERNAME_ATTR);
            host = xml.getAttribute(HOST_ATTR);
            opened = XMLUtils.getXMLDate(xml, OPENED_ATTR);
        }

        private void delete() {
            if (lockFile != null) {
                logger.severe("Deleting / ignoring device lock " + lockFile);
                lockFile.delete();
            }
        }

        public int compareTo(DeviceLock that) {
            // sort in reverse-chronological order, newest first
            if (this.opened == that.opened) return 0;
            if (this.opened == null) return +1;
            if (that.opened == null) return -1;
            return that.opened.compareTo(this.opened);
        }
    }


    private static final String LOCK_SUBDIR = "locks";
    private static final String LOCK_FILE_PREFIX = "device-lock-";
    private static final String LOCK_FILE_SUFFIX = ".xml";

    private static final String LOCK_TAG = "device-lock";
    private static final String OWNER_ATTR = "owner";
    private static final String USERNAME_ATTR = "username";
    private static final String HOST_ATTR = "host";
    private static final String OPENED_ATTR = "opened";

}
