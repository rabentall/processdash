package teamdash.templates.setup;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlSerializer;

import teamdash.templates.setup.SyncDiscrepancy.EVSchedule;
import teamdash.templates.setup.SyncDiscrepancy.PlanTime;

public class UserDataWriter extends TinyCGIBase {


    private static final String FORCE_REFRESH_PARAM = "forceRefresh";


    @Override
    protected void writeHeader() {
        out.print("Content-type: text/xml; charset=UTF-8\r\n\r\n");
        out.flush();
    }

    @Override
    protected void writeContents() throws IOException {
        String processID = getStringValue("Team_Process_PID");
        String processVersion = TemplateLoader.getPackageVersion(processID);
        String initials = getStringValue(TeamDataConstants.INDIV_INITIALS);
        Date timestamp = new Date();

        XmlSerializer ser = XMLUtils.getXmlSerializer(true);
        ser.setOutput(outStream, ENCODING);
        ser.startDocument(ENCODING, Boolean.TRUE);

        ser.startTag(null, DOCUMENT_TAG);
        ser.attribute(null, PROCESS_ID_ATTR, processID);
        ser.attribute(null, VERSION_ATTR, processVersion);
        ser.attribute(null, INITIALS_ATTR, initials);
        ser.attribute(null, TIMESTAMP_ATTR, XMLUtils.saveDate(timestamp));

        if (hasValue(getData("Enable_Reverse_Sync"))) {
            try {
                writeDiscrepancies(ser);
                writeNewTasks(ser, processID);
                writeActualData(ser);
            } catch (WrappedIOException wie) {
                throw wie.getIOException();
            }
        }

        ser.endTag(null, DOCUMENT_TAG);
        ser.endDocument();
    }

    private void writeNewTasks(XmlSerializer ser, String processID)
            throws IOException {
        DashHierarchy hier = getPSPProperties();
        PropertyKey projectRoot = hier.findExistingKey(getPrefix());
        String phaseDataName = HierarchySynchronizer
                .getEffectivePhaseDataName(processID);
        writeNewTasks(ser, hier, projectRoot, "root", phaseDataName);
    }

    private void writeNewTasks(XmlSerializer ser, DashHierarchy hier,
            PropertyKey parent, String parentID, String phaseDataName)
            throws IOException {

        String prevSiblingName = null;
        String prevSiblingID = null;
        for (int i = 0;  i < hier.getNumChildren(parent);  i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            String childID = getWbsIdForPath(child.path());

            if (hasValue(childID)) {
                // this task has a WBS ID, so it must have come from the WBS
                // originally.  It isn't a new task.  So just recurse and
                // look for new tasks underneath it.
                if (!isPSPTask(child.path()))
                    writeNewTasks(ser, hier, child, childID, phaseDataName);

            } else {
                // this task does not have a WBS ID, so it is new.  Write a
                // "new task" entry for it.
                String nextSiblingID = null;
                if (prevSiblingName == null && prevSiblingID == null)
                    nextSiblingID = getFirstTaskID(hier, parent);
                writeNewTask(ser, hier, parentID, child, prevSiblingName,
                    prevSiblingID, nextSiblingID, phaseDataName);
            }

            prevSiblingName = child.name();
            prevSiblingID = childID;
        }
    }

    private String getFirstTaskID(DashHierarchy hier, PropertyKey parent) {
        for (int i = 0;  i < hier.getNumChildren(parent);  i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            String childID = getWbsIdForPath(child.path());
            if (hasValue(childID))
                return childID;
        }
        return null;
    }

