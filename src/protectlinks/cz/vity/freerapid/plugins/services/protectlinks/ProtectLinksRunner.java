package cz.vity.freerapid.plugins.services.protectlinks;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RickCL
 */
public class ProtectLinksRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ProtectLinksRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.protectlinks.com";

	@Override
	public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);
        logger.info("Starting run task " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        logger.info(fileURL);

        if (makeRequest(getMethod)) {
            // <iframe name="pagetext" height="100%" frameborder="no" width="100%" src="http://rapidshare.com/files/340304382/Natali_Klaudia_Sascha_and_Beatrice_Table_Tonguers.part1.rar"></iframe>
            String redirect = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("pagetext").getAction();

            this.httpFile.setNewURL(new URL(redirect));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
        	throw new ServiceConnectionProblemException();
        }
	}

    /**
     * Convert a http://www.protectlinks.com/268140 to http://www.protectlinks.com/redirect.php?id=268140
     * @param URL
     * @return http://www.protectlinks.com/redirect.php?id=?????????
     */
    private String checkURL(String URL) {
    	Matcher matcher = Pattern.compile("http://(?:.[^\\d])*[/|=](\\d*)").matcher(URL);
    	if( matcher.find() ) {
    		return SERVICE_WEB + "/redirect.php?id=" + matcher.group(1);
    	}
    	return URL;
    }

}
