package cz.vity.freerapid.plugins.services.eyesfile;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Class which contains main code
 *
 * @author bircihe
 */
class EyesFileFileRunner extends XFileSharingRunner {

    @Override
    protected void setLanguageCookie() throws Exception {
        do {
            final HttpMethod method = getGetMethod(fileURL);
            if (!makeRequest(method)) {
                if (getContentAsString().contains("The document has moved"))
                    fileURL = PlugUtils.getStringBetween(getContentAsString(), "The document has moved <a href=\"", "\">");
                else
                    throw new ServiceConnectionProblemException();
            }
        } while (getContentAsString().contains("The document has moved"));
        super.setLanguageCookie();
    }

}