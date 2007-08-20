// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

/**
 * This class is actually a simple JDateChooserCellEditor (from the JCalendar library)
 * that uses a dateChooser which can use any format to display a date. Furthermore,
 * it allows the use of the "Enter" key to validate changes made in a field.
 */
public class JDateTimeChooserCellEditor extends AbstractCellEditor
                                        implements TableCellEditor {

    private JDateChooser dateChooser;

    public JDateTimeChooserCellEditor(String format) {
        dateChooser = new JDateChooser(null, null, format,
            new JTextFieldDateTimeEditor());

        // Causes the Enter key to save changes
        JComponent uiComponent = dateChooser.getDateEditor().getUiComponent();
        if (uiComponent instanceof JTextField) {
            JTextField tf = (JTextField) uiComponent;
            tf.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing();
                }
            });

            tf.setBorder(null);
        }
    }

    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        Date date = null;
        if (value instanceof Date)
            date = (Date) value;

        dateChooser.setDate(date);

        return dateChooser;
    }

    public Object getCellEditorValue() {
        return dateChooser.getDate();
    }


    public static class JTextFieldDateTimeEditor extends JTextFieldDateEditor {
        public Date getDate() {
            try {
                date = dateFormatter.parse(getText());
            } catch (Exception e) {
                date = null;
            }
            return date;
        }
    }
}
