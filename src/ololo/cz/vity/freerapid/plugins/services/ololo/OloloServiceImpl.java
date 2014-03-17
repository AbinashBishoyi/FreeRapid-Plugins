package cz.vity.freerapid.plugins.services.ololo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OloloServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ololo.fm";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OloloFileRunner();
    }

}