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

package teamdash.hist.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import teamdash.wbs.WBSEditor;

public class BlameHistoryAction extends AbstractAction {

    private WBSEditor wbsEditor;

    private JFrame frame;

    private String dataLocation;

    private BlameHistoryDialog dialog;

    public BlameHistoryAction(WBSEditor wbsEditor, JFrame frame,
            String dataLocation) {
        super(BlameHistoryDialog.resources.getString("Menu_Text"));
        this.wbsEditor = wbsEditor;
        this.frame = frame;
        this.dataLocation = dataLocation;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new BlameHistoryDialog(wbsEditor, frame, dataLocation);
        } else {
            dialog.setVisible(true);
        }
    }

}
