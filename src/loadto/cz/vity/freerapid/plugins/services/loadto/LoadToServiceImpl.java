package cz.vity.freerapid.plugins.services.loadto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class LoadToServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "load.to";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?load\\.to/.*", Pattern.CASE_INSENSITIVE);

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public boolean supportsURL(String url) {
        return pattern.matcher(url).matches();
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new LoadToRunner().run(downloader);
    }

}