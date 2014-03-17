package cz.vity.freerapid.plugins.services.ziddu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek
 */
public class ZidduShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "ziddu.com";
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
        new ZidduRunner().run(downloadTask, context);
    }
    */

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZidduRunner();
    }
}