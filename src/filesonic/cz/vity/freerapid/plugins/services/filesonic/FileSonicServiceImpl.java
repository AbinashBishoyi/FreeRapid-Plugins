package cz.vity.freerapid.plugins.services.filesonic;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class FileSonicServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filesonic.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSonicFileRunner();
    }

}