package cz.vity.freerapid.plugins.dev;

import cz.vity.freerapid.plugins.dev.plugimpl.DevDialogSupport;
import cz.vity.freerapid.plugins.dev.plugimpl.DevPluginContextImpl;
import cz.vity.freerapid.plugins.dev.plugimpl.DevStorageSupport;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginContext;
import cz.vity.freerapid.plugins.webclient.interfaces.ShareDownloadService;
import cz.vity.freerapid.plugins.webclient.ssl.EasySSLProtocolSocketFactory;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.jdesktop.application.Application;

import java.net.ProxySelector;
import java.util.logging.Logger;

/**
 * Help application for testing plugins.
 *
 * @author Vity
 */
public abstract class PluginDevApplication extends Application {
    private final static Logger logger = Logger.getLogger(PluginDevApplication.class.getName());
    private boolean useTempFiles = false;

    /**
     * Returns new instance of HttpFileDownloadTask
     *
     * @param file     the file that should be downloaded
     * @param settings internet connection settings
     * @return instance of HttpFileDownloadTask for processing file
     */
    protected HttpFileDownloadTask getHttpFileDownloader(HttpFile file, ConnectionSettings settings) {
        final PluginDevDownloadTask devDownloadTask = new PluginDevDownloadTask(file, settings);
        devDownloadTask.setUseTempFiles(useTempFiles);
        return devDownloadTask;
    }


    public boolean isUseTempFiles() {
        return useTempFiles;
    }

    public void setUseTempFiles(boolean useTempFiles) {
        this.useTempFiles = useTempFiles;
    }

    @Override
    protected void initialize(String[] args) {
        super.initialize(args);
        Utils.getAppPath();
        ProxySelector.setDefault(null);
        try {
            trustAllCerts();
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
    }

    private void trustAllCerts() throws Exception {
        ProtocolSocketFactory sf = new EasySSLProtocolSocketFactory();
        Protocol p = new Protocol("https", sf, 443);
        Protocol.registerProtocol("https", p);
    }

    /**
     * Returns new instance of HttpFile for testing purposes
     *
     * @return instance  of HttpFile
     * @see cz.vity.freerapid.plugins.dev.PluginDevHttpFile
     */
    protected HttpFile getHttpFile() {
        return new PluginDevHttpFile();
    }

    /**
     * Runs plugin test - both runCheck (if it is supported by service) and run() on the service implementation
     *
     * @param service  service that is used for downloading
     * @param file     file that is being downloaded
     * @param settings internet connection settings
     * @throws Exception when anything went wrong
     */
    public void testRun(ShareDownloadService service, HttpFile file, ConnectionSettings settings) throws Exception {
        final PluginContext plugContext = getPluginContext();
        service.setPluginContext(plugContext);
        final HttpFileDownloadTask fileDownloader = getHttpFileDownloader(file, settings);
        try {
            if (service.supportsRunCheck()) {
                service.runCheck(fileDownloader);
                if (fileDownloader.getDownloadFile().getFileState() == FileState.NOT_CHECKED) {
                    throw new IllegalStateException("File state cannot be set to NOT_CHECKED after runCheck()");
                }
            }
            service.run(fileDownloader);
        } catch (ErrorDuringDownloadingException e) {
            if (fileDownloader != null)
                logger.warning(fileDownloader.getClient().getContentAsString());
            throw e;
        }
    }

    /**
     * Runs plugin runCheck test only (if it is supported by service, otherwise does nothing)
     *
     * @param service  service that is used for downloading
     * @param file     file that is being downloaded
     * @param settings internet connection settings
     * @throws Exception when anything went wrong
     */
    public void testRunCheck(ShareDownloadService service, HttpFile file, ConnectionSettings settings) throws Exception {
        final PluginContext plugContext = getPluginContext();
        if (service.supportsRunCheck()) {
            HttpFileDownloadTask fileDownloader = getHttpFileDownloader(file, settings);
            try {
                service.setPluginContext(plugContext);
                service.runCheck(fileDownloader);
                if (fileDownloader.getDownloadFile().getFileState() == FileState.NOT_CHECKED) {
                    throw new IllegalStateException("File state cannot be set to NOT_CHECKED after runCheck()");
                }
            } catch (ErrorDuringDownloadingException e) {
                logger.warning(fileDownloader.getClient().getContentAsString());
                throw e;
            }
        }
    }

    protected PluginContext getPluginContext() {
        return DevPluginContextImpl.create(new DevDialogSupport(this.getContext()), new DevStorageSupport(this.getContext()));
    }

}
