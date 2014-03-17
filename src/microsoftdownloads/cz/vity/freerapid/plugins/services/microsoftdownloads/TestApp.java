package cz.vity.freerapid.plugins.services.microsoftdownloads;

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
            //we set file URL - many different types
            httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?familyid=A4DD31D5-F907-4406-9012-A5C3199EA2B3&displaylang=en"));//regular1 english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?displaylang=ja&FamilyID=a4dd31d5-f907-4406-9012-a5c3199ea2b3"));//regular1 non-english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?FamilyID=2da43d38-db71-4c1b-bc6a-9b6652cd92a3&displayLang=en"));//regular2 english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?FamilyID=2da43d38-db71-4c1b-bc6a-9b6652cd92a3&displayLang=cs"));//regular2 non-english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?FamilyID=b444bf18-79ea-46c6-8a81-9db49b4ab6e5&displaylang=en"));//regular3 english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?FamilyID=b444bf18-79ea-46c6-8a81-9db49b4ab6e5&displaylang=fi"));//regular3 non-english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?displaylang=en&FamilyID=28c97d22-6eb8-4a09-a7f7-f6c7a1f000b5"));//multiple english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?displaylang=de&FamilyID=28c97d22-6eb8-4a09-a7f7-f6c7a1f000b5"));//multiple non-english
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/info.aspx?na=46&p=1&SrcDisplayLang=en&SrcCategoryId=&SrcFamilyId=28c97d22-6eb8-4a09-a7f7-f6c7a1f000b5&u=http%3a%2f%2fdownload.microsoft.com%2fdownload%2f3%2f0%2fe%2f30e87f07-d6b3-4ab3-a93d-a17814ed8b4b%2f32+BIT%2fsetup.exe"));//direct
            //httpFile.setNewURL(new URL("http://www.microsoft.com/downloads/details.aspx?displaylang=en&FamilyID=4758433b-11dd-49fc-9529-f8d7a914e1bf"));//needs WGA validation

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MicrosoftDownloadsServiceImpl service = new MicrosoftDownloadsServiceImpl(); //instance of service - of our plugin
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