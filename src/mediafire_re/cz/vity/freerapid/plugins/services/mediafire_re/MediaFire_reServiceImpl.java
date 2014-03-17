package cz.vity.freerapid.plugins.services.mediafire_re;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MediaFire_reServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MediaFire.re";
    }

    @Override
    public String getName() {
        return "mediafire.re";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MediaFire_reFileRunner();
    }

}