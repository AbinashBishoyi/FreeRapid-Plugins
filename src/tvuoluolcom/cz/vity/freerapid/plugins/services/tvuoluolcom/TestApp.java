package cz.vity.freerapid.plugins.services.tvuoluolcom;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://tvuol.uol.com.br/assistir.htm?currentPage=1&tagIds=3173&time=all&orderBy=mais-recentes&edFilter=editorial&discard_cache=true&video=cientistas-testam-se-e-possivel-fabricar-diamantes-em-casa-04028C1C3470D4994326"));
            //httpFile.setNewURL(new URL("http://tvuol.uol.com.br/assistir.htm?video=auto-cruze-hatch-aposta-nos-mais-exigentes-04024D1A356EC0C12326&tagIds=31909&orderBy=mais-recentes&edFilter=editorial&time=all&currentPage=1")); //HD
            //httpFile.setNewURL(new URL("http://tvuol.uol.com.br/assistir.htm?video=making-of-do-ensaio-da-exbbb-laisa-para-a-playboy-em-2011-04020D183072D0B12326"));
            //httpFile.setNewURL(new URL("http://tvuol.uol.com.br/assistir.htm?video=mulher-fica-nua-em-canteiro-da-br101-em-vitoria-04024D1A396EC0C12326"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final TvuolUolComServiceImpl service = new TvuolUolComServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}