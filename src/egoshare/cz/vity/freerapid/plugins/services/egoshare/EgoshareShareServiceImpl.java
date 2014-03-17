package cz.vity.freerapid.plugins.services.egoshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek
 */
public class EgoshareShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "egoshare.com";
    private ServicePluginContext context = new ServicePluginContext();

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }
    /*
    public void run() throws Exception {
        super.run();
        new EgoshareRunner().run(downloader, context);
    }
    */

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EgoshareRunner(context);
    }
}