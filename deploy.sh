#!/bin/bash

# ====== 可配置参数 ======
REGISTRY_URL="crpi-xlafczp8xa83qpr9.cn-beijing.personal.cr.aliyuncs.com"
NAMESPACE="gaoxinkai"
IMAGE_NAME="myapp"
SERVER_IP="101.42.104.145"
SERVER_USER="root"
SERVER_PORT_MAP="9850:8085"
ALIYUN_USER="1102336460@qq.com"
ALIYUN_PASS="gxk19990805"
# ========================

cd "$(dirname "$0")"

# 从 vite/version.json 中读取版本号
VERSION=$(grep -oP '(?<="version": ")[^"]+' vite/version.json)

echo "==== 读取版本号 version.json：$VERSION ===="

FULL_IMAGE_TAG="$REGISTRY_URL/$NAMESPACE/$IMAGE_NAME:$VERSION"

echo "==== Step 1: 跳过 Maven 打包，假设 jar 包已经准备好 ===="

echo "==== Step 2: 构建 Docker 镜像 ===="
docker build -t $IMAGE_NAME:$VERSION ./mine-api

echo "==== Step 3: 镜像打标签 ===="
docker tag $IMAGE_NAME:$VERSION $FULL_IMAGE_TAG

echo "==== Step 4: 登录阿里云仓库 ===="
docker login $REGISTRY_URL -u $ALIYUN_USER -p $ALIYUN_PASS

echo "==== Step 5: 推送镜像到仓库 ===="
docker push $FULL_IMAGE_TAG

echo "==== Step 6: SSH到服务器，拉取镜像并运行容器 ===="
ssh $SERVER_USER@$SERVER_IP "
docker login $REGISTRY_URL -u $ALIYUN_USER -p $ALIYUN_PASS && \
docker pull $FULL_IMAGE_TAG && \
docker stop $IMAGE_NAME || true && docker rm $IMAGE_NAME || true && \
docker run -d -p $SERVER_PORT_MAP --name $IMAGE_NAME $FULL_IMAGE_TAG
"

echo "==== 自动化完成，镜像版本号：$VERSION，$SERVER_PORT_MAP ===="