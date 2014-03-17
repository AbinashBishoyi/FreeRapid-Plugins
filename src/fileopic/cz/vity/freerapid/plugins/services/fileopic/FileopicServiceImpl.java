package cz.vity.freerapid.plugins.services.fileopic;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileopicServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Fileopic";
    }

    @Override
    public String getName() {
        return "fileopic.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileopicFileRunner();
    }

}