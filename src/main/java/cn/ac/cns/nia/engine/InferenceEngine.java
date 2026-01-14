package cn.ac.cns.nia.engine;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ij.IJ;
import ij.process.ImageProcessor;

import java.nio.FloatBuffer;
import java.util.Collections;

/**
 * 优化后的推理引擎
 * 1. 实现了 AutoCloseable，必须显式关闭以释放显存/内存。
 * 2. 模型只加载一次，而不是每张图加载一次。
 */
public class InferenceEngine implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    // 构造函数：只在这里加载模型 (耗时操作只做一次)
    public InferenceEngine(String modelPath) throws OrtException {
        // 1. 获取环境
        this.env = OrtEnvironment.getEnvironment();
        
        // 2. 配置选项
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
        // 如果有 GPU 支持，可以在这里添加: opts.addCUDA(); 

        // 3. 创建 Session (加载模型)
        this.session = env.createSession(modelPath, opts);
        
        // 4. 缓存输入节点名称，避免每次推理都去查询
        this.inputName = session.getInputNames().iterator().next();
    }

    /**
     * 运行推理 (快速)
     */
    public ImageProcessor run(ImageProcessor inputIp, double normalizationFactor) {
        try {
            int width = inputIp.getWidth();
            int height = inputIp.getHeight();

            // --- 1. 预处理 (Pre-processing) ---
            float[] pixels = (float[]) inputIp.convertToFloat().getPixels();
            float normScale = (float) normalizationFactor;
            if (normScale <= 0) normScale = 1.0f;

            // 归一化
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] /= normScale;
            }

            // --- 2. 创建 Tensor ---
            // 注意：这里假设模型输入是 [1, H, W, 1] (NHWC)
            // 如果你的模型是 PyTorch 导出的，通常需要 [1, 1, H, W] (NCHW)
            // 下面是针对 NHWC 的代码：
            long[] shape = new long[]{1, height, width, 1};

            // 使用 try-with-resources 自动关闭 Tensor，防止内存泄漏
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(pixels), shape)) {
                
                // --- 3. 推理 (Inference) ---
                try (OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {
                    
                    // --- 4. 解析结果 (Post-processing) ---
                    OnnxTensor outputTensor = (OnnxTensor) result.get(0);
                    float[][][][] outputData = (float[][][][]) outputTensor.getValue();
                    
                    // 假设输出也是 [1, H, W, 1]
                    int outH = outputData[0].length;
                    int outW = outputData[0][0].length;
                    
                    float[] resultPixels = new float[outW * outH];
                    
                    // 反归一化
                    for (int y = 0; y < outH; y++) {
                        for (int x = 0; x < outW; x++) {
                            // 对应 NHWC: [batch][y][x][channel]
                            float val = outputData[0][y][x][0] * normScale;
                            if (val < 0) val = 0;
                            resultPixels[y * outW + x] = val;
                        }
                    }
                    
                    return new ij.process.FloatProcessor(outW, outH, resultPixels);
                }
            }

        } catch (Exception e) {
            IJ.log("❌ Inference Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 必须实现的方法：释放 Native 内存
     */
    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        if (env != null) {
            env.close();
        }
    }
}