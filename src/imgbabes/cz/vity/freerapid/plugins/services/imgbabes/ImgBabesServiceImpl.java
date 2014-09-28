package cz.vity.freerapid.plugins.services.imgbabes;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ImgBabesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imgbabes.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImgBabesFileRunner();
    }

}