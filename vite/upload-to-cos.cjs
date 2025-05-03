const COS = require('cos-nodejs-sdk-v5');
const path = require('path');
const fs = require('fs');
const { promisify } = require('util'); // 用于将回调风格的 API 转换为 Promise
require('dotenv').config({ path: path.resolve(__dirname, '..', '.env') }); // 从项目根目录加载 .env 文件

// ========== 配置加载 ==========
// dotenv 已经将变量加载到 process.env

const SecretId = process.env.COS_SECRET_ID;
const SecretKey = process.env.COS_SECRET_KEY;
const Bucket = process.env.COS_BUCKET; // 从 env 读取 Bucket
const Region = process.env.COS_REGION; // 从 env 读取 Region

if (!SecretId || !SecretKey || !Bucket || !Region) {
  console.error('错误：缺少 COS 配置信息 (COS_SECRET_ID, COS_SECRET_KEY, COS_BUCKET, COS_REGION)。请检查项目根目录的 .env 文件。');
  process.exit(1);
}

// ========== 初始化COS客户端 ==========
const cos = new COS({ SecretId, SecretKey });
const putObjectPromise = promisify(cos.putObject).bind(cos);
// cos.sliceUploadFile 可能对大文件更好，但 putObject 更简单
// const sliceUploadFilePromise = promisify(cos.sliceUploadFile).bind(cos); 

// ========== 辅助函数 ==========

/**
 * 上传单个文件到 COS
 * @param {string} localPath 本地文件完整路径
 * @param {string} cosKey COS 上的目标 Key (路径+文件名)
 * @param {string} [contentType] 可选的内容类型
 */
async function uploadSingleFile(localPath, cosKey, contentType) {
  if (!fs.existsSync(localPath)) {
    throw new Error(`本地文件未找到: ${localPath}`);
  }
  console.log(`  上传文件: ${localPath} -> ${cosKey}`);
  const params = {
    Bucket,
    Region,
    Key: cosKey,
    Body: fs.createReadStream(localPath),
    ContentLength: fs.statSync(localPath).size,
  };
  if (contentType) {
    params.ContentType = contentType;
  }
  try {
    const data = await putObjectPromise(params);
    console.log(`  上传成功: ${data.Location}`); // data.Location 包含完整 URL
    return data;
  } catch (err) {
    console.error(`  上传 ${cosKey} 失败:`, err);
    throw err; // 重新抛出错误以便上层捕获
  }
}

/**
 * 递归上传目录内容到 COS
 * @param {string} localDirPath 本地目录路径
 * @param {string} cosPrefix COS 上的目标前缀 (例如 '/')
 */
async function uploadDirectory(localDirPath, cosPrefix = '/') {
  if (!fs.existsSync(localDirPath) || !fs.statSync(localDirPath).isDirectory()) {
    throw new Error(`本地目录无效或未找到: ${localDirPath}`);
  }
  console.log(`上传目录: ${localDirPath} -> ${cosPrefix}`);
  
  const files = fs.readdirSync(localDirPath);
  for (const file of files) {
    const localFilePath = path.join(localDirPath, file);
    const stats = fs.statSync(localFilePath);
    // 确保 cosKey 使用 / 作为分隔符，并移除可能的前导 / (如果 prefix 是 /)
    const cosKey = (cosPrefix.endsWith('/') ? cosPrefix : cosPrefix + '/') + file; 
    const cleanCosKey = cosKey.startsWith('/') ? cosKey.substring(1) : cosKey; 

    if (stats.isDirectory()) {
      // 递归上传子目录
      await uploadDirectory(localFilePath, cleanCosKey + '/'); 
    } else if (stats.isFile()) {
      // 上传文件 (可以根据文件扩展名猜测 ContentType，但简单起见这里省略)
      await uploadSingleFile(localFilePath, cleanCosKey);
    }
  }
  console.log(`目录 ${localDirPath} 上传完成。`);
}

// ========== 主逻辑 ==========
async function main() {
  // 解析命令行参数 (更健壮的方式)
  const args = process.argv.slice(2);
  const typeIndex = args.indexOf('--type');
  const sourceIndex = args.indexOf('--source');
  const targetIndex = args.indexOf('--target');

  if (typeIndex === -1 || sourceIndex === -1 || targetIndex === -1 || 
      args.length <= typeIndex + 1 || args.length <= sourceIndex + 1 || args.length <= targetIndex + 1) {
    console.error('用法: node upload-to-cos.cjs --type <web|apk|version> --source <local_path> --target <cos_key_or_prefix>');
    process.exit(1);
  }

  const uploadType = args[typeIndex + 1];
  const sourcePath = path.resolve(__dirname, '..', args[sourceIndex + 1]); // source 相对于项目根目录
  const targetPath = args[targetIndex + 1]; // target 是 COS 上的路径

  console.log(`\n开始上传任务: 类型=${uploadType}, 源=${sourcePath}, 目标=${targetPath}`);

  try {
    switch (uploadType) {
      case 'web':
        await uploadDirectory(sourcePath, targetPath);
        break;
      case 'apk':
        // targetPath 应该是完整的 COS Key (路径+文件名)
        await uploadSingleFile(sourcePath, targetPath.startsWith('/') ? targetPath.substring(1) : targetPath);
        break;
      case 'version':
        // targetPath 应该是完整的 COS Key (路径+文件名)
        await uploadSingleFile(sourcePath, targetPath.startsWith('/') ? targetPath.substring(1) : targetPath, 'application/json');
        break;
      default:
        console.error(`错误：未知的上传类型 "${uploadType}"。支持的类型: web, apk, version`);
        process.exit(1);
    }
    console.log(`上传任务 (${uploadType}) 成功完成。\n`);
  } catch (error) {
    console.error(`上传任务 (${uploadType}) 失败:`, error);
    process.exit(1); // 确保脚本失败时返回非零退出码
  }
}

main();
