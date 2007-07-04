// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ev.ui;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JLabel;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.ThreadThrottler;

public class DependencyIndicator extends JLabel implements
        PropertyChangeListener {

    Window window;

    DashboardContext context;

    ActiveTaskModel taskModel;

    private Worker currentWorker = null;

    public DependencyIndicator(ProcessDashboard dash, ActiveTaskModel taskModel) {
        this.window = dash;
        this.context = dash;
        this.taskModel = taskModel;

        new ToolTipTimingCustomizer().install(this);
        taskModel.addPropertyChangeListener(this);
    }

    public void update() {
        setIcon(null);
        setText(null);
        setToolTipText(null);

        String taskPath = taskModel.getPath();
        new Worker(taskPath).start();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        update();
    }


    private class Worker extends SwingWorker {

        String taskPath;

        long requestTime;

        public Worker(String taskPath) {
            this.taskPath = taskPath;
            this.requestTime = System.currentTimeMillis();

            if (currentWorker != null)
                currentWorker.interrupt();
            currentWorker = this;
        }

        public Object construct() {
            ThreadThrottler.beginThrottling(0.2);
            try {
                return doCalc();
            } finally {
                ThreadThrottler.endThrottling();
            }
        }

        private Object doCalc() {
            EVDependencyCalculator calc = new EVDependencyCalculator(context
                    .getData(), context.getHierarchy(), context.getCache());
            String owner = ProcessDashboard.getOwnerName(context.getData());
            List dependencies = EVTaskDependency.getAllDependencies(context
                    .getData(), taskPath, owner);
            calc.recalculate(dependencies);

            // an immediate change to the dashboard will cause the window to
            // apparently "jump", which the user might find disconcerting.
            // wait a moment before setting the icon.
            long now = System.currentTimeMillis();
            long elapsed = now - requestTime;
            if (elapsed < 1000) {
                try {
                    Thread.sleep(1000 - elapsed);
                } catch (InterruptedException ie) {}
            }

            return dependencies;
        }


        public void finished() {
            if (currentWorker != this)
                return;

            List dependencies = (List) get();

            TaskDependencyAnalyzer.GUI a = new TaskDependencyAnalyzer.GUI(
                    dependencies);
            switch (a.getStatus()) {

            case TaskDependencyAnalyzer.NO_DEPENDENCIES:
            case TaskDependencyAnalyzer.ALL_COMPLETE:
                break;

            case TaskDependencyAnalyzer.HAS_ERROR:
            case TaskDependencyAnalyzer.HAS_INCOMPLETE:
            case TaskDependencyAnalyzer.HAS_REVERSE:
                a.syncLabel(DependencyIndicator.this);
                window.pack();
                break;
            }
        }

    }

}
