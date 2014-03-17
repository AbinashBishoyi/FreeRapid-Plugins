package cz.vity.freerapid.plugins.services.vimeo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
public class VimeoSettingsPanel extends JPanel {
    private VimeoSettingsConfig config;

    public VimeoSettingsPanel(VimeoServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"High Definition", "Standard Definition", "Mobile"};
        final int[] qualityIndexMap = {2, 1, 0};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        int qs = config.getQualitySetting();
        for (int i = 0; i < qualityIndexMap.length; i++) {
            if (qualityIndexMap[i] == qs) {
                qualityList.setSelectedIndex(i);
                break;
            }
        }

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityIndexMap[qualityList.getSelectedIndex()]);
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
