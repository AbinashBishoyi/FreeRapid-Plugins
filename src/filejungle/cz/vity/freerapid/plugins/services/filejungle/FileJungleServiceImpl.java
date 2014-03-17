package cz.vity.freerapid.plugins.services.filejungle;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author CrazyCoder
 */
public class FileJungleServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filejungle.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileJungleFileRunner();
    }

}
