package cz.vity.freerapid.plugins.services.linkbee;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Javi
 */
public class LinkBeeServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "linkbee.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkBeeFileRunner();
    }

}