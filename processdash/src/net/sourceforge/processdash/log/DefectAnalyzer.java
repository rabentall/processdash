// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.log;

import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;


public class DefectAnalyzer {

    public interface Task {

        /** Analyze or act upon a defect in some way.
         *
         * @param d The defect to be analyzed.  The defect should be
         *          considered read-only.
         */
        void analyze(String path, Defect d);
    }

    public static final String NO_CHILDREN_PARAM = "noChildren";

    public static void run(DashHierarchy props,
                           DataRepository data,
                           String prefix,
                           Map queryParameters,
                           Task t) {
        String [] prefixes = ResultSet.getPrefixList
            (data, queryParameters, prefix);
        boolean includeChildren = !queryParameters.containsKey(NO_CHILDREN_PARAM);
        run(props, data, prefixes, includeChildren, t);
    }

    public static void run(DashHierarchy props, DataRepository data,
            String[] prefixes, boolean includeChildren, Task t) {
        for (int i = 0;   i < prefixes.length;   i++)
            run(props, prefixes[i], includeChildren, t);
        ImportedDefectManager.run(props, data, prefixes, includeChildren, t);
    }

    /** Perform some analysis task on all the defects under a given node
     *  in the hierarchy.
     *
     * @param props The PSPProperties object to be walked.
     * @param path  The path of the node in the hierarchy where the analysis
     *    should start.
     * @param includeChildren if true, the node and all its children will be
     *    analyzed;  if false, only the node itself will be visited.
     * @param t An analysis task to perform.  Every defect encountered will
     *    be passed to the task, in turn.
     */
    public static void run(DashHierarchy props, String path,
            boolean includeChildren, Task t) {
        PropertyKey pKey;
        if (path == null || path.length() == 0)
            pKey = PropertyKey.ROOT;
        else
            pKey = props.findExistingKey(path);

        if (pKey != null)
            run(props, pKey, includeChildren, t);
    }

    /** Perform some analysis task on all the defects under a given node
     *  in the hierarchy.
     *
     * @param props The PSPProperties object to be walked.
     * @param pKey  The PropertyKey of the node in the hierarchy where the
     *    analysis should start.
     * @param includeChildren if true, the node and all its children will be
     *    analyzed;  if false, only the node itself will be visited.
     * @param t An analysis task to perform.  Every defect encountered will
     *    be passed to the task, in turn.
     */
    public static void run(DashHierarchy props, PropertyKey pKey,
            boolean includeChildren, Task t) {
        Prop prop = props.pget (pKey);
        String path = pKey.path();
        String defLogName = prop.getDefectLog ();

        // If this node has a defect log,
        if (defLogName != null && defLogName.length() != 0) {
            DefectLog dl = new DefectLog
                (dataDirectory + defLogName, path, null, null);
            // read all the defects in that log, and
            Defect[] defects = dl.readDefects();
            for (int d=0;  d < defects.length;  d++)
                if (defects[d] != null)   // pass them to the analyzer task.
                    t.analyze(path, defects[d]);
        }

        // recursively analyze all the children of this node.
        if (includeChildren)
            for (int i = 0; i < prop.getNumChildren(); i++)
                run(props, prop.getChild(i), includeChildren, t);
    }

    private static String dataDirectory;

    /** Register the directory where defect logs will be found.
     * This only needs to be called once, but <b>must</b> be called before
     * calling either variant of run().
     */
    public static void setDataDirectory(String d) {
        dataDirectory = d;
        if (!dataDirectory.endsWith(Settings.sep))
            dataDirectory = dataDirectory + Settings.sep;
    }
}
