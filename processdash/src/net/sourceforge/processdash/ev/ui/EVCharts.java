// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.util.Map;

import org.jfree.data.xy.XYDataset;

public class EVCharts {

    /** A widget that displays a cumulative earned value chart */
    public static class Value extends AbstractEVChart {

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getValueChartData();
        }

    }

    /** A widget that displays a cumulative direct time chart */
    public static class DirectTime extends AbstractEVChart {

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getTimeChartData();
        }

    }

    /** A widget that displays a combined chart */
    public static class Combined extends AbstractEVChart {

        @Override
        protected XYDataset createDataset(Map env, Map params) {
            return getSchedule(env).getCombinedChartData();
        }

    }

}
