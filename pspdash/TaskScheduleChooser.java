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

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.*;

public class TaskScheduleChooser
    implements ActionListener, ListSelectionListener
{

    protected static Map openWindows = new Hashtable();

    protected PSPDashboard dash;
    protected JDialog dialog = null;
    protected JList list = null;
    protected JButton newButton, renameButton, deleteButton,
        cancelButton, okayButton;

    TaskScheduleChooser(PSPDashboard dash) {
        this(dash, EVTaskList.findTaskLists(dash.data));
    }
    TaskScheduleChooser(PSPDashboard dash, String[] templates) {
        if (templates == null || templates.length == 0)
            displayNewTemplateDialog(dash);
        else
            displayChooseTemplateDialog(dash, templates);
    }


    public void displayNewTemplateDialog(PSPDashboard dash) {
        if (dialog != null) dialog.dispose();

        String taskName = getTemplateName
            (dash, "Create New Schedule",
             "Choose a name for the new task & schedule template:");

        open(dash, taskName);
    }

    protected String getTemplateName(Component parent,
                                     String title, String prompt) {

        String taskName = "";
        Object message = prompt;
        while (true) {
            taskName = (String) JOptionPane.showInputDialog
                (parent, message, title,
                 JOptionPane.PLAIN_MESSAGE, null, null, taskName);


            if (taskName == null) return null; // user cancelled input

            taskName = taskName.trim();

            if (taskName.length() == 0)
                message = new String[] {
                    "Please enter a template name, or click cancel.", prompt };

            else if (taskName.indexOf('/') != -1)
                message = new String[] {
                    "The template name cannot contain the '/' character.",
                    prompt };

            else if (templateExists(taskName))
                message = new String[] {
                    "There is already a template with the name '" +
                    taskName + "'.",
                    prompt };

            else
                break;
        }
        return taskName;
    }

    private boolean templateExists(String taskListName) {
        String [] taskLists = EVTaskList.findTaskLists(dash.data);
        for (int i = taskLists.length;   i-- > 0; )
            if (taskListName.equals(taskLists[i]))
                return true;
        return false;
    }

    public void displayChooseTemplateDialog(PSPDashboard dash,
                                            String[] taskLists) {
        this.dash = dash;

        dialog = new JDialog();
        dialog.setTitle("Open/Create Task & Schedule");
        dialog.getContentPane().add
            (new JLabel("Choose a task & schedule template:"),
             BorderLayout.NORTH);

        list = new JList(taskLists);
        list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        openSelectedTaskList(); }});
        list.getSelectionModel().addListSelectionListener(this);
        dialog.getContentPane().add(new JScrollPane(list),
                                    BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttons.add(newButton = new JButton("New..."));
        newButton.addActionListener(this);

        buttons.add(renameButton = new JButton("Rename..."));
        renameButton.addActionListener(this);
        renameButton.setEnabled(false);

        buttons.add(deleteButton = new JButton("Delete..."));
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(false);

        buttons.add(cancelButton = new JButton("Cancel"));
        cancelButton.addActionListener(this);

        buttons.add(okayButton = new JButton("Open"));
        okayButton.addActionListener(this);
        okayButton.setEnabled(false);

        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(dialog.DISPOSE_ON_CLOSE);

        dialog.pack();
        dialog.show();
        dialog.toFront();
    }

    public static void open(PSPDashboard dash, String taskListName) {
        if (taskListName == null)
            return;
        TaskScheduleDialog d =
            (TaskScheduleDialog) openWindows.get(taskListName);
        if (d != null)
            d.show();
        else
            openWindows.put(taskListName,
                            new TaskScheduleDialog(dash, taskListName));
    }

    static void close(String taskListName) {
        openWindows.remove(taskListName);
    }


    public void valueChanged(ListSelectionEvent e) {
        boolean itemSelected = !list.getSelectionModel().isSelectionEmpty();
        renameButton.setEnabled(itemSelected);
        deleteButton.setEnabled(itemSelected);
        okayButton.setEnabled(itemSelected);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okayButton)
            openSelectedTaskList();
        else if (e.getSource() == newButton)
            displayNewTemplateDialog(dash);
        else if (e.getSource() == renameButton)
            renameSelectedTaskList();
        else if (e.getSource() == deleteButton)
            deleteSelectedTaskList();
        else if (e.getSource() == cancelButton)
            dialog.dispose();
    }

    protected void openSelectedTaskList() {
        open(dash, (String) list.getSelectedValue());
        if (dialog != null) dialog.dispose();
    }

    protected void renameSelectedTaskList() {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName == null) return;

        String newName = getTemplateName
            (dialog, "Rename Schedule",
             "Choose a new name for the task & schedule template '" +
             taskListName + "':");

        if (newName != null) {
            EVTaskList taskList =
                new EVTaskList(taskListName, dash.data, dash.props, false);
            taskList.save(newName);
            refreshList();
            dialog.toFront();
        }
    }

    protected void deleteSelectedTaskList() {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName == null) return;

        String message = "Are you certain you want to delete the task" +
            " & schedule template '" + taskListName + "'?";
        if (JOptionPane.showConfirmDialog(dialog,
                                          message,
                                          "Confirm Schedule Deletion",
                                          JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE)
            == JOptionPane.YES_OPTION) {
            EVTaskList taskList =
                new EVTaskList(taskListName, dash.data, dash.props, false);
            taskList.save(null);
            refreshList();
            dialog.toFront();
        }
    }

    protected void refreshList() {
        list.setListData(EVTaskList.findTaskLists(dash.data));
    }

    public static void closeAll() {
        Set s = new HashSet(openWindows.values());
        Iterator i = s.iterator();
        while (i.hasNext())
            ((TaskScheduleDialog) i.next()).confirmClose(false);
    }
}
