import java.io.IOException;
import java.net.URLEncoder;

import pspdash.TinyCGIBase;
import pspdash.TinyWebServer;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;

public class inclTeamTools extends TinyCGIBase {

    private static final String WBS_EDITOR_URL =
        "../../team/tools/index.shtm?directory=";
    private static final String SYNC_PARAM = "&syncURL=";
    private static final String SYNC_URL = "sync.class?run";

    protected void writeContents() throws IOException {
        try {
            String prefix = getPrefix();
            if (prefix == null) return;

            DataRepository data = getDataRepository();
            String dataName = DataRepository.createDataName
                (prefix, "Team_Data_Directory");
            SimpleData d = data.getSimpleValue(dataName);
            if (d == null || !d.test()) {
                out.print(TEAM_DIR_MISSING_MSG);
                return;
            }

            String directory = d.format();
            String wbsURL = WBS_EDITOR_URL + URLEncoder.encode(directory);
            String scriptPath = (String) env.get("SCRIPT_PATH");
            String uri = resolveRelativeURI(scriptPath, wbsURL);

            String syncURI = resolveRelativeURI(scriptPath, SYNC_URL);
            String syncURL = "http://" + TinyWebServer.getHostName() +
                ":" + getTinyWebServer().getPort() + syncURI;
            uri = uri + SYNC_PARAM + URLEncoder.encode(syncURL);

            outStream.write(getRequest(uri, true));
        } catch (Exception e) {
            out.print(TOOLS_MISSING_MSG);
        }
    }

    private static final String TEAM_DIR_MISSING_MSG =
            "<html><body>" +
            "<p><b>The advanced team tools (such as the Custom Process Editor " +
            "and the Work Breakdown Structure Editor) cannot be used until you " +
            "specify a team data directory on the project parameters page.</b>" +
            "</body></html>";

    private static final String TOOLS_MISSING_MSG =
        "<html><body>" +
        "<p><b>The advanced team tools (such as the Custom Process Editor " +
        "and the Work Breakdown Structure Editor) have not been installed " +
        "in this instance of the dashboard.</b>" +
        "</body></html>";

}
