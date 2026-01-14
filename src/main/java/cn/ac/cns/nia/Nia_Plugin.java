package cn.ac.cns.nia;

import cn.ac.cns.nia.engine.InferenceEngine;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * NIA Manager - Final Fixed Version
 * 1. Fixed "Invisible UI" bug by forcing window size.
 * 2. Fixed title spacing.
 * 3. Full-width buttons.
 * 4. Robust image loading.
 * 5. Fixed Log window popping up unexpectedly.
 */
public class Nia_Plugin implements PlugIn {

    // ==========================================
    // 🎨 UI THEME & CONFIG
    // ==========================================
    private static final Color THEME_BLUE = new Color(33, 100, 200);
    private static final Color THEME_RED  = new Color(210, 80, 0);
    
    private static final Font FONT_UI = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    
    // [Fix] 强制设定窗口大小，防止 pack() 导致窗口缩成一条线
    private static final int FRAME_WIDTH = 260;
    private static final int FRAME_HEIGHT = 450; 

    // ==========================================

    private static String lastCustomModelPath = "";
    private static final String BUILT_IN_MODEL_NAME = "dncnn.onnx";
    
    // Components
    private JFrame mainFrame;
    private JRadioButton rbBuiltIn, rbCustom;
    private JTextField txtCustomPath;
    private JButton btnBrowse;
    private JLabel lblImageInfo;
    private JProgressBar progressBar;
    private JButton btnRun;

    @Override
    public void run(String arg) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { /* Ignore */ }

