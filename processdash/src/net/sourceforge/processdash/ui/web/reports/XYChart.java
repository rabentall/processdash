// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ui.web.reports;


import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.web.CGIChartBase;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.StandardXYItemRenderer;
import org.jfree.data.XYDataset;

import pspdash.data.DateData;


public class XYChart extends CGIChartBase {

    /** Create a scatter plot. */
    public JFreeChart createChart() {
        JFreeChart chart;
        String xLabel = null, yLabel = null;
        if (!chromeless) {
            xLabel = Translator.translate(data.getColName(1));

            yLabel = getSetting("yLabel");
            if (yLabel == null && data.numCols() == 2)
                yLabel = data.getColName(2);
            if (yLabel == null) yLabel = getSetting("units");
            if (yLabel == null) yLabel = "Value";
            yLabel = Translator.translate(yLabel);
        }

        if ((data.numRows() > 0 && data.numCols() > 0 &&
             data.getData(1,1) instanceof DateData) ||
            parameters.get("xDate") != null)
            chart = ChartFactory.createTimeSeriesChart
                (null, xLabel, yLabel, data.xyDataSource(), true, true, false);
        else {
            XYDataset src = data.xyDataSource();
            chart = ChartFactory.createScatterPlot
                (null, xLabel, yLabel, src, PlotOrientation.VERTICAL,
                 true, false, false);

            String trendLine = getParameter("trend");
            if ("none".equalsIgnoreCase(trendLine))
                ;
            else if ("average".equalsIgnoreCase(trendLine))
                addTrendLine(chart, XYDataSourceTrendLine.getAverageLine(src));
            else
                addTrendLine(chart,
                             XYDataSourceTrendLine.getRegressionLine(src));
        }

        if (data.numCols() == 2)
            chart.setLegend(null);

        return chart;
    }

    private void addTrendLine(JFreeChart chart, XYDataset dataset) {
        XYPlot plot = chart.getXYPlot();
        plot.setSecondaryDataset(0, dataset);
        plot.setSecondaryRenderer
            (0, new StandardXYItemRenderer(StandardXYItemRenderer.LINES));
    }

    public void massageParameters() {
        //parameters.put("order", parameters.get("d1"));
    }

    protected Axis getAxis(JFreeChart chart, PlotOrientation dir) {
        try {
            XYPlot p = chart.getXYPlot();
            if (dir.equals(p.getOrientation()))
                return p.getRangeAxis();
            else
                return p.getDomainAxis();
        } catch (Exception e) {
            return null;
        }
    }

}
