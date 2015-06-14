// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import teamdash.wbs.WBSFilenameConstants;

public class ProjectHistoryBridgedFile extends ProjectHistoryBridgedAbstract {

    private File historyZipFile;

    public ProjectHistoryBridgedFile(File historyZipFile) throws IOException {
        this.historyZipFile = historyZipFile;
        initFileRevisionsZip();
        initChanges();
        initTimeDelta();
    }

    @Override
    protected File getFileRevisionsZip() throws IOException {
        return historyZipFile;
    }

    @Override
    protected InputStream getChangeHistory() throws IOException {
        return getVersionFile(WBSFilenameConstants.CHANGE_HISTORY_FILE, lastMod);
    }

}