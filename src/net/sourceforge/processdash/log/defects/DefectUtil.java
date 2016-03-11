// Copyright (C) 2007-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.defects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.process.WorkflowInfo;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.process.WorkflowInfoFactory;
import net.sourceforge.processdash.team.TeamDataConstants;

public class DefectUtil {

    /**
     * If the given path is part of a team workflow, return a list of phases
     * that are appropriate for defect injection/removal.
     * 
     * @param taskPath
     *            the path of a task within the dashboard
     * @param context
     *            the dashboard context
     * @return a list of phases appropriate for the containing workflow.
     */
    public static DefectPhaseList getWorkflowDefectPhases(String taskPath,
            DashboardContext context) {

        // retrieve the workflow info for the current project. If no workflow
        // info is found, return null.
        DataRepository data = context.getData();
        WorkflowInfo workflowInfo = WorkflowInfoFactory.get(data, taskPath);
        if (workflowInfo == null || workflowInfo.isEmpty())
            return null;
        DefectPhaseList result = new DefectPhaseList();
        result.workflowInfo = workflowInfo;

        // check to see if this is a PSP task.
        String parentPath = DataRepository.chopPath(taskPath);
        String phaseSuffix = null;
        if (isPspTask(data, parentPath)) {
            phaseSuffix = taskPath.substring(parentPath.length());
            taskPath = parentPath;
        } else if (isPspTask(data, taskPath)) {
            phaseSuffix = "/Code";
        }

        // see if the current task came from a (potentially nested) workflow.
        // If so, add all applicable workflow phases to our result.
        String path = taskPath;
        Set<Phase> phasesSeen = new HashSet<Phase>();
        while (path != null) {

            // see if this task represents a phase in a workflow.
            String workflowId = getWorkflowID(data, path);
            if (workflowId != null && phaseSuffix != null)
                workflowId += phaseSuffix;
            Phase phase = workflowInfo.getPhase(workflowId);
            if (phase == null)
                break; // not a workflow phase. We are done.

            // if this is a new phase we haven't seen before, add to our result.
            // (Phases we've seen before will represent subdivided tasks.)
            if (!phasesSeen.contains(phase)) {

                // if we have any phases in our result already, they must be
                // nested underneath the current task. Update their phase IDs
                // to document this nested relationship.
                for (DefectPhase dp : result)
                    dp.phaseID = phase.getPhaseId() + "," + dp.phaseID;

                // add all of the phases in the given workflow to our result
                boolean firstPhase = true;
                for (Phase onePhase : phase.getWorkflow().getPhases()) {
                    DefectPhase dp = new DefectPhase(onePhase.getPhaseName());
                    dp.processName = onePhase.getWorkflow().getWorkflowName();
                    dp.phaseID = onePhase.getPhaseId();
                    dp.legacyPhase = onePhase.getMcfPhase();

                    // potentially set the default injection/removal phases
                    if (onePhase == phase) {
                        if (!firstPhase && result.defaultInjectionPhase == -1)
                            result.defaultInjectionPhase = result.size() - 1;
                        if (result.defaultRemovalPhase == -1)
                            result.defaultRemovalPhase = result.size();
                    }

                    phasesSeen.add(onePhase);
                    result.add(dp);
                    firstPhase = false;
                }
            }

            // step up to the parent and look for phases there, as well.
            path = DataRepository.chopPath(path);
            phaseSuffix = null;
        }

        return result;
    }

    private static boolean isPspTask(DataContext data, String path) {
        return data.getSimpleValue(path + "/PSP Project") != null;
    }

    private static String getWorkflowID(DataContext data, String path) {
        String dataName = DataRepository.createDataName(path,
            TeamDataConstants.WORKFLOW_ID_DATA_NAME);
        SimpleData sd = data.getSimpleValue(dataName);
        if (sd == null)
            return null;

        String result = sd.format();
        int commaPos = result.indexOf(',');
        return (commaPos == -1 ? result : result.substring(0, commaPos));
    }


