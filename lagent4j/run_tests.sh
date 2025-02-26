#!/bin/bash
echo "正在运行lagent4j单元测试..."
mvn test
if [ $? -eq 0 ]; then
    echo "测试成功完成！"
else
    echo "测试失败，请检查错误信息。"
fi 