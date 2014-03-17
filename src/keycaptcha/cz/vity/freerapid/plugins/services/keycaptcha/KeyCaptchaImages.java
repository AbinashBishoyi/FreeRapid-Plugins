package cz.vity.freerapid.plugins.services.keycaptcha;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author ntoskrnl
 */
class KeyCaptchaImages {

    private BufferedImage background;
    private BufferedImage sample;
    private List<BufferedImage> pieces;

    public BufferedImage getBackground() {
        return background;
    }

    public void setBackground(final BufferedImage background) {
        this.background = background;
    }

    public BufferedImage getSample() {
        return sample;
    }

    public void setSample(final BufferedImage sample) {
        this.sample = sample;
    }

    public List<BufferedImage> getPieces() {
        return pieces;
    }

    public void setPieces(final List<BufferedImage> pieces) {
        this.pieces = pieces;
    }

}
