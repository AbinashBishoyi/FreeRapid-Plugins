package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class UlozToServiceImpl extends AbstractFileShareService {

    static {
        if (!"yes".equals(System.getProperty("cz.vity.freerapid.updatechecked", null))) {
            try {
                System.setProperty("cz.vity.freerapid.updatechecked", "yes");
                @SuppressWarnings("unchecked")
                final Class<org.jdesktop.application.Application> mainApp =
                        (Class<org.jdesktop.application.Application>) Class.forName("cz.vity.freerapid.core.MainApp");
                try {
                    //make sure that version < 0.85u1
                    Class.forName("cz.vity.freerapid.plugins.webclient.utils.ScriptUtils");
                } catch (ClassNotFoundException e) {
                    {
                        final java.io.File file = new java.io.File(cz.vity.freerapid.utilities.Utils.getAppPath(), "startup.properties");
                        String s = cz.vity.freerapid.utilities.Utils.loadFile(file, "UTF-8");
                        s = s.replaceAll("(?m)^\\-\\-debug$", "#--debug");
                        if (file.delete()) {
                            final java.io.OutputStream os = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file));
                            os.write(s.getBytes("UTF-8"));
                            os.close();
                        }
                    }
                    {
                        final java.lang.reflect.Method startCheckNewVersion = mainApp.getDeclaredMethod("startCheckNewVersion");
                        startCheckNewVersion.setAccessible(true);
                        startCheckNewVersion.invoke(org.jdesktop.application.Application.getInstance(mainApp));
                    }
                }
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    private static final String SERVICE_NAME = "uloz.to";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UlozToRunner();
    }


}