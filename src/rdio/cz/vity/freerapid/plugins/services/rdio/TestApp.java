package cz.vity.freerapid.plugins.services.rdio;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Armin_van_Buuren/album/A_State_Of_Trance_Year_Mix_2012_(Mixed_By_Armin_van_Buuren)/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Armin_van_Buuren/album/A_State_Of_Trance_Year_Mix_2012_(Mixed_By_Armin_van_Buuren)/track/The_Year_Of_Two_(Mix_Cut)_(A_State_Of_Trance_Year_Mix_2012_Intro)/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Miriam_Makeba/album/Makeba!/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Miriam_Makeba/album/Makeba!/track/Emavungwini_[Down_In_The_Dumps]/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Miriam_Makeba/album/Makeba!/track/Emavungwini_%5BDown_In_The_Dumps%5D/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Les_Mis%C3%A9rables_Cast/album/Les_Mis%C3%A9rables_The_Motion_Picture_Soundtrack_Deluxe_%28Deluxe_Edition%29/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Les_Mis%C3%A9rables_Cast/album/Les_Mis%C3%A9rables_The_Motion_Picture_Soundtrack_Deluxe_(Deluxe_Edition)/track/Look_Down_(Deluxe_Version)/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Anna_Veleva/album/Famous_Soprano_Opera_Arias/track/Lucia_Lammermoor%2C_Act_1_-_%22Regnava_nel_silenzio%22/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Anna_Veleva/album/Famous_Soprano_Opera_Arias/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Barros_De_Alencar/album/Grandes_Sucessos_-_Barros_De_Alencar/track/N%C3%A3o_V%C3%A1_Embora_(Tu_Me_Plais_Et_Je_T%27Aime)/"));
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Nouman_Khalid%2CBilal_Saeed/album/Jugni_(feat._Bilal_Saeed)/track/Jugni_(feat._Bilal_Saeed)/"));

            httpFile.setNewURL(new URL("http://www.rdio.com/artist/Luiz_Melodia/album/Bis_-_Luiz_Melodia/track/Ser_Bo%C3%AAmio/"));  //No permissions to stream
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Luiz_Melodia/album/Esta%C3%A7%C3%A3o_Melodia/track/Rei_do_Samba/")); //No permissions to stream
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Luiz_Melodia/album/Dois_Ases_4/track/Malandrando/")); //No permissions to stream
            //httpFile.setNewURL(new URL("http://www.rdio.com/artist/Luiz_Melodia/album/Luiz_Melodia_Especial_MTV/track/Eu_Agora_Sou_Feliz/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final RdioServiceImpl service = new RdioServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}