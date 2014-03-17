package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
//public abstract class XFileSharingCommonServiceImpl extends AbstractFileShareService {
public class XFileSharingCommonServiceImpl extends AbstractFileShareService {
    private volatile PremiumAccount config;

    //private void checkPrerequisites() throws PluginImplementationException {
    //check prerequisites
    protected void checkPrerequisites() throws PluginImplementationException {
        if (getPluginConfigFile() == null)
            throw new PluginImplementationException("getPluginConfigFile return value cannot be null");
        if (getPluginServiceTitle() == null)
            throw new PluginImplementationException("getPluginServiceTitle return value cannot be null");
        if (getImplClass() == null) throw new PluginImplementationException("getImplClass return value cannot be null");
    }

    public XFileSharingCommonServiceImpl() {
        super();
    }

    @Override
    public String getName() {
        return "xfilesharingcommon.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    /*
    @Override
    public abstract PluginRunner getPluginRunnerInstance();

    protected abstract String getPluginConfigFile();

    protected abstract String getPluginServiceTitle();

    protected abstract Class getImplClass();
    */

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new XFileSharingCommonFileRunner();
    }

    //filename where plugin config file (user account) will be stored
    //return value should not be null, if registered users is supported
    //return value ex : "plugin_RyuShare.xml"
    protected String getPluginConfigFile() {
        return null;
    }

    //used in account dialog title bar
    //return value should not be null, if registered users is supported
    //return value ex : "RyuShare"
    protected String getPluginServiceTitle() {
        return null;
    }

    //used for synch
    //return value should not be null, if registered users is supported
    //return value ex : RyuShareServiceImpl.class;
    protected Class getImplClass() {
        return null;
    }

    @Override
    public void showOptions() throws Exception {
        checkPrerequisites();
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        checkPrerequisites();
        return showAccountDialog(getConfig(), getPluginServiceTitle(), getPluginConfigFile());
    }

    public PremiumAccount getConfig() throws Exception {
        checkPrerequisites();
        if (config == null) {
            synchronized (getImplClass()) {
                config = getAccountConfigFromFile(getPluginConfigFile());
            }
        }
        return config;
    }

    public void setConfig(final PremiumAccount config) throws PluginImplementationException {
        checkPrerequisites();
        this.config = config;
    }

}