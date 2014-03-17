package cz.vity.freerapid.plugins.services.multishare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class MultiShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "multishare.cz";
    }


    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MultiShareFileRunner();
    }

}