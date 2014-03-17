package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class HuluServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "hulu.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HuluFileRunner();
    }

}