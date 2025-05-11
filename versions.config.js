// versions.config.js
// =============================================
//  !! 这是管理所有版本号的唯一真实来源 !!
//  !! 修改版本号时，请只修改此文件 !!
// =============================================

module.exports = {
  // --- Web 相关 ---
  webVersion: "1.3.8", // 当前最新的 Web 版本 (发布新 Web 版本时修改这里)

  // --- Native (Android) 相关 ---
  nativeVersionName: "1.4.5", // 当前最新的 Native 版本名 (发布新 Native 版本时修改这里)
  nativeVersionCode: 10405,      // 当前最新的 Native 版本代码 (发布新 Native 版本时修改这里)

  // --- CDN 和 APK 信息 ---
  cdnBaseUrl: "https://cdn.abocidee.com", // 您的 CDN 基础 URL
  // APK 文件名模式，{versionName} 会被替换
  apkFileNamePattern: "app-v{versionName}.apk", 
  // CDN 上 version.json 的路径 (相对于 cdnBaseUrl)
  cdnVersionJsonPath: "/version.json", 

  // --- (可选) 更新说明 ---
  // 每次发布时可以更新这些说明
  webReleaseNotes: "新增了非批次管理商品转化为批次管理商品的功能",
  nativeReleaseNotes: "提升了稳定性。\n优化了用户体验。"
};

// Helper function to generate full APK URL
module.exports.getApkUrl = function() {
    const fileName = this.apkFileNamePattern.replace('{versionName}', this.nativeVersionName);
    return `${this.cdnBaseUrl}/${fileName}`; // 假设 APK 在 CDN 根目录
};
