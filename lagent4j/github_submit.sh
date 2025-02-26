#!/bin/bash

echo "===== Lagent4j GitHub提交助手 ====="
echo

# 检查git是否安装
if ! command -v git &> /dev/null; then
    echo "错误: 未找到Git。请先安装Git"
    echo "Ubuntu/Debian: sudo apt-get install git"
    echo "CentOS/RHEL: sudo yum install git"
    echo "macOS: brew install git"
    exit 1
fi

# 检查是否已初始化git仓库
if [ ! -d .git ]; then
    echo "正在初始化Git仓库..."
    git init
    if [ $? -ne 0 ]; then
        echo "错误: 无法初始化Git仓库。"
        exit 1
    fi
fi

# 检查远程仓库是否已配置
if ! git remote -v | grep -q origin; then
    echo "请输入您的GitHub仓库URL (例如: https://github.com/username/lagent4j.git):"
    read REPO_URL
    
    git remote add origin $REPO_URL
    if [ $? -ne 0 ]; then
        echo "错误: 无法添加远程仓库。"
        exit 1
    fi
    echo "成功添加远程仓库: $REPO_URL"
fi

# 显示当前状态
echo
echo "当前Git状态:"
git status
echo

# 添加所有文件
echo "是否添加所有文件到暂存区? (Y/N)"
read ADD_ALL
if [ "$ADD_ALL" = "Y" ] || [ "$ADD_ALL" = "y" ]; then
    git add .
    echo "已添加所有文件到暂存区。"
else
    echo "请手动添加文件后继续。"
    read -p "按Enter键继续..."
fi

# 提交更改
echo
echo "请输入提交信息:"
read COMMIT_MSG

git commit -m "$COMMIT_MSG"
if [ $? -ne 0 ]; then
    echo "错误: 提交失败。"
    exit 1
fi
echo "成功创建提交。"

# 推送到远程仓库
echo
echo "是否推送到GitHub? (Y/N)"
read PUSH
if [ "$PUSH" = "Y" ] || [ "$PUSH" = "y" ]; then
    echo "正在推送到GitHub..."
    
    # 检查是否有分支
    CURRENT_BRANCH=$(git branch --show-current)
    if [ -z "$CURRENT_BRANCH" ]; then
        echo "未检测到分支，创建main分支..."
        git checkout -b main
        CURRENT_BRANCH="main"
    fi
    
    git push -u origin $CURRENT_BRANCH
    if [ $? -ne 0 ]; then
        echo "错误: 推送失败。可能需要先在GitHub创建仓库或检查凭据。"
        exit 1
    fi
    echo "成功推送到GitHub!"
else
    echo "已跳过推送到GitHub。"
fi

echo
echo "操作完成!" 