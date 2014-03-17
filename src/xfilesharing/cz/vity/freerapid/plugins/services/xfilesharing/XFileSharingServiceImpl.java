package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 * @author ntoskrnl
 */
public abstract class XFileSharingServiceImpl extends AbstractFileShareService {

    private final String configFile = "plugin_" + getServiceTitle() + ".xml";
    private PremiumAccount config;

    public abstract String getServiceTitle();

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public void showOptions() throws Exception {
        final PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            setConfig(pa);
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), getServiceTitle(), configFile);
    }

    public PremiumAccount getConfig() throws Exception {
        synchronized (getClass()) {
            if (config == null) {
                config = getAccountConfigFromFile(configFile);
            }
            return config;
        }
    }

    public void setConfig(final PremiumAccount config) {
        synchronized (getClass()) {
            this.config = config;
        }
    }

}