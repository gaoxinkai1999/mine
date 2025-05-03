const fs = require('fs');
// Removed duplicate fs require
const path = require('path');
const config = require('./versions.config.js');

// 不再需要 vite/version.json 的路径
// const viteVersionPath = path.join(__dirname, 'vite', 'version.json'); 
const buildGradlePath = path.join(__dirname, 'vite', 'android', 'app', 'build.gradle');
const outputCdnJsonPath = path.join(__dirname, 'dist', 'cdn-version.json'); // 输出到 dist 目录

console.log('读取配置:', config);

// --- 移除步骤 1: 不再更新 vite/version.json ---
console.log('ℹ️ 跳过更新 vite/version.json (已废弃)');

// --- 步骤 2 (原步骤 2): 更新 android/app/build.gradle ---
// 注意：这是一个基础的正则替换，可能需要根据您的 build.gradle 具体结构调整
try {
    let buildGradleContent = fs.readFileSync(buildGradlePath, 'utf8');
    let updated = false;

    // 替换 versionCode
    const versionCodeRegex = /versionCode\s+\d+/;
    if (versionCodeRegex.test(buildGradleContent)) {
        buildGradleContent = buildGradleContent.replace(versionCodeRegex, `versionCode ${config.nativeVersionCode}`);
        console.log(`✅ 准备更新 ${buildGradlePath} -> versionCode: ${config.nativeVersionCode}`);
        updated = true;
    } else {
        console.warn(`⚠️ 未在 ${buildGradlePath} 中找到 versionCode 行进行替换。`);
    }

    // 替换 versionName
    const versionNameRegex = /versionName\s+"[^"]+"/;
    if (versionNameRegex.test(buildGradleContent)) {
        buildGradleContent = buildGradleContent.replace(versionNameRegex, `versionName "${config.nativeVersionName}"`);
        console.log(`✅ 准备更新 ${buildGradlePath} -> versionName: "${config.nativeVersionName}"`);
        updated = true;
    } else {
        console.warn(`⚠️ 未在 ${buildGradlePath} 中找到 versionName 行进行替换。`);
    }

    if (updated) {
        fs.writeFileSync(buildGradlePath, buildGradleContent, 'utf8');
        console.log(`✅ 成功写入更新到 ${buildGradlePath}`);
    } else {
         console.log(`ℹ️ ${buildGradlePath} 无需更新。`);
    }

} catch (error) {
    console.error(`❌ 更新 ${buildGradlePath} 失败:`, error);
    console.error('   请检查文件路径是否正确以及脚本是否有写入权限。');
    console.error('   您可能需要手动更新 build.gradle 文件。');
}

// --- 3. 生成 CDN version.json 内容 ---
const cdnJsonContent = {
    web: {
        version: config.webVersion,
        releaseNotes: config.webReleaseNotes || ""
    },
    native: {
        android: {
            version: config.nativeVersionName,
            versionCode: config.nativeVersionCode,
            apkUrl: config.getApkUrl(), // 使用辅助函数生成 URL
            releaseNotes: config.nativeReleaseNotes || ""
        },
    },
    lastUpdated: new Date().toISOString() // 添加更新时间戳
};

try {
    // 确保输出目录存在
    const outputDir = path.dirname(outputCdnJsonPath);
    if (!fs.existsSync(outputDir)){
        fs.mkdirSync(outputDir, { recursive: true });
        console.log(`✅ 创建输出目录 ${outputDir}`);
    }

    fs.writeFileSync(outputCdnJsonPath, JSON.stringify(cdnJsonContent, null, 2), 'utf8');
    console.log(`✅ 成功生成 CDN JSON 文件到 ${outputCdnJsonPath}`);
    console.log('   >>> 请将此文件的内容上传或部署到 CDN 的:', config.cdnVersionJsonPath);
} catch (error) {
    console.error(`❌ 生成 CDN JSON 文件 (${outputCdnJsonPath}) 失败:`, error);
}

console.log('\n版本更新脚本执行完毕。');
