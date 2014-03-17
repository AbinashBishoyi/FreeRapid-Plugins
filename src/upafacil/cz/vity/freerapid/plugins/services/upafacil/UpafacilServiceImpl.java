package cz.vity.freerapid.plugins.services.upafacil;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpafacilServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "upafacil.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpafacilFileRunner();
    }

}