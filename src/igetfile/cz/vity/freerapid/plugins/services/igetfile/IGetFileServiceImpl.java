package cz.vity.freerapid.plugins.services.igetfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class IGetFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "IGetFile";
    }

    @Override
    public String getName() {
        return "igetfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IGetFileFileRunner();
    }

}