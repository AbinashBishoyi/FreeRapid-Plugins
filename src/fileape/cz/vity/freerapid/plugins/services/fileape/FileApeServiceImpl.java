package cz.vity.freerapid.plugins.services.fileape;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileApeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "fileape.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileApeFileRunner();
    }

}