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

import java.io.IOException;
import pspdash.StringUtils;
import pspdash.data.DataRepository;
import pspdash.data.ResultSet;

public class excel extends pspdash.TinyCGIBase {

    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Content-type: application/octet-stream\r\n\r\n");
        out.flush();
    }

    protected void writeContents() throws IOException {
        out.println("WEB");
        out.println("1");
        String hostPort = (String) env.get("HTTP_HOST");
        if (hostPort == null) {
            String host = (String) env.get("SERVER_NAME");
            String port = (String) env.get("SERVER_PORT");
            hostPort = host + ":" + port;
        }
        String url, uri = (String) env.get("REQUEST_URI");
        if (uri.indexOf('?') == -1) {
            url = (String) env.get("HTTP_REFERER");
            if (url == null)
                url = "http://" + hostPort + uri + "?qf=export.rpt";
        } else {
            uri=StringUtils.findAndReplace(uri,"/excel.iqy",  "/table.class");
            uri=StringUtils.findAndReplace(uri,"/excel.class","/table.class");
            url = "http://" + hostPort + uri;
        }
        out.println(url);
    }

}