    /**
     * Return the same information as
     * {@link #getDefectPhases(String, DashboardContext)}, but in the form of a
     * DefectPhaseList.
     */
    public static DefectPhaseList getDefectPhaseList(String defectPath,
            String taskPath, DashboardContext context) {
        List<String> phaseNames = getDefectPhases(defectPath, context);
        String removalPhase = guessRemovalPhase(defectPath, taskPath, context);
        String injectionPhase = guessInjectionPhase(phaseNames, removalPhase);

        String processName = null;
        SaveableData sd = context.getData().getInheritableValue(taskPath,
            TeamDataConstants.PROCESS_NAME);
        if (sd != null)
            processName = sd.getSimpleValue().format();

        DefectPhaseList result = new DefectPhaseList();
        for (String onePhase : phaseNames) {
            DefectPhase dp = new DefectPhase(onePhase);
            dp.processName = processName;

            if (onePhase.equals(injectionPhase))
                result.defaultInjectionPhase = result.size();
            if (onePhase.equals(removalPhase))
                result.defaultRemovalPhase = result.size();

            result.add(dp);
        }

        return result;
    }


    /** Return the list of phases that should be displayed in a drop-down
     * list for selection as defect injection and removal phases.
     * 
     * @param defectPath the path to a task in the dashboard hierarchy
     * @param context the dashboard context
     * @return a list of phases
     */
    public static List getDefectPhases(String defectPath,
            DashboardContext context) {
        int prefixLength = 0;

        List result = new ArrayList();

        Enumeration leafNames = getInheritedPhaseList(defectPath, context
                .getData());
        if (leafNames == null) {
            DashHierarchy hier = context.getHierarchy();
            PropertyKey defectPathKey = hier.findExistingKey(defectPath);
            if (defectPathKey == null)
                return result;
            leafNames = hier.getLeafNames(defectPathKey);
            prefixLength = defectPath.length() + 1;
        }

        while (leafNames.hasMoreElements()) {
            String item = (String) leafNames.nextElement();
            if (item == null || item.length() <= prefixLength) continue;
            item = item.substring(prefixLength);

            // This is NOT the right way to do this. A better way would be to
            // look at the defect flag of each leaf.  Leaves that wanted to
            // forbid defects could set their flag to false. But this will work...
            if (item.endsWith("Postmortem") || item.endsWith("Reassessment"))
                continue;           // don't add to the list.
            result.add(item);
        }

        return result;
    }


    protected static Enumeration getInheritedPhaseList(String defectPath,
            DataRepository data) {
        Object inheritedPhaseList = data.getInheritableValue
            (defectPath, "Effective_Defect_Phase_List");
        ListData list = null;
        if (inheritedPhaseList instanceof ListData)
            list = (ListData) inheritedPhaseList;
        else if (inheritedPhaseList instanceof StringData)
            list = ((StringData) inheritedPhaseList).asList();

        if (list == null)
            return null;

        Vector result = new Vector();
        for (int i = 0;   i < list.size();   i++)
            result.add(list.get(i).toString());
        return result.elements();
    }

