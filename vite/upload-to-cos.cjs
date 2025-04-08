const COS = require('cos-nodejs-sdk-v5');
const path = require('path');

// ========== 配置区域 ==========
// 替换为你的腾讯云密钥
const fs = require('fs');
const envPath = require('path').resolve(__dirname, '.env.cos');
if (fs.existsSync(envPath)) {
  const envContent = fs.readFileSync(envPath, 'utf-8');
  envContent.split('\n').forEach(line => {
    const match = line.match(/^\s*([\w.-]+)\s*=\s*(.*)\s*$/);
    if (match) {
      const key = match[1];
      const value = match[2];
      process.env[key] = value;
    }
  });
}

const SecretId = process.env.COS_SECRET_ID;
const SecretKey = process.env.COS_SECRET_KEY;

// Bucket名称，格式如 'example-1250000000'
const Bucket = '1-1317280880';
// 地域，例如 'ap-shanghai'
const Region = 'ap-beijing';

// 本地APK文件路径
const apkFilePath = path.resolve(__dirname, 'app-release.apk');
// 本地版本描述文件路径
const versionFilePath = path.resolve(__dirname, 'version.json');

// ========== 初始化COS客户端 ==========
const cos = new COS({
  SecretId,
  SecretKey,
});

// ========== 上传函数 ==========
function uploadFile(filePath, cosKey) {
  return new Promise((resolve, reject) => {
    cos.putObject({
      Bucket,
      Region,
      Key: cosKey,
      Body: fs.createReadStream(filePath),
      ContentLength: fs.statSync(filePath).size,
    }, (err, data) => {
      if (err) {
        console.error(`上传 ${cosKey} 失败:`, err);
        reject(err);
      } else {
        console.log(`上传成功: https://${Bucket}.cos.${Region}.myqcloud.com/${cosKey}`);
        resolve(data);
      }
    });
  });
}

// ========== 执行上传 ==========
async function main() {
  try {
    await uploadFile(apkFilePath, 'app-release.apk'); // 可改为 'apk/app-release.apk' 指定子目录
    await uploadFile(versionFilePath, 'version.json');
    console.log('全部文件上传完成');
  } catch (error) {
    console.error('上传过程中出错:', error);
  }
}

main();