package cz.vity.freerapid.plugins.services.edisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class EdiskRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EdiskRunner.class.getName());

    public EdiskRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("te text z obr")) {
                checkNameAndSize(getContentAsString());
                //    while (getContentAsString().contains("te text z obr")) {
                PostMethod method = hackCaptcha(getContentAsString());
                //    method.setFollowRedirects(true);
                makeRequest(method);

                //   }
                String finalURL = getContentAsString();
                GetMethod finalMethod = getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String sicherName(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)_[0-9.]+\\.html", s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "file01";
    }

    private String checkURL(String fileURL) {
        return fileURL.replaceFirst("edisk.sk", "edisk.cz");

    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("edisk.cz")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("neexistuje z ")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }

        Matcher matcher = PlugUtils.matcher(": ([^ ]+) \\(([0-9.]+ .B)\\)</h2>", content);
        // odebiram jmeno
        String fn;
        if (matcher.find()) {
            fn = matcher.group(1);
            Long a = PlugUtils.getFileSizeFromString(matcher.group(2));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else fn = sicherName(fileURL);
        logger.info("File name " + fn);
        httpFile.setFileName(fn);
        // konec odebirani jmena


    }

    /*
  private PostMethod stepCaptcha(String contentAsString) throws Exception {
      if (contentAsString.contains("te text z obr")) {
          CaptchaSupport captchaSupport = getCaptchaSupport();
          Matcher matcher = PlugUtils.matcher("img src=\"([^\"]*captcha[^\"]*)\"", contentAsString);
          if (matcher.find()) {
              String host = "http://" + httpFile.getFileUrl().getHost();
              String s = host + matcher.group(1);

              logger.info("Captcha URL " + s);
              String captcha = captchaSupport.getCaptcha(s);
              if (captcha == null) {
                  throw new CaptchaEntryInputMismatchException();
              } else {

                  matcher = PlugUtils.matcher("form method=\"post\" action=\"([^\"]*)\"", contentAsString);
                  if (!matcher.find()) {
                      logger.info(getContentAsString());
                      throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                  }
                  String postTargetURL;
                  postTargetURL = matcher.group(1);
                  postTargetURL = postTargetURL.replace("stahnout-soubor", "x-download");
                  logger.info("Captcha target URL " + postTargetURL);
                  client.setReferer(fileURL);
                  final PostMethod postMethod = getPostMethod(postTargetURL);
                  postMethod.addParameter("captchaCode", captcha);
                  postMethod.addParameter("type", "member");
                  return postMethod;

              }
          } else {
              logger.warning(contentAsString);
              throw new PluginImplementationException("Captcha picture was not found");
          }
      }
      return null;
  }
    */
    private PostMethod hackCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("te text z obr")) {
            downloadTask.sleep(5);
            Matcher matcher = PlugUtils.matcher("form method=\"post\" action=\"([^\"]*)\"", contentAsString);
            if (!matcher.find()) {
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
            String postTargetURL;
            postTargetURL = matcher.group(1);
            String type = "";
            if (postTargetURL.contains("stahnout-soubor")) {
                postTargetURL = postTargetURL.replace("stahnout-soubor", "x-download");
                type = "member";
            }
            if (postTargetURL.contains("stahni")) {
                postTargetURL = postTargetURL.replace("stahni", "x-download");
                type = "quick";
            }

            logger.info("Captcha target URL " + postTargetURL);
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(postTargetURL);
            postMethod.addParameter("captchaCode", "5415");
            postMethod.addParameter("type", type);
            return postMethod;


        }
        return null;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("neexistuje z ")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }
        if (getContentAsString().contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException(String.format("<b>Mùžete stahovat pouze jeden soubor naráz</b><br>"));

        }


    }

}