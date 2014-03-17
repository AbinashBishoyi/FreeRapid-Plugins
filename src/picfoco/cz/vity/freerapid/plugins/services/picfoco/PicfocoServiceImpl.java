package cz.vity.freerapid.plugins.services.picfoco;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author tong2shot
 */
public class PicfocoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "picfoco.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PicfocoFileRunner();
    }

}