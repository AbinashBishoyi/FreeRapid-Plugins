package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.ulozto.captcha.SoundReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS (captcha)
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());
    private int captchaCount = 0;

    public UlozToRunner() {
        super();
    }

    private void ageCheck(String content) throws Exception{
       if(content.contains("confirmContent")){ //eroticky obsah vyzaduje potvruemo
               String confirmUrl=checkURL(fileURL)+"?do=askAgeForm-submit";               
               PostMethod confirmMethod = (PostMethod) getMethodBuilder()
                       .setAction(confirmUrl)
                       .setEncodePathAndQuery(true)
                       .setAndEncodeParameter("agree", "Souhlas\u00edm")
                       .toPostMethod();
               makeRedirectedRequest(confirmMethod);
               if(getContentAsString().contains("confirmContent")){
                  throw new PluginImplementationException("Cannot confirm age");
               }
       }
    }
    
    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            ageCheck(getContentAsString());
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(cz.vity.freerapid.plugins.webclient.DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        //Cookie: uloz-to-id=356097727; __utma=140660086.1203105506.1328293691.1329637591.1329640156.8; __utmz=140660086.1328293691.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utmc=140660086; PHPSESSID=vs3jl2js8r1saohhobu8uinht4; nette-browser=xiaupyelif; __utmb=140660086.3.9.1329640178664

        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) { 
            checkProblems();
            ageCheck(getContentAsString());
            checkNameAndSize(getContentAsString());
            addCookie(new Cookie(".uloz.to", "__utma", "140660086.124876764.1329640156.1329640156.1329640156.1", "/", 86400, false));
            addCookie(new Cookie(".uloz.to", "__utmb", "140660086.3.9.1329640592039", "/", 86400, false));
            addCookie(new Cookie(".uloz.to", "__utmc", "140660086", "/", 86400, false));
            addCookie(new Cookie(".uloz.to", "__utmz", "140660086.1328293691.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)", "/", 86400, false));
        
            if (getContentAsString().contains("captchaContainer")) {                
                boolean saved = false;
                captchaCount = 0;
                loopcaptcha:while (getContentAsString().contains("captchaContainer") || getContentAsString().contains("?captcha=no")) {
                    //client.getHTTPClient().getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 8);
                    HttpMethod method = stepCaptcha();
                    method.setFollowRedirects(false);
                    downloadTask.sleep(new Random().nextInt(4) + new Random().nextInt(3));
                    httpFile.setState(DownloadState.GETTING);                    
                    /*
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile, false);
                    if (inputStream != null) {
                        downloadTask.saveToFile(inputStream);
                        return;
                    }*/
                    makeRequest(method);
                    while ((method.getStatusCode() == 302)||(method.getStatusCode() == 303)) {
                        String nextUrl = method.getResponseHeader("Location").getValue();
                        method = getMethodBuilder().setReferer(checkURL(fileURL)).setAction(nextUrl).toHttpMethod();                        
                        method.setFollowRedirects(false);
                        //downloadTask.sleep(new Random().nextInt(15) + new Random().nextInt(3));
                        if (saved = tryDownloadAndSaveFile(method)) break loopcaptcha;                        
                    }
                    checkProblems();
                }
                if (!saved) {
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }                
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
        {
           checkProblems();
           throw new PluginImplementationException();
        }
    }

    private String checkURL(String fileURL) {
        return fileURL.replaceFirst("(ulozto\\.net|ulozto\\.cz|ulozto\\.sk)", "uloz.to");
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("uloz.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        
        PlugUtils.checkName(httpFile, content, "class=\"jsShowDownload\">", "</a>");

        
        String size="";
        try{
            size=PlugUtils.getStringBetween(content, "<span id=\"fileSize\">", "</span>");
            if(size.contains("|")){
               size=size.substring(size.indexOf("|")+1).trim();
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        }catch(PluginImplementationException ex){
           //u online videi neni velikost
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
    
    private HttpMethod stepCaptcha() throws Exception {
        if (getContentAsString().contains("Please click here to continue")) {
            logger.info("Using HTML redirect");
            return getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Please click here to continue").toGetMethod();
        }
        CaptchaSupport captchaSupport = getCaptchaSupport();
        MethodBuilder captchaMethod = getMethodBuilder().setActionFromImgSrcWhereTagContains("class=\"captcha\"");
        /* //vygenerovani rucne
        Random rnd=new Random();        
        int a = rnd.nextInt(50000)+1;
        MethodBuilder captchaMethod = getMethodBuilder().setAction("http://img.uloz.to/captcha/"+ a +".png");
        */
        String captcha = "";
        /* //precteni
           if (captchaCount++ < 6) {
            logger.warning("captcha url:" + captchaMethod.getAction());
            Matcher m = Pattern.compile("uloz\\.to/captcha/([0-9]+)\\.png").matcher(captchaMethod.getAction());
            if (m.find()) {
                String number = m.group(1);
                SoundReader captchaReader = new SoundReader();          
                HttpMethod methodSound = getMethodBuilder().setAction("http://img.uloz.to/captcha/sound/" + number + ".mp3").toGetMethod();
                captcha = captchaReader.parse(client.makeRequestForFile(methodSound));
                methodSound.releaseConnection();
            }
        } else {*/
            captcha = captchaSupport.getCaptcha(captchaMethod.getAction());
        //}
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            MethodBuilder sendForm = getMethodBuilder().setReferer(checkURL(fileURL)).setActionFromFormWhereActionContains("do=downloadDialog-freeDownloadForm-submit", true);
            sendForm.setParameter("freeDownload", "St"+((char) 0xC3) + ((char)0xA1) + "hnout");
            sendForm.setAndEncodeParameter("captcha[text]", captcha);
            return sendForm.toPostMethod();
        }
   }

    //"Prekro�en pocet FREE slotu, pouzijte VIP download

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("Soubor byl sma")) {
            throw new URLNotAvailableAnymoreException("Soubor byl smazan");
        }
        if (content.contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        if (content.contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException("Muzete stahovat pouze jeden soubor naraz");

        }
        if (content.contains("et FREE slot") && content.contains("ijte VIP download")) {
            logger.warning(getContentAsString());
            throw new YouHaveToWaitException("Nejsou dostupne FREE sloty", 40);
        }


    }

}
