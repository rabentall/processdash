// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import pspdash.data.DataRepository;
import pspdash.data.StringData;
import pspdash.data.SimpleData;

public class EVTaskListXML extends EVTaskListXMLAbstract {

    public static final String XML_DATA_NAME = "XML Task List";
    protected DataRepository data;

    public EVTaskListXML(String taskListName, DataRepository data) {
        super(taskListName, null, false);
        this.data = data;

        if (!openXML(data, taskListName))
            createErrorRootNode(cleanupName(taskListName),
                                "Task list missing");
    }

    private boolean openXML(DataRepository data, String taskListName) {
        String dataName = data.createDataName(taskListName, XML_DATA_NAME);
        SimpleData value = data.getSimpleValue(dataName);
        if (!(value instanceof StringData))
            return false;
        String xmlDoc = value.format();
        return openXML(xmlDoc, cleanupName(taskListName));
    }

    public void recalc() {
        if (!openXML(data, taskListName))
            createErrorRootNode(cleanupName(taskListName),
                                "Task list missing");
        super.recalc();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.indexOf(MAIN_DATA_PREFIX) != -1);
    }

    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = data.createDataName(taskListName, XML_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }

    public static String taskListNameFromDataElement(String dataName) {
        if (dataName == null ||
            dataName.indexOf(MAIN_DATA_PREFIX) == -1 ||
            !dataName.endsWith("/" + XML_DATA_NAME))
            return null;

        return dataName.substring
            (0, dataName.length() - XML_DATA_NAME.length() - 1);
    }

}
