// update.js - 简化版：只包含原生 APK 下载/安装和版本比较逻辑
import { showLoadingToast, showFailToast, showSuccessToast } from "vant";
import 'vant/es/toast/style';
import 'vant/es/dialog/style';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { FileOpener } from '@capacitor-community/file-opener';

// 不再需要 ref 和本地 version.json
// import { ref } from 'vue';
// import APP_VERSION from '@/../version.json' 

/**
 * 版本号比较方法
 * @param {string} version1
 * @param {string} version2
 * @returns {-1 | 0 | 1} -1 if v1 < v2, 0 if v1 === v2, 1 if v1 > v2
 */
function compareVersions(version1, version2) {
  // 确保输入是字符串
  const v1Str = String(version1 || '0');
  const v2Str = String(version2 || '0');

  const v1 = v1Str.split('.').map(Number);
  const v2 = v2Str.split('.').map(Number);

  for (let i = 0; i < Math.max(v1.length, v2.length); i++) {
    const num1 = v1[i] || 0;
    const num2 = v2[i] || 0;
    if (num1 < num2) return -1;
    if (num1 > num2) return 1;
  }
  return 0;
}

/**
 * 下载并安装APK
 * @param {string} apkUrl APK 的下载链接
 */
async function downloadAndInstallApk(apkUrl) {
  let downloadListener = null; // 将监听器引用移到外部以便在 catch 中也能访问
  try {
    showLoadingToast({
      message: '准备下载...',
      forbidClick: true,
      loadingType: 'spinner',
      duration: 0 // 持续显示直到手动关闭
    });

    // 生成临时文件名
    const fileName = `app-update-${Date.now()}.apk`;
    
    // 显示下载进度提示 (初始)
    showLoadingToast({
      message: '正在下载新版本...0%',
      forbidClick: true,
      loadingType: 'spinner',
      duration: 0
    });
    
    // 使用 Filesystem.downloadFile 直接下载 APK 文件
    const downloadResult = await new Promise((resolve, reject) => {
      // 设置下载监听器
      downloadListener = Filesystem.addListener('progress', progressData => {
        // 确保 contentLength 大于 0 避免除零错误
        if (progressData.contentLength && progressData.contentLength > 0) {
          const progress = Math.round(progressData.bytes / progressData.contentLength * 100);
          showLoadingToast({
            message: `正在下载新版本...${progress}%`,
            forbidClick: true,
            loadingType: 'spinner',
            duration: 0
          });
        } else {
           // 如果没有 contentLength，显示一个通用消息
           showLoadingToast({
             message: `正在下载新版本...`,
             forbidClick: true,
             loadingType: 'spinner',
             duration: 0
           });
        }
      });
      
      // 执行下载
      Filesystem.downloadFile({
        url: apkUrl,
        path: fileName,
        directory: Directory.Documents, // 推荐使用 Documents 或 Cache 目录
        progress: true // 启用进度事件
      }).then(result => {
        downloadListener?.remove(); // 安全地移除监听器
        resolve(result);
      }).catch(error => {
        downloadListener?.remove(); // 安全地移除监听器
        reject(error);
      });
    });
    
    console.log('下载完成:', downloadResult);
    showLoadingToast({ message: '下载完成，准备安装...', duration: 1000 }); // 短暂提示
    
    // 获取文件的完整路径 (使用 downloadResult 中的 uri)
    const fileUri = downloadResult.uri; 
    if (!fileUri) {
        throw new Error('下载结果中未找到文件 URI');
    }
    console.log('文件 URI:', fileUri);
    
    // 打开 APK 进行安装
    await openApk(fileUri); // 直接传递 URI
    
  } catch (error) {
    console.error('下载或安装APK失败:', error);
    showFailToast('下载或安装失败'); // 提供通用错误提示
    // 可以在这里添加更具体的错误日志或用户反馈
    // 例如：检查网络连接、存储权限、未知来源应用安装权限等
  } finally {
     // 确保 Toast 关闭
     showLoadingToast({ message: '', duration: 1 }); 
  }
}

/**
 * 打开 APK 文件进行安装
 * @param {string} fileUri 文件的 URI (来自 Filesystem.downloadFile 或 getUri)
 */
async function openApk(fileUri) {
  try {
    showLoadingToast({
      message: '正在打开安装程序...',
      forbidClick: true,
      loadingType: 'spinner',
      duration: 0
    });

    // 尝试打开APK进行安装
    await FileOpener.open({
      filePath: fileUri, // 使用 URI
      contentType: 'application/vnd.android.package-archive',
      openWithDefault: true
    });

    // 关闭 loading toast，因为安装界面会接管
    showLoadingToast({ message: '', duration: 1 }); 
    // showSuccessToast('请按照提示安装新版本'); // 这个提示可能被安装界面覆盖

  } catch (openError) {
    console.error('打开安装程序失败:', openError);
    showFailToast('打开安装程序失败，请检查权限');
    showLoadingToast({ message: '', duration: 1 }); // 确保关闭 loading
  }
}

/**
 * 应用更新组合式函数 (简化版)
 * 提供下载APK和版本比较的功能
 */
export function useAppUpdate() {
  // 不再需要 isChecking 状态，因为检查逻辑已移出
  // const isChecking = ref(false);

  // 移除 checkForUpdates 和 fetchVersionInfo 函数

  // 只导出需要被外部使用的函数
  return {
    downloadAndInstallApk,
    compareVersions 
    // isChecking, // 不再需要导出
    // checkForUpdates, // 已移除
  };
}
