package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Arthur Gunawan
 */
public class TurboBitServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "turbobit.net";
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