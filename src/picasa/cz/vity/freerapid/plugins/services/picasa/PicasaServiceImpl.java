package cz.vity.freerapid.plugins.services.picasa;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class PicasaServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "picasaweb.google.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PicasaFileRunner();
    }

}
