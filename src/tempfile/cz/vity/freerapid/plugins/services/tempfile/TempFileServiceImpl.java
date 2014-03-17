package cz.vity.freerapid.plugins.services.tempfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TempFileServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tempfile.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TempFileFileRunner();
    }

}