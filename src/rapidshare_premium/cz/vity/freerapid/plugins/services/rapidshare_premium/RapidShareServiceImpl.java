package cz.vity.freerapid.plugins.services.rapidshare_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek & Tomáš Procházka <to.m.p@atomsoft.cz>
 */
public class RapidShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "RapidShare.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return Integer.MAX_VALUE;
    }

    protected PluginRunner getPluginRunnerInstance() {
        return new RapidShareRunner();
    }

}
