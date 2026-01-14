package cn.ac.cns.nia.engine;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ij.IJ;
import ij.process.ImageProcessor;

import java.nio.FloatBuffer;
import java.util.Collections;

public class InferenceEngine {

    private String modelPath;

    public InferenceEngine(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * 运行推理
     * @param inputIp 输入的 2D 图像处理器
     * @param normalizationFactor 归一化因子 (通常是图像最大值或理论最大值)
     * @return 去噪后的 32-bit FloatProcessor
     */
    public ImageProcessor run(ImageProcessor inputIp, double normalizationFactor) {
        try (OrtEnvironment env = OrtEnvironment.getEnvironment()) {
            // 设置 Session 选项 (开启优化)
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            try (OrtSession session = env.createSession(modelPath, opts)) {
                String inputName = session.getInputNames().iterator().next();
                
                int width = inputIp.getWidth();
                int height = inputIp.getHeight();

                // 1. 预处理
                float[] pixels = (float[]) inputIp.convertToFloat().getPixels();
                float normScale = (float) normalizationFactor;
                
                // 防止除以 0
                if (normScale <= 0) normScale = 1.0f;

                // 归一化 (Pixel / Factor)
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] /= normScale;
                }

                // 2. 创建 Tensor [1, H, W, 1] (NHWC 格式)
                long[] shape = new long[]{1, height, width, 1};
                
                try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(pixels), shape)) {
                    
                    // 3. 推理
                    OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor));
                    
                    // 4. 解析结果
                    OnnxTensor outputTensor = (OnnxTensor) result.get(0);
                    float[][][][] outputData = (float[][][][]) outputTensor.getValue();
                    
                    int outH = outputData[0].length;
                    int outW = outputData[0][0].length;
                    
                    float[] resultPixels = new float[outW * outH];
                    
                    // 5. 反归一化 (Val * Factor)
                    for (int y = 0; y < outH; y++) {
                        for (int x = 0; x < outW; x++) {
                            // NHWC 输出: [0][y][x][0]
                            float val = outputData[0][y][x][0] * normScale;
                            
                            // 钳制范围 (Clip)，防止溢出
                            if (val < 0) val = 0;
                            // 对于 32-bit float 图，通常不限制上限；但对于整数图，最好限制一下
                            // 这里我们暂不限制上限，交给 Plugin 层去处理转换
                            
                            resultPixels[y * outW + x] = val;
                        }
                    }
                    
                    return new ij.process.FloatProcessor(outW, outH, resultPixels);
                }
            }
        } catch (Exception e) {
            IJ.log("❌ Engine Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}