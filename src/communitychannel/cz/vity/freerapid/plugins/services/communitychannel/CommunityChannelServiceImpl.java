package cz.vity.freerapid.plugins.services.communitychannel;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class CommunityChannelServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "communitychannel.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CommunityChannelFileRunner();
    }

}