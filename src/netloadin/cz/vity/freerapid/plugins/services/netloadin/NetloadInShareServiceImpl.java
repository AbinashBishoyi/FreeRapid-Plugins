package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class NetloadInShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "netload.in";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?netload\\.in/.*", Pattern.CASE_INSENSITIVE);

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }

    public boolean supportsURL(String url) {
        return pattern.matcher(url).matches();
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new NetloadInRunner().run(downloader);
    }

}