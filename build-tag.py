import os
import subprocess
import sys
import shutil
import xml.etree.ElementTree as ET
from collections import deque
from rich.console import Console
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich.live import Live
from rich.text import Text
from rich.traceback import install

# 安装 Rich 错误追踪，报错更优雅
install()
console = Console()

# ================= 配置区 =================
PROJECT_NAME = "NIA Denoise"  # ✅ 已修改为 NIA
# 如果目录里有这个模板，会自动生成 .zenodo.json；如果没有则自动跳过
TEMPLATE_FILE = ".zenodo.template.json"
OUTPUT_FILE = ".zenodo.json"

# 滚动日志的显示行数
LOG_HEIGHT = 12
# ==========================================

def get_build_command():
    """自动检测是用 mvnd (Maven Daemon) 还是 mvn"""
    if shutil.which("mvnd"):
        return "mvnd clean package"
    return "mvn clean package"

def run_process_with_live_log(command, live, log_lines, generate_panel_func, allow_failure=False):
    """
    运行单个命令，并将输出实时喂给 Live 面板
    """
    log_lines.append(f"[dim]⚡ 执行: {command}[/]")
    live.update(generate_panel_func())

    # Windows下 shell=True 是必须的，errors='replace' 防止GBK/UTF-8乱码崩溃
    process = subprocess.Popen(
        command, 
        shell=True, 
        stdout=subprocess.PIPE, 
        stderr=subprocess.STDOUT, 
        text=True,
        encoding='utf-8', 
        errors='replace'
    )

    while True:
        line = process.stdout.readline()
        if not line and process.poll() is not None:
            break
        if line:
            clean_line = line.strip()
            if clean_line:
                # 简单过滤掉太长的 Maven 下载日志，保持界面清爽
                if len(clean_line) > 100 and "Downloading" in clean_line:
                    clean_line = clean_line[:97] + "..."
                log_lines.append(clean_line)
                live.update(generate_panel_func())

    if process.returncode != 0:
        if allow_failure:
            log_lines.append(f"[yellow]⚠️  该步骤失败但被忽略 (允许失败)[/]")
            live.update(generate_panel_func())
            return True
        else:
            return False
    return True

def run_sequence_in_window(steps, title, final_success_msg):
    """
    在滚动窗口中运行一系列命令
    steps: list of (command, allow_failure_bool)
    """
    log_lines = deque(maxlen=LOG_HEIGHT)
    
    def generate_panel():
        log_content = Text.from_markup("\n".join(log_lines))
        return Panel(
            log_content,
            title=f"[bold blue]⏳ {title}[/]",
            border_style="blue",
            height=LOG_HEIGHT + 2,
            padding=(0, 1)
        )

    with Live(generate_panel(), refresh_per_second=10, console=console) as live:
        for cmd, allow_fail in steps:
            success = run_process_with_live_log(cmd, live, log_lines, generate_panel, allow_fail)
            if not success:
                console.print(Panel(f"[bold red]❌ 执行失败！[/]\n命令: {cmd}\n请检查上方日志。", style="red"))
                sys.exit(1)
    
    console.print(f"[bold green]✅ {final_success_msg}[/]")

def get_pom_version():
    """从 pom.xml 读取版本号"""
    pom_file = "pom.xml"
    if not os.path.exists(pom_file):
        console.print(f"[bold red]❌ 错误: 找不到 {pom_file}[/]")
        sys.exit(1)
    try:
        # 注册命名空间，防止解析带 xmlns 的 xml 出错
        ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")
        tree = ET.parse(pom_file)
        root = tree.getroot()
        # 命名空间处理
        ns = {'mvn': 'http://maven.apache.org/POM/4.0.0'}
        
        # 尝试直接找 version
        version_tag = root.find('mvn:version', ns)
        if version_tag is not None:
            return version_tag.text.strip()
            
        console.print("[bold red]❌ 错误: 无法在 pom.xml 中找到 <version>[/]")
        sys.exit(1)
    except Exception as e:
        console.print(f"[bold red]❌ 解析 pom.xml 失败: {e}[/]")
        sys.exit(1)

