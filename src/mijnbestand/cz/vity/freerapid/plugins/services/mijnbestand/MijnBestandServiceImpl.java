package cz.vity.freerapid.plugins.services.mijnbestand;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MijnBestandServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "mijnbestand.nl";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MijnBestandFileRunner();
    }

}