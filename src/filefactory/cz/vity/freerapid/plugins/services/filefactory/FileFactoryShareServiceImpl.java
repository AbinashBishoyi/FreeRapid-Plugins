package cz.vity.freerapid.plugins.services.filefactory;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import javax.swing.*;

/**
 * @author Vity
 */
public class FileFactoryShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filefactory.com";


    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public Icon getFaviconImage() {
        return null;
    }

    public Icon getSmallImage() {
        return null;
    }

    public Icon getBigImage() {
        return null;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new FileFactoryRunner().run(downloader);
    }

}
