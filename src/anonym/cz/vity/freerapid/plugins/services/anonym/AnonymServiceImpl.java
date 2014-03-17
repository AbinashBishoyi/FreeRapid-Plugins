package cz.vity.freerapid.plugins.services.anonym;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author benpicco
 */
public class AnonymServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "anonym.to";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//Check not supported
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AnonymFileRunner();
    }

}
