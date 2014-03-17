package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;

/**
 * @author tong2shot
 */
public class AccountServiceImpl extends XFileSharingCommonServiceImpl implements AccountService {
    private volatile PremiumAccount config;

    //filename where plugin config file (user account) will be stored
    //ex : "plugin_RyuShare.xml"
    protected final String configFile;

    //used in account dialog title bar
    //ex : "RyuShare"
    protected final String serviceTitle;

    //used for synch
    //ex : RyuShareFilesServiceImpl.class;
    protected final Class implClass;

    public AccountServiceImpl(String configFile, String serviceTitle, Class implClass) {
        super();
        this.configFile = configFile;
        this.serviceTitle = serviceTitle;
        this.implClass = implClass;
    }

    //check prerequisites
    protected void checkPrerequisites() throws PluginImplementationException {
        if (configFile == null)
            throw new PluginImplementationException("getPluginConfigFile return value cannot be null");
        if (serviceTitle == null)
            throw new PluginImplementationException("getPluginServiceTitle return value cannot be null");
        if (implClass == null) throw new PluginImplementationException("getImplClass return value cannot be null");
    }

    @Override
    public void showOptions() throws Exception {
        checkPrerequisites();
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        checkPrerequisites();
        return showAccountDialog(getConfig(), serviceTitle, configFile);
    }

    public PremiumAccount getConfig() throws Exception {
        checkPrerequisites();
        if (config == null) {
            synchronized (implClass) {
                config = getAccountConfigFromFile(configFile);
            }
        }
        return config;
    }

    public void setConfig(final PremiumAccount config) throws PluginImplementationException {
        checkPrerequisites();
        this.config = config;
    }
}
