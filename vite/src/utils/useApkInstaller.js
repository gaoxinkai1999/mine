import { ref } from 'vue';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Capacitor } from '@capacitor/core';
import { FileOpener } from '@capacitor-community/file-opener';
import { showToast } from 'vant';

export function useApkInstaller() {
    const progress = ref(0);
    const isDownloading = ref(false);
    const error = ref(null);

    // 下载并安装 APK
    async function downloadAndInstallApk(apkUrl, apkName) {
        try {
            // 检查参数
            if (!apkUrl || !apkName) {
                error.value = '无效的APK URL或名称';
                showToast({ message: '❌ 无效的APK参数', duration: 2000 });
                return;
            }

            // 检查平台
            if (Capacitor.getPlatform() !== 'android') {
                error.value = '当前平台不支持 APK 安装';
                showToast({ message: '❌ 当前平台不支持安装', duration: 2000 });
                return;
            }

            isDownloading.value = true;
            progress.value = 0;
            error.value = null;

            showToast({ message: '开始下载 APK...', duration: 1000 });

            // 设置进度监听
            let progressListener;
            progressListener = await Filesystem.addListener('progress', (progressData) => {
                try {
                    console.log('进度事件数据:', JSON.stringify(progressData));

                    let percent = 0;
                    // 处理不同结构的事件数据
                    const bytes = progressData.bytes || progressData.loaded;
                    const contentLength = progressData.contentLength || progressData.total;

                    if (bytes !== undefined && contentLength !== undefined) {
                        percent = Math.round((bytes / contentLength) * 100);
                    } else if (progressData.progress !== undefined) {
                        percent = Math.round(progressData.progress * 100);
                    }

                    percent = Math.min(Math.max(percent, 0), 100);
                    progress.value = percent;

                    if (percent % 20 === 0 || percent === 100) {
                        showToast({ message: `下载进度：${percent}%`, duration: 500 });
                    }
                } catch (err) {
                    console.error('处理进度事件出错:', err);
                }
            });

            // 确保文件名有正确的后缀
            const fileName = apkName.endsWith('.apk') ? apkName : `${apkName}.apk`;

            // 开始下载 APK
            console.log('开始下载APK:', apkUrl);
            const result = await Filesystem.downloadFile({
                path: fileName,
                url: apkUrl,
                directory: Directory.External,
                progress: true, // 启用进度事件
            });

            // 下载完成后移除监听器
            console.log('下载完成，准备移除监听器');
            if (progressListener) {
                try {
                    await progressListener.remove();
                    console.log('进度监听器已移除');
                } catch (err) {
                    console.warn('移除进度监听器失败:', err);
                }
            }

            showToast({ message: '✅ APK 下载完成', duration: 1000 });
            isDownloading.value = false;

            // 检查下载结果
            if (!result || !result.path) {
                throw new Error('下载结果无效');
            }

            // 安装 APK
            await installApk(result.path);
        } catch (e) {
            console.error('❌ APK 下载或安装失败:', e);
            error.value = e.message || '下载或安装失败';
            showToast({ message: `❌ 下载失败: ${e.message || '请重试'}`, duration: 2000 });
            isDownloading.value = false;
        }
    }

    // 使用 FileOpener 插件打开 APK 文件进行安装
    async function installApk(apkPath) {
        try {
            if (!apkPath) {
                throw new Error('APK路径无效');
            }

            if (Capacitor.getPlatform() === 'android') {
                await FileOpener.open({
                    filePath: apkPath,
                    contentType: 'application/vnd.android.package-archive',
                });
                showToast({ message: '✅ 启动 APK 安装...', duration: 1000 });
            } else {
                throw new Error('当前平台不支持 APK 安装');
            }
        } catch (e) {
            console.error('❌ 安装 APK 失败:', e);
            error.value = e.message || '安装失败';
            showToast({ message: `❌ 安装失败: ${e.message || ''}`, duration: 2000 });
            throw e; // 向上抛出异常以便调用方处理
        }
    }

    // 取消下载
    async function cancelDownload() {
        try {
            // 当前 Capacitor API 可能不直接支持取消下载，可以考虑以下替代方法：
            // 1. 将 isDownloading 设置为 false
            isDownloading.value = false;
            // 2. 重置进度和错误信息
            progress.value = 0;
            error.value = '下载已取消';
            showToast({ message: '下载已取消', duration: 1000 });
            // 注意：这只是UI上的取消，实际下载任务可能仍在后台继续
        } catch (e) {
            console.error('取消下载失败:', e);
            error.value = '取消下载失败';
        }
    }

    return {
        downloadAndInstallApk,
        installApk,
        cancelDownload,
        progress,
        isDownloading,
        error,
    };
}