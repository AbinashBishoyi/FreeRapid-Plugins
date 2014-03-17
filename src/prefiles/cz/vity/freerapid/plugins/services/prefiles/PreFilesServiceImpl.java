package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author CrazyCoder
 */
public class PreFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getName() {
        return "prefiles.com";
    }

    @Override
    public String getServiceTitle() {
        return "PreFiles";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PreFilesFileRunner();
    }

}