package cz.vity.freerapid.plugins.services.linestorage;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LineStorageServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "LineStorage";
    }

    @Override
    public String getName() {
        return "linestorage.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LineStorageFileRunner();
    }

}