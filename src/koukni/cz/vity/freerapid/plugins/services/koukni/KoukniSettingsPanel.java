package cz.vity.freerapid.plugins.services.koukni;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author birchie
 */
class KoukniSettingsPanel extends JPanel {
    private KoukniSettingsConfig config;

    public KoukniSettingsPanel(KoukniServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private static final String[] qualityStrings = {"360p", "720p", "1080p"};

    public String getQualityString(final int quality) {
        return qualityStrings[quality];
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred video quality:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        qualityLabel.setLabelFor(qualityList);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedIndex(config.getVideoQuality());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality(qualityList.getSelectedIndex());
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
