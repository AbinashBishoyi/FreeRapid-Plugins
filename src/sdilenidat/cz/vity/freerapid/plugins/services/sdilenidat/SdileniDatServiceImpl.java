package cz.vity.freerapid.plugins.services.sdilenidat;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SdileniDatServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sdilenidat.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SdileniDatFileRunner();
    }

}