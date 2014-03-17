package cz.vity.freerapid.plugins.services.imagebam;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Arthur Gunawan
 */
public class ImagebamServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imagebam.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImagebamFileRunner();
    }

}
