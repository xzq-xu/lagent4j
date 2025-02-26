@echo off
echo ===== Lagent4j GitHub提交助手 =====
echo.

REM 检查git是否安装
where git >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未找到Git。请先安装Git: https://git-scm.com/downloads
    pause
    exit /b 1
)

REM 检查是否已初始化git仓库
if not exist .git (
    echo 正在初始化Git仓库...
    git init
    if %ERRORLEVEL% NEQ 0 (
        echo 错误: 无法初始化Git仓库。
        pause
        exit /b 1
    )
)

REM 检查远程仓库是否已配置
git remote -v | findstr origin >nul
if %ERRORLEVEL% NEQ 0 (
    echo 请输入您的GitHub仓库URL (例如: https://github.com/username/lagent4j.git):
    set /p REPO_URL=
    
    git remote add origin %REPO_URL%
    if %ERRORLEVEL% NEQ 0 (
        echo 错误: 无法添加远程仓库。
        pause
        exit /b 1
    )
    echo 成功添加远程仓库: %REPO_URL%
)

REM 显示当前状态
echo.
echo 当前Git状态:
git status
echo.

REM 添加所有文件
echo 是否添加所有文件到暂存区? (Y/N)
set /p ADD_ALL=
if /i "%ADD_ALL%"=="Y" (
    git add .
    echo 已添加所有文件到暂存区。
) else (
    echo 请手动添加文件后继续。
    pause
)

REM 提交更改
echo.
echo 请输入提交信息:
set /p COMMIT_MSG=

git commit -m "%COMMIT_MSG%"
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 提交失败。
    pause
    exit /b 1
)
echo 成功创建提交。

REM 推送到远程仓库
echo.
echo 是否推送到GitHub? (Y/N)
set /p PUSH=
if /i "%PUSH%"=="Y" (
    echo 正在推送到GitHub...
    
    REM 检查是否有分支
    for /f %%i in ('git branch --show-current') do set CURRENT_BRANCH=%%i
    if "%CURRENT_BRANCH%"=="" (
        echo 未检测到分支，创建main分支...
        git checkout -b main
        set CURRENT_BRANCH=main
    )
    
    git push -u origin %CURRENT_BRANCH%
    if %ERRORLEVEL% NEQ 0 (
        echo 错误: 推送失败。可能需要先在GitHub创建仓库或检查凭据。
        pause
        exit /b 1
    )
    echo 成功推送到GitHub!
) else (
    echo 已跳过推送到GitHub。
)

echo.
echo 操作完成!
pause 