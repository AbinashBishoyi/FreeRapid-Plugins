package cz.vity.freerapid.plugins.services.imgserve;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ImgServeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imgserve.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImgServeFileRunner();
    }

}