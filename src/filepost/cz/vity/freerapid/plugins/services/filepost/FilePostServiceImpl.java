package cz.vity.freerapid.plugins.services.filepost;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author CrazyCoder
 */
public class FilePostServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filepost.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilePostFileRunner();
    }
}