def ask_for_version(detected_version):
    console.print(Panel.fit(
        f"🔍 检测到 pom.xml 版本: [bold cyan]{detected_version}[/]",
        title="版本检测", border_style="blue"
    ))
    return Prompt.ask("📝 请确认发布版本号 (Git Tag)", default=detected_version)

def generate_zenodo_json(version):
    """如果有模板，生成 Zenodo 元数据；否则跳过"""
    if not os.path.exists(TEMPLATE_FILE):
        console.print(f"[dim]ℹ️  未找到 {TEMPLATE_FILE}，跳过 Zenodo 元数据生成[/]")
        return False
        
    try:
        with open(TEMPLATE_FILE, 'r', encoding='utf-8') as f:
            content = f.read()
        new_content = content.replace("{{VERSION}}", version)
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            f.write(new_content)
        console.print(f"[green]✅ 已更新元数据: {OUTPUT_FILE}[/]")
        return True
    except Exception as e:
        console.print(f"[red]❌ 生成 Zenodo 文件失败: {e}[/]")
        return False

def build_project():
    cmd = get_build_command()
    console.rule("[bold green]🔨 第一步：构建项目[/]")
    console.print(f"[dim]使用构建命令: {cmd}[/]")
    
    steps = [(cmd, False)]
    run_sequence_in_window(steps, "正在执行 Maven 构建...", "构建完成")

def git_operations(version, has_zenodo):
    tag_name = f"v{version}"
    console.rule(f"[bold cyan]🚀 第二步：发布 {tag_name}[/]")
    
    if not Confirm.ask(f"❓ 确认将 [bold green]{tag_name}[/] 推送到 GitHub 吗?"):
        console.print("[bold red]🚫 操作已取消[/]")
        sys.exit(0)

    console.print("[bold blue]📦 正在提交代码...[/]")
    
    # 1. 准备要提交的文件
    files_to_add = "pom.xml"
    if has_zenodo:
        files_to_add += f" {OUTPUT_FILE}"
    
    # 执行添加和提交 (在后台运行，不占用 UI)
    subprocess.run(f"git add {files_to_add}", shell=True)
    subprocess.run(f'git commit -m "chore: release {tag_name}"', shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 2. 定义 Git 操作序列
    git_steps = [
        # 推送当前分支代码 (确保 version 变更被推送)
        ("git push origin main", False), 
        
        # 删除本地旧 Tag (允许失败)
        (f"git tag -d {tag_name}", True),
        
        # 删除远程旧 Tag (允许失败)
        (f"git push origin :refs/tags/{tag_name}", True),
        
        # 打新 Tag
        (f"git tag -a {tag_name} -m \"Release {tag_name}\"", False),
        
        # 推送新 Tag
        (f"git push origin {tag_name}", False)
    ]

    # 在滚动窗口中执行这些步骤
    run_sequence_in_window(git_steps, "执行 Git 推送与打标...", "Git 发布完成")

    console.print(Panel.fit(
        f"[bold green]🎉 发布成功！[/]\n\n"
        f"项目: [bold white]{PROJECT_NAME}[/]\n"
        f"版本: [bold cyan]{tag_name}[/]\n\n"
        f"👉 下一步: 请前往 GitHub Releases 页面基于 [bold cyan]{tag_name}[/] 发布 Release。\n"
        f"   (如果配置了 Action，Release 会自动触发构建)",
        title="NIA 发布助手",
        border_style="green"
    ))

if __name__ == "__main__":
    # 标题栏
    console.print(Panel.fit(
        f"[bold white]{PROJECT_NAME} 自动化发布工具[/] [dim](for CNS Lab)[/]", 
        style="bold blue"
    ))
    
    # 1. 获取并确认版本
    ver = get_pom_version()
    final_ver = ask_for_version(ver)
    
    # 2. 生成附属文件 (如果模板存在)
    has_zenodo = generate_zenodo_json(final_ver)
    
    # 3. 构建 (mvn package)
    build_project()
    
    # 4. Git 打标与推送
    git_operations(final_ver, has_zenodo)