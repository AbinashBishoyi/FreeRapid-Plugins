package cz.vity.freerapid.plugins.services.ceskatelevize;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author JPEXS
 */
public class CeskaTelevizeSettingsPanel extends JPanel {
    private CeskaTelevizeSettingsConfig config;
    private int qualityMapping[]=new int[]{-1,288,404,576,720,-2};

    public CeskaTelevizeSettingsPanel(CeskaTelevizeServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"Lowest available","288p","404p","576p","720p","Highest available"};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        int qs = config.getQualitySetting();
        for(int i=0;i<qualityMapping.length;i++){
            if(qualityMapping[i]==qs){
                qualityList.setSelectedIndex(i);
                break;
            }
        }
        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityMapping[qualityList.getSelectedIndex()]);
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        //add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}