package cz.vity.freerapid.plugins.services.filesmonster;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Lukiz
 */
public class FilesMonsterServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "filesmonster.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesMonsterFileRunner();
    }

}
