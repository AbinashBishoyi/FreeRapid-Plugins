package cz.vity.freerapid.plugins.services.linksave;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
// import java.net.urlDECODER;
import java.net.URLDecoder;
import java.net.URL;
import java.lang.Character;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class LinksaveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinksaveFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);


        if (fileURL.length() < 42) {
            final String escapedURI = getMethodBuilder().setAction(PlugUtils.unescapeHtml(fileURL)).toHttpMethod().getURI().getEscapedURI();
            logger.info("New Link : " + escapedURI);     //Debug purpose, show the new found link
            this.httpFile.setNewURL(new URL(escapedURI));  //Set New URL for the link
            this.httpFile.setPluginID("linksavegroup.in");
            this.httpFile.setState(DownloadState.QUEUED);


        } else {


            GetMethod method = getGetMethod(fileURL); //create GET request
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
            String contentAsString = getContentAsString();//check for response


            if (contentAsString.contains("downloadbutton_highlight.png")) {
                String mLink = PlugUtils.getStringBetween(contentAsString, "<a href=\"", "\" onclick=\"javascript:document.getElementById");
                method = getGetMethod(PlugUtils.unescapeHtml(mLink));
                if (!makeRedirectedRequest(method)) { //we make the main request
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
                contentAsString = getContentAsString();//check for response

            }

            String mLink = PlugUtils.getStringBetween(contentAsString, "\"auto\" noresize src=\"", "\"");

            logger.info(mLink);
            method = getGetMethod(mLink);
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
            contentAsString = getContentAsString();//check for response
            logger.info(contentAsString);


            if (contentAsString.contains("<iframe src=\"&#")) {
                String unCode = PlugUtils.getStringBetween(contentAsString, "<iframe src=\"", "\"");

                logger.info("Code : " + PlugUtils.unescapeHtml(unCode));

                final String escapedURI = getMethodBuilder().setAction(PlugUtils.unescapeHtml(unCode)).toHttpMethod().getURI().getEscapedURI();
                logger.info("New Link : " + escapedURI);     //Debug purpose, show the new found link
                this.httpFile.setNewURL(new URL(escapedURI));  //Set New URL for the link
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);


                contentAsString = getContentAsString();//check for response

            }

            if (contentAsString.contains("llIIllIIlIIIIIllllllllllllllllllIIIIIl")) {
                String unCode = PlugUtils.getStringBetween(contentAsString, "IIIIIl(\"", "\"");
                String mCode = URLDecoder.decode(unCode, "UTF-8");
                String mEncrypted = PlugUtils.getStringBetween(mCode, "a('", "')");
                String mDecrypted = decrypt(mEncrypted);

                logger.info("Decodes : " + mCode);
                logger.info("Decrypt : " + mDecrypted);


                String encURL = "http://" + PlugUtils.getStringBetween(mDecrypted, "http://", "\"");
                encURL = encURL.replace("');", "");


                logger.info("Enc URL: " + encURL);


                method = getGetMethod(encURL);
                if (!makeRedirectedRequest(method)) { //we make the main request
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
                contentAsString = getContentAsString();//check for response

            }

            mLink = PlugUtils.getStringBetween(contentAsString, "iframe src=\"", "\"");
            final String escapedURI = getMethodBuilder().setAction(PlugUtils.unescapeHtml(mLink)).toHttpMethod().getURI().getEscapedURI();
            logger.info("New Link : " + escapedURI);     //Debug purpose, show the new found link
            this.httpFile.setNewURL(new URL(escapedURI));  //Set New URL for the link
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        }
    }


    private String decrypt(String m) {
        String c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String b = "";
        int i = 0;
        m = m.replaceAll("[^A-Za-z0-9\\+\\=]", "");

        do {
            int i1 = i;
            int i2 = i + 1;
            int i3 = i + 2;
            int i4 = i + 3;
            i = i + 4;
            int g = c.indexOf(m.charAt(i1));
            int h = c.indexOf(m.charAt(i2));
            int k = c.indexOf(m.charAt(i3));
            int l = c.indexOf(m.charAt(i4));
            int d = (g << 2) | (h >> 4);
            int e = ((h & 15) << 4) | (k >> 2);
            int f = ((k & 3) << 6) | l;
            char nCode = (char) d;
            b = b + nCode;
            if (k != 64) {

                nCode = (char) e;

                b = b + nCode;
            }
            if (l != 64) {

                nCode = (char) f;
                b = b + nCode;
            }
        } while (i < m.length());
        return b;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
