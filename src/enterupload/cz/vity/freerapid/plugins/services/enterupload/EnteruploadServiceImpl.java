package cz.vity.freerapid.plugins.services.enterupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek
 */
public class EnteruploadServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "enterupload.com";
    //private ServicePluginContext context = new ServicePluginContext();

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }
    /*
    public void run() throws Exception {
        super.run();
        new EnteruploadRunner().run(downloadTask, context);
    }
    */

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EnteruploadRunner();
    }
}