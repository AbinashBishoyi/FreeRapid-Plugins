package cz.vity.freerapid.plugins.services.shareflare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.io.IOException;
import java.net.URL;

/**
 * @author RickCL
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://shareflare.net/download/0802.0809136b26e6fa1127b0335c48/61.PopCap.Games.Collection___WwW.DeGracaeMaisGostoso.org.bY.Vamp.rar.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final ShareflareServiceImpl service = new ShareflareServiceImpl(); //instance of service - of our plugin
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
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
//        InputStream in = TestApp.class.getResourceAsStream("/resources/shareflare-captcha.bin");
//        //InputStream in = new ByteArrayInputStream(letters);
//        GZIPInputStream gzis = new GZIPInputStream(in);
//        ObjectInputStream ois = new ObjectInputStream(gzis);
//        final TreeMap<String, Matrix> map = (TreeMap<String, Matrix>) ois.readObject();
//        final TreeMap out = new TreeMap<String, cz.vity.freerapid.plugins.services.letitbit.captcha.Matrix>();
//        for (Map.Entry<String, Matrix> entry : map.entrySet()) {
//            final cz.vity.freerapid.plugins.services.letitbit.captcha.Matrix m = new cz.vity.freerapid.plugins.services.letitbit.captcha.Matrix();
//            m.width = entry.getValue().width;
//            m.height = entry.getValue().height;
//            m.value = entry.getValue().value;
//            out.put(entry.getKey(), m);
//        }
//        final ObjectOutputStream out2 = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File("c:\\out.bin"))));
//        out2.writeObject(out);
//        out2.close();
//        System.out.println("map = " + map);
    }
}