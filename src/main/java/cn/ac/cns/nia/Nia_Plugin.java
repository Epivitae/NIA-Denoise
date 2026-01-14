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
 * NIA Manager - Fail-Safe Version
 * 1. Robust Image Loading: Won't crash if images are missing.
 * 2. Fallback to Text if logos are not found.
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
    
    private static final int FRAME_WIDTH = 260; 
    private static final int FRAME_HEIGHT = 400; 

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
        mainFrame = new JFrame("AI Denoise"); 
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setResizable(true);
        mainFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);

        // [Safe Load 1] 窗口图标：如果找不到，就什么都不做，不会崩
        Image appIcon = safeLoadImage("/NIA.png");
        if (appIcon != null) {
            mainFrame.setIconImage(appIcon);
        }

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 8, 8, 8)); 
        mainPanel.setBackground(new Color(250, 250, 250));

        // ==========================================
        // Header (Robust Logic)
        // ==========================================
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); 
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // [Safe Load 2] Logo 组件
        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = safeLoadIcon("/NIA.png", 32, 32);
        
        if (logoIcon != null) {
            // ✅ 有图片：显示图片
            lblLogo.setIcon(logoIcon);
            lblLogo.setText(null);
        } else {
            // ❌ 没图片：显示备用文字 (防止界面空白)
            lblLogo.setText("<html><span style='font-size:18px; font-weight:bold; color:#555;'>NIA</span></html>");
            lblLogo.setIcon(null);
        }

        // 标题文字
        String version = readVersion();
        String hexBlue = String.format("#%02x%02x%02x", THEME_BLUE.getRed(), THEME_BLUE.getGreen(), THEME_BLUE.getBlue());
        
        JLabel lblTitle = new JLabel("<html><div style='margin-top:0px;'>" +
                "<span style='font-size:15px; font-weight:bold; color:" + hexBlue + "; font-family:SansSerif;'>NIA Denoise</span><br>" +
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

        // 设置 ContentPane
        mainFrame.setContentPane(mainPanel);
        
        // 自动刷新图片信息
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                ImagePlus imp = IJ.getImage();
                String info = (imp != null) ? getDimsString(imp) : "No Image Open";
                lblImageInfo.setText(info);
            }
        });
        
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true); // 确保最后一定显示
    }

    // ==========================================
    // 🛡️ 安全的图片加载助手 (防止没图时崩溃)
    // ==========================================
    private Image safeLoadImage(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null; // 文件不存在
            return new ImageIcon(url).getImage();
        } catch (Exception e) {
            return null; // 加载失败，静默返回 null
        }
    }

    private ImageIcon safeLoadIcon(String path, int w, int h) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            ImageIcon original = new ImageIcon(url);
            if (original.getIconWidth() <= 0) return null; // 图片损坏
            Image img = original.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return null;
        }
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
    // 🧠 核心处理逻辑 (Memory Safe)
    // ==========================================
    private void processImage(boolean showLog) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            SwingUtilities.invokeLater(() -> {
                IJ.error("No Image", "Please open an image first.");
                lblImageInfo.setText("No Image Open");
            });
            return;
        }

        if (showLog) IJ.log("Processing on ORIGINAL image (Destructive)...");

        SwingUtilities.invokeLater(() -> {
            lblImageInfo.setText(getDimsString(imp));
            btnRun.setEnabled(false);
            btnRun.setText("Processing...");
            progressBar.setIndeterminate(true);
            progressBar.setString("Initializing Engine...");
        });

        if (showLog) IJ.log("\\Clear");

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

        InferenceEngine engine = null;

        try {
            long startTime = System.currentTimeMillis();
            
            engine = new InferenceEngine(finalModelPath);

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

            int count = 0;
            
            for (int t = 1; t <= nFrames; t++) {
                for (int z = 1; z <= nSlices; z++) {
                    for (int c = 1; c <= nChannels; c++) {
                        count++;
                        int idx = imp.getStackIndex(c, z, t);

                        if (count % 5 == 0 || count == nTotal) {
                            int finalProgress = (int) ((count / (float) nTotal) * 100);
                            int finalCount = count;
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(finalProgress);
                                progressBar.setString(finalCount + "/" + nTotal);
                            });
                        }

                        ImageProcessor ip = stack.getProcessor(idx);
                        ImageProcessor outIp = engine.run(ip, normFactor);

                        if (outIp != null) {
                            Object pixels;
                            if (stack.getBitDepth() == 8) pixels = outIp.convertToByte(false).getPixels();
                            else if (stack.getBitDepth() == 16) pixels = outIp.convertToShort(false).getPixels();
                            else pixels = outIp.getPixels();
                            
                            stack.setPixels(pixels, idx);
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            SwingUtilities.invokeLater(() -> {
                imp.updateAndDraw(); 
                IJ.run(imp, "Enhance Contrast", "saturated=0.35");
                
                progressBar.setValue(100);
                progressBar.setString("Done");
                
                String msg = "✅ Finished in " + duration + "ms";
                if (showLog) IJ.log(msg);
                else IJ.showStatus(msg);

                javax.swing.Timer resetTimer = new javax.swing.Timer(2000, e -> {
                    if (mainFrame.isVisible()) {
                        btnRun.setText("Start Denoising");
                        btnRun.setEnabled(true);
                        progressBar.setString("Ready");
                        progressBar.setValue(0);
                    }
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            });

        } catch (Exception e) {
            IJ.handleException(e);
            resetUIState();
        } finally {
            if (engine != null) {
                try {
                    if (engine instanceof AutoCloseable) {
                        ((AutoCloseable) engine).close();
                    } 
                } catch (Exception ex) {
                    System.err.println("Failed to close engine: " + ex.getMessage());
                }
            }
        }
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