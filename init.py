import os
from pathlib import Path

# ================= 配置区域 =================
PROJECT_NAME = "NIA_Neural_Inference_Assistant"
ARTIFACT_ID = "nia-imagej"
GROUP_ID = "cn.ac.cns"
VERSION = "0.1.0-SNAPSHOT"
PACKAGE_DIR = "cn/ac/cns/nia"  # 对应 cn.ac.cns.nia
AUTHOR = "Kui Wang"
EMAIL = "k@cns.ac.cn"
URL = "www.cns.ac.cn"
# ===========================================

def create_file(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip())
    print(f"✅ Created: {path}")

def main():
    base_path = Path(PROJECT_NAME)
    
    # 1. 生成 pom.xml (Maven 核心配置)
    pom_content = f"""
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>{GROUP_ID}</groupId>
    <artifactId>{ARTIFACT_ID}</artifactId>
    <version>{VERSION}</version>
    <name>NIA: Neural Inference Assistant</name>
    <description>A native AI denoising assistant for ImageJ, powered by DJL and ONNX Runtime.</description>
    <url>http://{URL}</url>

    <developers>
        <developer>
            <id>kuiwang</id>
            <name>{AUTHOR}</name>
            <email>{EMAIL}</email>
            <organization>CNS</organization>
            <organizationUrl>http://{URL}</organizationUrl>
        </developer>
    </developers>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <djl.version>0.28.0</djl.version>
        <imagej.version>1.54f</imagej.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>net.imagej</groupId>
            <artifactId>ij</artifactId>
            <version>${{imagej.version}}</version>
        </dependency>

        <dependency>
            <groupId>ai.djl</groupId>
            <artifactId>api</artifactId>
            <version>${{djl.version}}</version>
        </dependency>
        
        <dependency>
            <groupId>ai.djl.onnxruntime</groupId>
            <artifactId>onnxruntime-engine</artifactId>
            <version>${{djl.version}}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>com.microsoft.onnxruntime</groupId>
            <artifactId>onnxruntime</artifactId>
            <version>1.17.1</version>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.36</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>scijava.public</id>
            <url>https://maven.scijava.org/content/groups/public</url>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <relocations>
                                <relocation>
                                    <pattern>ai.djl</pattern>
                                    <shadedPattern>cn.ac.cns.nia.internal.djl</shadedPattern>
                                </relocation>
                            </relocations>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
"""
    create_file(base_path / "pom.xml", pom_content)

    # 2. 生成 Java 主插件类 (NiaPlugin.java)
    java_main_dir = base_path / "src" / "main" / "java" / PACKAGE_DIR
    plugin_class_content = f"""
package cn.ac.cns.nia;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import cn.ac.cns.nia.engine.InferenceEngine;

/**
 * Neural Inference Assistant (NIA)
 * Author: {AUTHOR}
 * Email: {EMAIL}
 */
public class NiaPlugin implements PlugIn {{

    @Override
    public void run(String arg) {{
        IJ.log("Starting NIA - Neural Inference Assistant...");
        
        ImagePlus imp = IJ.getImage();
        if (imp == null) {{
            IJ.error("No image open!");
            return;
        }}

        // TODO: 添加简单的 GUI 对话框 (GenericDialog)
        
        // 调用推理引擎
        InferenceEngine engine = new InferenceEngine();
        boolean success = engine.checkEngineAvailability();
        
        if (success) {{
            IJ.log("AI Engine is ready. Starting inference...");
            // engine.run(imp);
        }} else {{
            IJ.error("Could not initialize ONNX Runtime.");
        }}
    }}
}}
"""
    create_file(java_main_dir / "NiaPlugin.java", plugin_class_content)

    # 3. 生成推理引擎骨架 (InferenceEngine.java)
    engine_dir = java_main_dir / "engine"
    engine_content = f"""
package cn.ac.cns.nia.engine;

import ai.djl.engine.Engine;
import ij.IJ;

public class InferenceEngine {{

    public InferenceEngine() {{
        // 初始化
    }}

    /**
     * 检查是否能加载 ONNX Runtime
     */
    public boolean checkEngineAvailability() {{
        try {{
            // 尝试加载引擎，DJL 会自动下载必要的 Native 库
            String engineName = Engine.getInstance().getEngineName();
            IJ.log("Detected Engine: " + engineName);
            return true;
        }} catch (Exception e) {{
            IJ.log("Error loading engine: " + e.getMessage());
            e.printStackTrace();
            return false;
        }}
    }}
    
    // TODO: 实现 runInference 方法，将 ImageProcessor 转为 Tensor
}}
"""
    create_file(engine_dir / "InferenceEngine.java", engine_content)

    # 4. 创建资源目录 (放模型)
    resources_dir = base_path / "src" / "main" / "resources"
    resources_dir.mkdir(parents=True, exist_ok=True)
    with open(resources_dir / "PUT_YOUR_MODEL_HERE.txt", 'w') as f:
        f.write("Please place your .onnx model file in this folder.\nNaming it 'model.onnx' is recommended.")
    print(f"✅ Created: {resources_dir} (For .onnx models)")

    # 5. 生成 .gitignore
    gitignore_content = """
target/
*.iml
.idea/
.settings/
.classpath
.project
*.DS_Store
"""
    create_file(base_path / ".gitignore", gitignore_content)

    # 6. 生成 README.md
    readme_content = f"""
# NIA: Neural Inference Assistant

**Author:** {AUTHOR} ({EMAIL})  
**Organization:** {URL}

## Description
NIA is a native ImageJ plugin designed for seamless AI-based image denoising using ONNX Runtime. 

## Requirements
* ImageJ / Fiji
* Java 8 or higher
* Internet connection (for first-time engine download)

## Development
This project is built using Maven.
"""
    create_file(base_path / "README.md", readme_content)

    print("\n🎉 Project structure generated successfully!")
    print(f"📂 Location: {base_path.absolute()}")
    print("👉 Next Step: Open this folder as a 'Maven Project' in IntelliJ IDEA or Eclipse.")

if __name__ == "__main__":
    main()