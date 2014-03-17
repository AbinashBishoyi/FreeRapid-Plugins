package cz.vity.freerapid.plugins.services.quickshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class QuickshareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "quickshare.cz";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?quickshare\\.cz/.*", Pattern.CASE_INSENSITIVE);

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
        new QuickshareRunner().run(downloader);
    }

}