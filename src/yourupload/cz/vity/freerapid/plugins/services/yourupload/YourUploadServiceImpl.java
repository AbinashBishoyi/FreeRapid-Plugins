package cz.vity.freerapid.plugins.services.yourupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class YourUploadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "yourupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YourUploadFileRunner();
    }

}