    /** Make an educated guess about which removal phase might correspond to
     * a particular dashboard task
     */
    public static String guessRemovalPhase(String defectPath,
            String taskPath, DashboardContext context) {

        // first, check to see if this task has registered an effective phase
        ProcessUtil pu = new ProcessUtil(context.getData());
        String effectivePhase = pu.getEffectivePhase(taskPath, false);
        if (effectivePhase != null)
            return effectivePhase;

        // if no effective phase was registered, infer it from the path.  We
        // don't use the path inference provided by ProcessUtil because we
        // need to preserve more than just the final path segment. For example,
        // in the case of a PSP3 project, we need to keep both the cycle name
        // and the phase name.
        int prefixLength = defectPath.length() + 1;
        if (taskPath.length() > prefixLength
                && Filter.pathMatches(taskPath, defectPath))
            return taskPath.substring(prefixLength);

        // no luck so far.  Look at the task in question, and see if it only
        // includes a single phase child (typical for old-style team projects
        // with phase stubs).
        DashHierarchy hier = context.getHierarchy();
        PropertyKey defectPathKey = hier.findExistingKey(defectPath);
        if (defectPathKey != null) {
            Enumeration leafNames = hier.getLeafNames(defectPathKey);
            List possibleMatches = new ArrayList();
            while (leafNames.hasMoreElements()) {
                String oneLeaf = (String) leafNames.nextElement();
                if (oneLeaf.length() > prefixLength) {
                    String leafTail = oneLeaf.substring(prefixLength);
                    if (leafTail.indexOf('/') == -1)
                        possibleMatches.add(leafTail);
                }
            }
            if (possibleMatches.size() == 1)
                return (String) possibleMatches.get(0);
        }


        return null;
    }



    /** Make an educated guess about which injection phase might correspond
     *  to the given removal phase.
     */
    public static String guessInjectionPhase(List phases, String removalPhase)
    {
        if (removalPhase == null || removalPhase.trim().length() == 0)
            return null;

        String result, mappedGuess, onePhase;

        // first, check the user's phase map setting for a potential match.
        int pos = removalPhase.lastIndexOf('/');
        if (pos == -1)
            mappedGuess = (String) INJ_REM_PAIRS.get(removalPhase);
        else
            mappedGuess =
                (String) INJ_REM_PAIRS.get(removalPhase.substring(pos+1));

        // next, guess that reviews/inspections are removing defects found
        // in a corresponding phase.
        if (mappedGuess == null
                && (removalPhase.endsWith(" Review")
                        || removalPhase.endsWith(" Inspection")
                        || removalPhase.endsWith(" Inspect"))) {
            int spacePos = removalPhase.lastIndexOf(' ');
            mappedGuess = removalPhase.substring(0, spacePos);
        }

        // now, find the removal phase in the current phase list.
        int i = phases.size();
        while (i-- > 0)
            if (removalPhase.equals(phases.get(i))) break;

        // next, walk backward through the list, looking for an instance of
        // the mappedGuess, or for the previous suspected non-quality phase
        result = null;
        while (i-- > 0) {
            onePhase = (String) phases.get(i);
            if (phaseMatches(onePhase, mappedGuess))
                return onePhase;
            if (result == null &&
                !onePhase.endsWith(" Review") &&
                !onePhase.endsWith(" Inspection") &&
                !onePhase.endsWith(" Inspect") &&
                !onePhase.endsWith("Compile") &&
                !onePhase.endsWith("Test"))
                // remember the first non-quality, non-failure phase
                // we find before the removalPhase.
                result = onePhase;
        }
        if (result == null)
            result = removalPhase;

        return result;
    }

    private static boolean phaseMatches(String fullName, String phaseName) {
        if (fullName == null || phaseName == null) return false;

        int pos = fullName.lastIndexOf('/');
        if (pos != -1)
            fullName = fullName.substring(pos+1);

        return fullName.equalsIgnoreCase(phaseName);
    }

    private static Map INJ_REM_PAIRS;
    static {
        HashMap phaseMap = new HashMap();
        String userSetting = Settings.getVal("defectDialog.phaseMap");
        if (userSetting != null) {
            StringTokenizer tok = new StringTokenizer(userSetting, "|");
            String phasePair, rem, inj;
            int pos;
            while (tok.hasMoreTokens()) {
                phasePair = tok.nextToken();
                pos = phasePair.indexOf("=>");
                if (pos != -1) {
                    inj = phasePair.substring(0, pos).trim();
                    rem = phasePair.substring(pos+2).trim();
                    phaseMap.put(rem, inj);
                }
            }
        }
        INJ_REM_PAIRS = Collections.unmodifiableMap(phaseMap);
    }
}
