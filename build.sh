#!/bin/bash

export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
echo "JAVA_HOME set to: $JAVA_HOME"

cd syn-data-demo/backend

# 清理并构建项目
/Users/xiong/Desktop/openclaw/anzhuang/apache-maven-3.8.8/bin/mvn clean package -U

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
fi