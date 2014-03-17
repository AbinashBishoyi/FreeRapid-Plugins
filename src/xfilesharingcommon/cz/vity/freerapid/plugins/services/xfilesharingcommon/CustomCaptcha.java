package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;

/**
 * @author tong2shot
 */
public interface CustomCaptcha {
    public HttpMethod stepCustomCaptcha(MethodBuilder methodBuilder) throws Exception;

    //public void setRegex(String regex);

    public String getCustomCaptchaRegex();
}
