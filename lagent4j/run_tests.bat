@echo off
echo 正在运行lagent4j单元测试...
call mvn test
if %ERRORLEVEL% == 0 (
    echo 测试成功完成！
) else (
    echo 测试失败，请检查错误信息。
)
pause 