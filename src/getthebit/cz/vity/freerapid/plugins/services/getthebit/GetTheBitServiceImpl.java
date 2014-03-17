package cz.vity.freerapid.plugins.services.getthebit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class GetTheBitServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "getthebit.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 4;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GetTheBitFileRunner();
    }

}