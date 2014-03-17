package cz.vity.freerapid.plugins.services.bandcamp;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class BandCampServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bandcamp.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BandCampFileRunner();
    }

}