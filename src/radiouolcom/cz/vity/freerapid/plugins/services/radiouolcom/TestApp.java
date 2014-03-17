package cz.vity.freerapid.plugins.services.radiouolcom;

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
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/programa/heavy-nation/edicao/14306639"));
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/#/programa/lado-bi/edicao/14312793"));
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/letras-e-musicas/eric-clapton/strange-brew/2474516"));
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/#/letras-e-musicas/daniel/tantinho/2677666"));
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/#/letras-e-musicas/psy/gangnam-style/2661687"));
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/musica/dado-villa-lobos/piretrum-partenium/194640")); //redirected to letras-e-musicas
            //httpFile.setNewURL(new URL("http://www.radio.uol.com.br/#/album/varios-artistas/bufo-e-spallanzani---trilha-sonora-original/18146"));
            httpFile.setNewURL(new URL("http://www.radio.uol.com.br/musica/cassia-eller/bufo-e-spallanzani--dentro-de-ti/194642"));

            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("187.85.179.170", 3128); //eg we can use local proxy to sniff HTTP communication
            final RadioUolComServiceImpl service = new RadioUolComServiceImpl();
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