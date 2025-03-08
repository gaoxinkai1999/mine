// update.js
import { showConfirmDialog, showLoadingToast, showFailToast, showSuccessToast } from "vant";
import 'vant/es/toast/style';
import 'vant/es/dialog/style';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { FileOpener } from '@capacitor-community/file-opener';

import { ref } from 'vue';
import APP_VERSION from '@/../version.json'
/**
 * 应用更新组合式函数
 * 提供检查更新和下载APK的功能
 */
export function useAppUpdate() {
  const isChecking = ref(false);

  /**
   * 检查更新
   */
  async function checkForUpdates() {
    if (isChecking.value) return;



    isChecking.value = true;
    try {
      const currentVersion = APP_VERSION.version;
      console.log('当前版本:', JSON.stringify(currentVersion));

      // 获取version.json的信息
      const versionInfo = await fetchVersionInfo();
      console.log('最新版本信息:', JSON.stringify(versionInfo, null, 2));

      // 检查版本号是否最新
      const isLatestVersion = compareVersions(currentVersion, versionInfo.version) >= 0;
      if (isLatestVersion) {
        console.log('当前版本已是最新，无需更新');
        return false;
      }

      // 下载并安装APK
      if (versionInfo.apkUrl) {
        console.log('需要下载APK更新');
        await downloadAndInstallApk(versionInfo.apkUrl);
        return true;
      } else {
        console.log('没有可用的APK下载链接');
        return false;
      }
    } catch (error) {
      console.error('检查更新失败:', error);
      showFailToast('检查更新失败');
      return false;
    } finally {
      isChecking.value = false;
    }
  }

  /**
   * 获取版本信息
   */
  async function fetchVersionInfo() {
    try {
      // 示例：使用时间戳防止缓存版本信息
      const url = `http://update.abocidee.com/updates/version.json?t=${Date.now()}`;
      const response = await fetch(url);
      return await response.json();
    } catch (error) {
      console.error('获取版本信息失败:', error);
      throw new Error('无法获取版本信息');
    }
  }

  /**
   * 版本号比较方法
   */
  function compareVersions(version1, version2) {
    const v1 = version1.split('.').map(Number);
    const v2 = version2.split('.').map(Number);

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
   */
  async function downloadAndInstallApk(apkUrl) {
    try {
      showLoadingToast({
        message: '准备下载...',
        forbidClick: true,
        loadingType: 'spinner',
      });

      // 生成临时文件名
      const fileName = `app-update-${Date.now()}.apk`;
      
      // 显示下载进度提示
      showLoadingToast({
        message: '正在下载新版本...',
        forbidClick: true,
        loadingType: 'spinner',
      });
      
      // 使用 Filesystem.downloadFile 直接下载 APK 文件
      const downloadResult = await new Promise((resolve, reject) => {
        // 设置下载监听器
        const downloadListener = Filesystem.addListener('progress', progressData => {
          const progress = Math.round(progressData.bytes / progressData.contentLength * 100);
          showLoadingToast({
            message: `正在下载新版本...${progress}%`,
            forbidClick: true,
            loadingType: 'spinner',
          });
        });
        
        // 执行下载
        Filesystem.downloadFile({
          url: apkUrl,
          path: fileName,
          directory: Directory.Documents,
          progress: true
        }).then(result => {
          // 下载完成后移除监听器
          downloadListener.remove();
          resolve(result);
        }).catch(error => {
          // 下载失败移除监听器
          downloadListener.remove();
          reject(error);
        });
      });
      
      console.log('下载完成:', downloadResult);
      
      // 获取文件的完整路径
      const fileInfo = await Filesystem.getUri({
        directory: Directory.Documents,
        path: fileName
      });
      
      console.log('文件路径信息:', fileInfo);
      
      // 打开 APK 进行安装
      await openApk(fileInfo.uri, fileName);
      
    } catch (error) {
      console.error('下载或安装APK失败:', error);
      console.log('详细错误信息:', error.message);
      if (error.message.includes('HTTP error') || error.message.includes('download')) {
        showFailToast('下载失败：无法连接到服务器');
      } else if (error.message.includes('write') || error.message.includes('storage')) {
        showFailToast('保存文件失败：存储空间不足');
      } else if (error.message.includes('open') || error.message.includes('activity')) {
        showFailToast('打开安装程序失败，请检查是否允许安装未知来源应用');
      } else {
        showFailToast(`更新失败：${error.message}`);
      }
      
      // 打印更详细的错误信息以便调试
      console.error('完整错误对象:', JSON.stringify(error, null, 2));
    }
  }

  /**
   * 打开 APK 文件进行安装
   */
  async function openApk(filePath, fileName) {
    try {
      showLoadingToast({
        message: '正在打开安装程序...',
        forbidClick: true,
        loadingType: 'spinner',
      });

      // 尝试打开APK进行安装
      await FileOpener.open({
        filePath: filePath,
        contentType: 'application/vnd.android.package-archive',
        openWithDefault: true
      });

      showSuccessToast('请按照提示安装新版本');
    } catch (openError) {
      showFailToast('打开安装程序失败，请检查是否允许安装未知来源应用')
    }
  }

  /**
   * 辅助方法：将 ArrayBuffer 转换为 Base64
   */
  function arrayBufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
  }

  return {
    isChecking,
    downloadAndInstallApk,
    checkForUpdates,
  };
}
