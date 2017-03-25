// Copyright (C) 1998-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class PauseIcon extends AbstractPixelAwareRecolorableIcon {

    public static PauseIcon black() {
        return new PauseIcon(Color.black, null);
    }

    public static PauseIcon glowing() {
        return new PauseIcon(Color.black, new Color(50, 50, 255));
    }

    public static PauseIcon disabled() {
        return new PauseIcon(new Color(100, 100, 100), null);
    }


    private int pad;

    private Color fill, highlight, shadow, innerGlow;

    public PauseIcon(Color fill, Color glow) {
        this(DashboardIconFactory.getStandardIconSize(),
                DashboardIconFactory.getStandardIconPad(), //
                fill, glow);
    }

    public PauseIcon(int size, int pad, Color fill, Color glow) {
        this.width = this.height = size;
        this.pad = pad;
        this.fill = fill;
        this.highlight = Color.white;
        this.shadow = Color.gray;
        if (glow != null)
            this.innerGlow = PaintUtils.makeTransparent(glow, 80);
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float scale) {
        int pad = (int) (this.pad * width / (double) this.width);
        int barHeight = height - 2 * pad;
        int barWidth = (int) (0.5 + (width - 2 * pad) * 2 / 5f);
        int barSpacing = width - 2 * (pad + barWidth);

        paintBar(g2, width, height, pad, pad, barWidth - 1, barHeight - 1);
        paintBar(g2, width, height, pad + barWidth + barSpacing, pad, //
            barWidth - 1, barHeight - 1);
    }

    private void paintBar(Graphics2D g2, int totalWidth, int totalHeight,
            int barLeft, int barTop, int barWidth, int barHeight) {
        // fill the background area
        Rectangle bar = new Rectangle(barLeft, barTop, barWidth, barHeight);
        g2.setColor(fill);
        g2.setClip(null);
        g2.fill(bar);

        // paint the glow, if applicable
        if (innerGlow != null) {
            g2.setColor(innerGlow);
            g2.setClip(bar);
            int midX = barLeft + barWidth / 2;
            int midY = barTop + barHeight / 2;
            for (int i = 8; i-- > 0; ) {
                int r = (int) (i * totalHeight / 17f);
                g2.fillOval(midX - r, midY - r, r * 2 + 1, r * 2 + 1);
            }
        }

        // prepare to draw the beveled inset edges on the bar
        float bevelWidth = 1 + (float) Math.pow(totalWidth - 17, 0.8) / 3f;
        g2.setStroke(new BasicStroke(bevelWidth));
        int barBottom = barTop + barHeight + 1;
        int barRight = barLeft + barWidth + 1;
        int bevel = barWidth / 2;

        // draw the shadow on the top-left
        g2.setColor(shadow);
        g2.setClip(shape(barLeft, barTop, barRight, barTop, //
            barRight - bevel, barTop + bevel, //
            barLeft + bevel, barBottom - bevel, //
            barLeft, barBottom));
        g2.draw(bar);

        // draw the highlight on the bottom-right
        g2.setColor(highlight);
        g2.setClip(shape(barRight, barBottom, barRight, barTop, //
            barRight - bevel, barTop + bevel, //
            barLeft + bevel, barBottom - bevel, //
            barLeft, barBottom));
        g2.draw(bar);
    }

}
