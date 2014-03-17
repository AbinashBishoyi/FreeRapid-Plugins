package cz.vity.freerapid.plugins.services.titulky;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class TitulkyServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "titulky.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TitulkyFileRunner();
    }

}