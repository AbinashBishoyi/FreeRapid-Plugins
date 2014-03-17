package cz.vity.freerapid.plugins.services.zhlednito;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class ZhledniToServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "zhlednito.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZhledniToFileRunner();
    }

}