package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;

/**
 * @author tong2shot
 */
public interface AccountService {
    public PremiumAccount showConfigDialog() throws Exception;

    public PremiumAccount getConfig() throws Exception;

    public void setConfig(final PremiumAccount config) throws PluginImplementationException;
}
