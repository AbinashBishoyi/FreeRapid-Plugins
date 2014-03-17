package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex,tong2shot
 */
public class MaknyosServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Maknyos";
    }

    public String getName() {
        return "maknyos.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MaknyosRunner();
    }
}