    private void writeNewTask(XmlSerializer ser, DashHierarchy hier,
            String parentID, PropertyKey node, String previousSiblingName,
            String previousSiblingID, String subsequentSiblingID,
            String phaseDataName) throws IOException {

        ser.startTag(null, NEW_TASK_TAG);

        // write the name of the task
        ser.attribute(null, TASK_NAME_ATTR, node.name());

        // write information about where the task is located in the WBS
        writeAttr(ser, PARENT_ID_ATTR, parentID);
        writeAttr(ser, PREV_SIBLING_NAME_ATTR, previousSiblingName);
        writeAttr(ser, PREV_SIBLING_ID_ATTR, previousSiblingID);
        writeAttr(ser, NEXT_SIBLING_ID_ATTR, subsequentSiblingID);

        // write estimated and actual data for the task
        String path = node.path();
        SimpleData estimatedTime = getData(path, "Estimated Time");
        SimpleData actualTime = getData(path, "Time");
        SimpleData startDate = getData(path, "Started");
        SimpleData completionDate = getData(path, "Completed");
        writeTimeDataAttr(ser, EST_TIME_ATTR, estimatedTime);
        writeTimeDataAttr(ser, TIME_ATTR, actualTime);
        writeActualDataAttr(ser, START_DATE_TAG, startDate);
        writeActualDataAttr(ser, COMPLETION_DATE_TAG, completionDate);

        if (isPSPTask(path)) {
            // if this is a PSP project, write the node type "/PSP/"
            writeAttr(ser, NODE_TYPE_ATTR, "/PSP/");

        } else {
            // otherwise, write the node type if there is one
            String phaseType = getStringData(getData(path, phaseDataName));
            if (phaseType != null && !phaseType.startsWith("?"))
                writeAttr(ser, NODE_TYPE_ATTR, phaseType);

            // recurse over subtasks
            for (int i = 0;  i < hier.getNumChildren(node);  i++) {
                PropertyKey subtask = hier.getChildKey(node, i);
                writeNewTask(ser, hier, null, subtask, null, null, null,
                    phaseDataName);
            }
        }

        ser.endTag(null, NEW_TASK_TAG);
    }

    private void writeAttr(XmlSerializer ser, String name, String value)
            throws IOException {
        if (hasValue(value))
            ser.attribute(null, name, value);
    }

    private void writeActualData(XmlSerializer ser) throws IOException {
        DashHierarchy hier = getPSPProperties();
        PropertyKey projectRoot = hier.findExistingKey(getPrefix());
        writeActualData(ser, hier, projectRoot, true);
    }

    private void writeActualData(XmlSerializer ser, DashHierarchy hier,
            PropertyKey node, boolean isRoot) throws IOException {

        if (!isRoot)
            writeActualDataForNode(ser, node);

        int numChildren = hier.getNumChildren(node);
        for (int i = 0;  i < numChildren;  i++) {
            PropertyKey child = hier.getChildKey(node, i);
            writeActualData(ser, hier, child, false);
        }
    }

    private void writeActualDataForNode(XmlSerializer ser, PropertyKey node)
            throws IOException {
        String path = node.path();
        String wbsID = getWbsIdForPath(path);
        if (!hasValue(wbsID))
            return;

        SimpleData actualTime = getData(path, "Time");
        SimpleData startDate = getData(path, "Started");
        SimpleData completionDate = getData(path, "Completed");

        if (hasValue(actualTime) || hasValue(startDate)
                || hasValue(completionDate)) {
            ser.startTag(null, ACTUAL_DATA_TAG);
            ser.attribute(null, WBS_ID_ATTR, wbsID);
            writeTimeDataAttr(ser, TIME_ATTR, actualTime);
            writeActualDataAttr(ser, START_DATE_TAG, startDate);
            writeActualDataAttr(ser, COMPLETION_DATE_TAG, completionDate);
            ser.endTag(null, ACTUAL_DATA_TAG);
        }
    }

    private void writeTimeDataAttr(XmlSerializer ser, String attrName,
            SimpleData time) throws IOException {
        if (hasValue(time) && time instanceof DoubleData) {
            double hours = ((DoubleData) time).getDouble() / 60.0;
            ser.attribute(null, attrName, Double.toString(hours));
        }
    }

    private void writeActualDataAttr(XmlSerializer ser, String attrName,
            SimpleData value) throws IOException {
        if (hasValue(value)) {
            if (value instanceof StringData)
                ser.attribute(null, attrName, value.format());
            else
                ser.attribute(null, attrName, value.saveString());
        }
    }

    private boolean hasValue(SimpleData value) {
        return value != null && value.test();
    }

    private void writeDiscrepancies(XmlSerializer ser) throws IOException {
        ListData discrepancies = getDiscrepancyList();
        if (discrepancies != null && discrepancies.size() > 1) {
            DiscrepancyWriter dw = new DiscrepancyWriter(ser);
            for (int i = 1; i < discrepancies.size(); i++) {
                SyncDiscrepancy d = (SyncDiscrepancy) discrepancies.get(i);
                d.visit(dw);
            }
        }
    }

    private ListData getDiscrepancyList() throws IOException {
        ListData discrepancies = ListData.asListData(getData(
            SyncDiscrepancy.DISCREPANCIES_DATANAME));

        if (needsRefresh(discrepancies)) {
            refreshDiscrepancyList();
            discrepancies = ListData.asListData(getData(
                SyncDiscrepancy.DISCREPANCIES_DATANAME));
        }

        return discrepancies;
    }

