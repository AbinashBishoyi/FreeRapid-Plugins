package cz.vity.freerapid.plugins.services.gboxes;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GBoxesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "GBoxes";
    }

    @Override
    public String getName() {
        return "gboxes.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GBoxesFileRunner();
    }

}