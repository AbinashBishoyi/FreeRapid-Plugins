package cz.vity.freerapid.plugins.services.direct;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://link.songspk.info/indian_movie/J_List/download.php?id=1159"));
            //httpFile.setNewURL(new URL("http://news.detik.com/read/2012/09/18/130549/2023965/10/penjaga-rumah-ditemukan-tewas-sambil-menggenggam-celana-dalam-wanita"));
            //httpFile.setNewURL(new URL("http://www.songspk.info/indian_movie/Jism2-2012x.html"));
            //httpFile.setNewURL(new URL("http://wordrider.net/forum/10/10748/10756/_subject_#msg-10756"));
            httpFile.setNewURL(new URL("http://downloads.mr-khan.com/load/Exclusive_Single/128/Kathe%20Hon%20Jadon%20Chaar%20Punjabi-%20-%20Nachhatar%20Gill%20-Guru%20(Desi%20Beam).mp3"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            // connectionSettings.setProxy("localhost", 8081);
            testRun(new DirectDownloadServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}