    private boolean needsRefresh(ListData discrepancies) {
        if (discrepancies == null || discrepancies.size() < 1
                || parameters.containsKey(FORCE_REFRESH_PARAM))
            return true;
        Date timestamp = (Date) discrepancies.get(0);
        long age = System.currentTimeMillis() - timestamp.getTime();
        return (age > REFRESH_AGE_CUTOFF);
    }

    private void refreshDiscrepancyList() throws IOException {
        String syncUri = resolveRelativeURI("sync.class");
        getRequest(syncUri, false);
    }

    private SimpleData getData(String name) {
        return getDataContext().getSimpleValue(name);
    }

    private SimpleData getData(String prefix, String name) {
        String dataName = DataRepository.createDataName(prefix, name);
        return getDataContext().getSimpleValue(dataName);
    }

    private String getStringData(SimpleData sd) {
        return (sd == null ? "" : sd.format());
    }

    private String getStringValue(String name) {
        return getStringData(getData(name));
    }

    private String getWbsIdForPath(String path) {
        return getStringData(getData(path, TeamDataConstants.WBS_ID_DATA_NAME));
    }

    private boolean isPSPTask(String path) {
        return hasValue(getData(path, "PSP Project"));
    }

    private static boolean hasValue(String s) {
        return StringUtils.hasValue(s);
    }

    private class WrappedIOException extends RuntimeException {

        public WrappedIOException(IOException ioe) {
            super(ioe);
        }

        public IOException getIOException() {
            return (IOException) getCause();
        }

    }

    private class DiscrepancyWriter implements SyncDiscrepancy.Visitor {

        XmlSerializer ser;

        public DiscrepancyWriter(XmlSerializer ser) {
            this.ser = ser;
        }

        public void visit(PlanTime p) {
            try {
                ser.startTag(null, PLAN_TIME_CHANGE_TAG);
                ser.attribute(null, PATH_ATTR, p.getPath());
                ser.attribute(null, WBS_ID_ATTR, p.getWbsId());
                ser.attribute(null, TIME_ATTR, Double.toString(p
                        .getPlannedHours()));
                ser.endTag(null, PLAN_TIME_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        public void visit(EVSchedule s) {
            Map<Date, Double> changes = s.getExceptions();
            if (changes == null || changes.isEmpty())
                return;

            try {
                ser.startTag(null, SCHEDULE_CHANGE_TAG);
                for (Map.Entry<Date, Double> e : changes.entrySet()) {
                    Date when = e.getKey();
                    Double hours = e.getValue();
                    ser.startTag(null, SCHEDULE_EXCEPTION_TAG);
                    ser.attribute(null, WHEN_ATTR, XMLUtils.saveDate(when));
                    if (hours != null)
                        ser.attribute(null, HOURS_ATTR, hours.toString());
                    else
                        ser.attribute(null, HOURS_ATTR, DEFAULT_HOURS_VAL);
                    ser.endTag(null, SCHEDULE_EXCEPTION_TAG);
                }
                ser.endTag(null, SCHEDULE_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

    }

    private static final long REFRESH_AGE_CUTOFF = 60 * 1000;

    private static final String ENCODING = "UTF-8";

    private static final String DOCUMENT_TAG = "userData";

    private static final String PROCESS_ID_ATTR = "teamProcessID";

    private static final String VERSION_ATTR = "dumpFileVersion";

    private static final String INITIALS_ATTR = "initials";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String NEW_TASK_TAG = "newTask";

    private static final String TASK_NAME_ATTR = "name";

    private static final String PARENT_ID_ATTR = "parentWbsId";

    private static final String PREV_SIBLING_ID_ATTR = "prevSiblingId";

    private static final String PREV_SIBLING_NAME_ATTR = "prevSiblingName";

    private static final String NEXT_SIBLING_ID_ATTR = "nextSiblingId";

    private static final String EST_TIME_ATTR = "estTime";

    private static final String NODE_TYPE_ATTR = "nodeType";

    private static final String ACTUAL_DATA_TAG = "actualData";

    private static final String START_DATE_TAG = "started";

    private static final String COMPLETION_DATE_TAG = "completed";

    private static final String PLAN_TIME_CHANGE_TAG = "planTimeChange";

    private static final String PATH_ATTR = "path";

    private static final String WBS_ID_ATTR = "wbsId";

    private static final String TIME_ATTR = "time";

    private static final String SCHEDULE_CHANGE_TAG = "scheduleChange";

    private static final String SCHEDULE_EXCEPTION_TAG = "scheduleException";

    private static final String WHEN_ATTR = "when";

    private static final String HOURS_ATTR = "hours";

    private static final String DEFAULT_HOURS_VAL = "DEFAULT";

}
