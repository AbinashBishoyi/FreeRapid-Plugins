package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class IskladkaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "iskladka.cz";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?iskladka\\.(cz|sk)/.*", Pattern.CASE_INSENSITIVE);

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
        new IskladkaRunner().run(downloader);
    }

}