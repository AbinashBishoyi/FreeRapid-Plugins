package cz.vity.freerapid.plugins.services.itv;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class ItvServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "itv.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ItvFileRunner();
    }

}