package cz.vity.freerapid.plugins.services.vip_file;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class VipFileServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "vip-file.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VipFileFileRunner();
    }

}
