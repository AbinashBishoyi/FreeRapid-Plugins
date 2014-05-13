package cz.vity.freerapid.plugins.services.yunfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Stan
 */
public class YunFileServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "yunfile.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YunFileFileRunner();
    }

}