        SwingUtilities.invokeLater(this::initGui);
    }

    private void initGui() {
        // [Fix] 标题去掉空格
        mainFrame = new JFrame("NIA Neural Inference Assistant"); 
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setResizable(true); // 允许调整大小
        
        // [Fix] 强制设置大小，解决“UI看不见”的问题
        mainFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);

        // [Fix] 使用 nia_app_icon.png (或 logo.png，取决于你最终用了哪个名字)
        // 建议你把图片重命名为 nia_app_icon.png 以避免冲突
        try {
            java.net.URL imgURL = getClass().getResource("/logo.png"); // 如果你改名了，这里也要改
            if (imgURL != null) {
                mainFrame.setIconImage(new ImageIcon(imgURL).getImage());
            }
        } catch (Exception e) { /* Ignore */ }

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 8, 8, 8)); 
        mainPanel.setBackground(new Color(250, 250, 250));

        // === 1. Header (Left Aligned) ===
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblLogo = new JLabel();
        try {
            java.net.URL imgURL = getClass().getResource("/logo.png"); // 如果你改名了，这里也要改
            if (imgURL != null) {
                ImageIcon original = new ImageIcon(imgURL);
                // 稍微改大一点点图标尺寸
                Image img = original.getImage().getScaledInstance(42, 42, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(img));
            } else {
                lblLogo.setText("<html><b style='font-size:18px; color:#555;'>NIA</b></html>");
            }
        } catch (Exception e) { /* Ignore */ }

        String version = readVersion();
        String hexBlue = String.format("#%02x%02x%02x", THEME_BLUE.getRed(), THEME_BLUE.getGreen(), THEME_BLUE.getBlue());
        JLabel lblTitle = new JLabel("<html><div style='margin-top:2px;'>" +
                "<span style='font-size:15px; font-weight:bold; color:" + hexBlue + "; font-family:SansSerif;'>NIA Denoise (AI) </span><br>" +
                "<span style='font-size:9px; color:gray; font-family:SansSerif;'>v" + version + " | © 2026 cns.ac.cn</span>" +
                "</div></html>");

        headerPanel.add(lblLogo);
        headerPanel.add(lblTitle);
        
        JSeparator sep = new JSeparator();
        sep.setForeground(Color.LIGHT_GRAY);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(sep);
        mainPanel.add(Box.createVerticalStrut(8));

        // === 2. Model Selection ===
        JPanel modelPanel = createWinManPanel("Model Selection");
        modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.Y_AXIS));

        rbBuiltIn = new JRadioButton("Built-in (DnCNN)");
        rbBuiltIn.setFont(FONT_UI);
        rbBuiltIn.setOpaque(false);
        rbBuiltIn.setSelected(true);
        rbBuiltIn.setFocusPainted(false);
        rbBuiltIn.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbCustom = new JRadioButton("Custom ONNX");
        rbCustom.setFont(FONT_UI);
        rbCustom.setOpaque(false);
        rbCustom.setFocusPainted(false);
        rbCustom.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbBuiltIn);
        bg.add(rbCustom);

        // File Row
        JPanel fileRow = new JPanel(new BorderLayout(2, 0));
        fileRow.setOpaque(false);
        fileRow.setBorder(new EmptyBorder(2, 22, 0, 2));
        fileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        fileRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCustomPath = new JTextField(lastCustomModelPath);
        txtCustomPath.setFont(new Font("SansSerif", Font.PLAIN, 10));
        txtCustomPath.setEnabled(false);
        
        btnBrowse = new JButton("...");
        btnBrowse.setFont(new Font("SansSerif", Font.BOLD, 10));
        btnBrowse.setMargin(new Insets(0, 4, 0, 4));
        btnBrowse.setPreferredSize(new Dimension(25, 20));
        btnBrowse.setEnabled(false);
        btnBrowse.setFocusPainted(false);

        fileRow.add(txtCustomPath, BorderLayout.CENTER);
        fileRow.add(btnBrowse, BorderLayout.EAST);

        // Listeners
        ActionListener toggle = e -> {
            boolean isCustom = rbCustom.isSelected();
            txtCustomPath.setEnabled(isCustom);
            btnBrowse.setEnabled(isCustom);
        };
        rbBuiltIn.addActionListener(toggle);
        rbCustom.addActionListener(toggle);
        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                txtCustomPath.setText(fc.getSelectedFile().getAbsolutePath());
                lastCustomModelPath = txtCustomPath.getText();
            }
        });

        modelPanel.add(rbBuiltIn);
        modelPanel.add(Box.createVerticalStrut(4));
        modelPanel.add(rbCustom);
        modelPanel.add(Box.createVerticalStrut(4));
        modelPanel.add(fileRow);

        mainPanel.add(modelPanel);
        mainPanel.add(Box.createVerticalStrut(8));

        // === 3. Image Info ===
        JPanel infoPanel = createWinManPanel("Image Info");
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        ImagePlus imp = IJ.getImage(); 
        String infoStr = (imp != null) ? getDimsString(imp) : "No Image Open";

        lblImageInfo = new JLabel(infoStr);
        lblImageInfo.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblImageInfo.setForeground(Color.DARK_GRAY);
        lblImageInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox chkLog = new JCheckBox("Show Log");
        chkLog.setFont(FONT_UI);
        chkLog.setOpaque(false);
        chkLog.setSelected(true);
        chkLog.setFocusPainted(false);
        chkLog.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkLog.setBorder(new EmptyBorder(5, 0, 0, 0));

        infoPanel.add(lblImageInfo);
        infoPanel.add(chkLog);

        mainPanel.add(infoPanel);
        mainPanel.add(Box.createVerticalGlue()); 

        // === 4. Action Button ===
        btnRun = new JButton("Start Denoising");
        btnRun.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnRun.setForeground(THEME_RED);
        btnRun.setFocusPainted(false);
        btnRun.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRun.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        btnRun.addActionListener(e -> new Thread(() -> processImage(chkLog.isSelected())).start());

        mainPanel.add(btnRun);
        mainPanel.add(Box.createVerticalStrut(8));

        // === 5. Progress Bar ===
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setFont(FONT_SMALL);
        progressBar.setPreferredSize(new Dimension(100, 14));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setForeground(THEME_BLUE);
        progressBar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        progressBar.setUI(new BasicProgressBarUI() {
            protected Color getSelectionBackground() { return Color.BLACK; }
            protected Color getSelectionForeground() { return Color.WHITE; }
        });

        mainPanel.add(progressBar);

        mainFrame.setContentPane(mainPanel);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private JPanel createWinManPanel(String title) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        Border lineBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, title);
        
        titledBorder.setTitleColor(THEME_BLUE);
        titledBorder.setTitleFont(FONT_BOLD);
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        titledBorder.setTitlePosition(TitledBorder.TOP);
        
        panel.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(4, 8, 8, 8)));
        return panel;
    }

    private String getDimsString(ImagePlus imp) {
        int[] dims = imp.getDimensions();
        return "Size: " + dims[0] + "x" + dims[1] + " | C" + dims[2] + " Z" + dims[3] + " T" + dims[4];
    }

    // ==========================================
    // 🧠 修复后的核心处理逻辑 (解决了 Log 弹窗问题)
    // ==========================================
    private void processImage(boolean showLog) {
        // [Logic Fix] 运行时再次获取图片
        ImagePlus imp = IJ.getImage();
        
        if (imp == null) {
            SwingUtilities.invokeLater(() -> {
                IJ.error("No Image", "Please open an image first.");
                lblImageInfo.setText("No Image Open");
            });
            return;
        }

        SwingUtilities.invokeLater(() -> lblImageInfo.setText(getDimsString(imp)));

        SwingUtilities.invokeLater(() -> {
            btnRun.setEnabled(false);
            btnRun.setText("Processing...");
            progressBar.setIndeterminate(true);
            progressBar.setString("Initializing...");
        });

        // [Fix 1] 只有当用户勾选了 Log 才清理，否则不要调用 IJ.log，因为它会强制打开窗口
        if (showLog) {
            IJ.log("\\Clear");
        }

        String finalModelPath;
        try {
            if (rbBuiltIn.isSelected()) {
                IJ.showStatus("NIA: Extracting...");
                finalModelPath = extractResourceToTemp(BUILT_IN_MODEL_NAME);
            } else {
                String p = txtCustomPath.getText();
                if (p == null || p.isEmpty() || !new File(p).exists()) {
                    IJ.error("Error", "Model not found.");
                    resetUIState();
                    return;
                }
                finalModelPath = p;
            }
        } catch (IOException e) {
            IJ.handleException(e);
            resetUIState();
            return;
        }

        long startTime = System.currentTimeMillis();
        InferenceEngine engine = new InferenceEngine(finalModelPath);
        
        ImageStack stack = imp.getStack();
        int nTotal = imp.getStackSize();
        int nChannels = imp.getNChannels();
        int nSlices = imp.getNSlices();
        int nFrames = imp.getNFrames();

        StackStatistics stats = new StackStatistics(imp);
        double max = stats.max;
        double normFactor = (max <= 0) ? 1.0 : max;

        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
        });

        try {
            int count = 0;
            for (int t = 1; t <= nFrames; t++) {
                for (int z = 1; z <= nSlices; z++) {
                    for (int c = 1; c <= nChannels; c++) {
                        count++;
                        int idx = imp.getStackIndex(c, z, t);
                        
                        int progress = (int) ((count / (float) nTotal) * 100);
                        int finalCount = count;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            progressBar.setString(finalCount + "/" + nTotal);
                        });

                        ImageProcessor ip = stack.getProcessor(idx);
                        ImageProcessor outIp = engine.run(ip, normFactor);

                        if (outIp != null) {
                            if (ip instanceof ij.process.ByteProcessor)
                                stack.setPixels(outIp.convertToByte(false).getPixels(), idx);
                            else if (ip instanceof ij.process.ShortProcessor)
                                stack.setPixels(outIp.convertToShort(false).getPixels(), idx);
                            else
                                stack.setPixels(outIp.getPixels(), idx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            IJ.handleException(e);
        }

        long endTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> {
            imp.updateAndDraw();
            IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            progressBar.setValue(100);
            progressBar.setString("Done");
            btnRun.setText("Finished");
            btnRun.setEnabled(true);
            
            String msg = "✅ Finished in " + (endTime - startTime) + "ms";
            
            // [Fix 2] 只有勾选了 Show Log 才打印到日志窗口
            if (showLog) {
                IJ.log(msg);
            } else {
                // 否则只显示在 ImageJ 底部状态栏，不弹窗
                IJ.showStatus(msg);
            }
        });
    }

    private void resetUIState() {
        SwingUtilities.invokeLater(() -> {
            btnRun.setEnabled(true);
            btnRun.setText("Start Denoising");
            progressBar.setIndeterminate(false);
            progressBar.setString("Error");
        });
    }

    private String readVersion() {
        try (InputStream is = getClass().getResourceAsStream("/version.txt")) {
            if (is == null) return "Dev";
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next().trim();
            }
        } catch (Exception e) { return "Unknown"; }
    }

    private String extractResourceToTemp(String resourceName) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (is == null) is = getClass().getResourceAsStream("/" + resourceName);
        if (is == null) throw new FileNotFoundException("Built-in model not found: " + resourceName);
        File tempFile = File.createTempFile("nia_model_", ".onnx");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile.getAbsolutePath();
    }

    public static void main(String[] args) {
        new ij.ImageJ();
        new Nia_Plugin().run("");
    }
}