package cz.vity.freerapid.plugins.services.qshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author RickCL
 */
public class QShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "qshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QShareFileRunner();
    }

}
