package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Arthur Gunawan
 */
public class TurboBitServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "turbobit.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value, in plugin.xml don't forget to update this value too
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TurboBitFileRunner();
    }

}