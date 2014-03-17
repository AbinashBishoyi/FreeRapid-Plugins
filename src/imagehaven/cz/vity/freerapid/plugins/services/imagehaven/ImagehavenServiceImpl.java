package cz.vity.freerapid.plugins.services.imagehaven;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class ImagehavenServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "imagehaven.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImagehavenFileRunner();
    }

}
