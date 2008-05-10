//This class shared from the Giant Java Tree, http://www.gjt.org
//originally in package org.gjt.fredde.util.gui;

/*  AboutDialog - Displays text in a JEditorPane
 *  Copyright (C) 1999 Fredrik Ehnbom
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.sourceforge.processdash.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.help.PCSH;

/**
 * Displays information about an application in a JEditorPane.
 */
public class AboutDialog extends JDialog implements HyperlinkListener {

    private static final int WINDOW_WIDTH = 560;
    private static final int WINDOW_HEIGHT = 350;

    private static final String ABOUT_TEXT_LOCATION = "/help/Topics/Overview/about.htm";
    private static final String CREDITS_TEXT_LOCATION = "/help/Topics/Overview/credits.htm";
    private static final String CONFIGURATION_TEXT_LOCATION = "/control/showenv.class?brief";

    private static final String ABOUT_LOGO_LOCATION = "about.png";

    Resources resources = Resources.getDashBundle("ProcessDashboard.About");

    /**
     * Creates a new AboutDialog.
     * @param text The text to display. If it begins with &lt;html&gt; it'll
     *         display directly what you wrote or else it asumes that
     *        you wanted it to display a location.
     */
    public AboutDialog(Frame parent, String title, String text) {
        super(parent, title);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Container pane = this.getContentPane();

        // The logo which is outside the tabbed pane
        Icon dashLogoIcon = getDashLogo();
        JLabel dashLogo = new JLabel(dashLogoIcon);
        dashLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pane.add(new JLabel(dashLogoIcon), BorderLayout.PAGE_START);

        // The about content
        JTabbedPane tabbedPane = new JTabbedPane();
        pane.add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab(resources.getString("Tab.About"),
                          getContentPanel(ABOUT_TEXT_LOCATION));
        tabbedPane.addTab(resources.getString("Tab.Credits"),
                          getContentPanel(CREDITS_TEXT_LOCATION));
        tabbedPane.addTab(resources.getString("Tab.Configuration"),
                          getContentPanel(CONFIGURATION_TEXT_LOCATION));

        // The OK button, below the tabbed pane.
        JPanel buttonPane = new JPanel();
        JButton okButton = new JButton(resources.getString("OK"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
            });
        buttonPane.add(okButton);
        pane.add(buttonPane, BorderLayout.PAGE_END);

        setVisible(true);
    }

    private JPanel getContentPanel(String textLocation) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JEditorPane editorPane = getEditorPane(textLocation);

        panel.add(new JScrollPane(editorPane));
        return panel;
    }

    private JEditorPane getEditorPane(String text) {
        JEditorPane editorPane = new JEditorPane();

        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(this);
        editorPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (text.toLowerCase().startsWith("<html>")) {
            editorPane.setText(text);
        } else {
            try {
                editorPane.setPage(Browser.mapURL(text));
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }

        return editorPane;
    }

    private static ImageIcon getDashLogo() {
        URL url = AboutDialog.class.getResource(ABOUT_LOGO_LOCATION);
        return new ImageIcon(url);
    }

    /**
     * Launch a browser when you click on a link
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url = e.getURL().toString();
            if (url.startsWith("http://help/"))
                PCSH.displayHelpTopic(url.substring(12));
            else
                Browser.launch(url);
        }
    }
}
