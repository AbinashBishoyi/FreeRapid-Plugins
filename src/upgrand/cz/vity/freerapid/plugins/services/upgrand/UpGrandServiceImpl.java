package cz.vity.freerapid.plugins.services.upgrand;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpGrandServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UpGrand";
    }

    @Override
    public String getName() {
        return "upgrand.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpGrandFileRunner();
    }

}