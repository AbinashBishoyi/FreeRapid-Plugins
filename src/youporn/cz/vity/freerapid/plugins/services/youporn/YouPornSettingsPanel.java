package cz.vity.freerapid.plugins.services.youporn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author birchie
 */
class YouPornSettingsPanel extends JPanel {
    private YouPornSettingsConfig config;

    public YouPornSettingsPanel(YouPornServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private static final String[] qualityStrings = {"Small", "Medium", "Large"};
    private static final String[] qualDescStrings = {"MP4 - For iPhone/iPod", "MP4 - For Windows 7/8, Mac and iPad", "MPG - For Windows XP/Vista"};
    private static final String[] qualTypeStrings = {".mp4", ".mp4", ".mpg"};

    public String getQualityDescription(final int quality) {
        return qualDescStrings[quality];
    }

    public String getQualityType(final int quality) {
        return qualTypeStrings[quality];
    }

    private void initPanel() {

        final JLabel qualityLabel = new JLabel("Preferred video quality:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        final JLabel qualDescLabel = new JLabel("");
        qualityLabel.setLabelFor(qualityList);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedIndex(config.getVideoQuality());
        qualDescLabel.setText(qualDescStrings[config.getVideoQuality()]);

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                qualDescLabel.setText(getQualityDescription(qualityList.getSelectedIndex()));
                config.setVideoQuality(qualityList.getSelectedIndex());
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(new JLabel(" "));
        add(qualDescLabel);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
