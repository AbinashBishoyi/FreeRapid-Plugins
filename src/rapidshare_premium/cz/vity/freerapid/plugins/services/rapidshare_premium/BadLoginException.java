/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.vity.freerapid.plugins.services.rapidshare_premium;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;

/**
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 600 $ ($Date: 2008-09-25 18:29:25 +0530 (Thu, 25 Sep 2008) $)
 */
class BadLoginException extends ErrorDuringDownloadingException {

    /** Constructor */
    public BadLoginException() {
    }

    public BadLoginException(String message) {
        super(message);
    }
}

