import { App } from '@capacitor/app';
import { useAppUpdate } from '@/utils/update.js'; // 导入原生更新逻辑
import { showConfirmDialog } from 'vant'; // 导入 Vant 对话框
import 'vant/es/dialog/style'; // 导入 Vant 对话框样式
// 不再需要导入本地 Web 版本信息
// import APP_VERSION_WEB from '@/../version.json'; 

// --- 配置 ---
// 后端 API 基础 URL (根据实际部署情况修改)
const CDN_VERSION_URL = 'https://cdn.abocidee.com/version.json'; // 指向 CDN 上的版本文件

// --- 状态变量 ---
let isAppActive = true; // 初始假设应用是活动的
// 使用 Vite 在构建时注入的全局常量来初始化当前 Web 版本
// 这个变量代表当前运行代码的真实版本，不应在运行时被修改
const BUILT_APP_VERSION = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : null; 
const { downloadAndInstallApk, compareVersions } = useAppUpdate(); // 获取原生下载安装函数和版本比较函数 (假设 compareVersions 在 useAppUpdate 中)

// --- 辅助函数 ---

/**
 * 提示用户重新加载以应用 Web 更新
 * @param {string} notes 更新日志
 */
function promptWebReload(notes = "") {
    // 检查 notes 是否为 null, undefined, 或空字符串/纯空格
    const displayNotes = (notes && notes.trim()) ? notes : '无'; 
    showConfirmDialog({
        title: 'Web 更新提示',
        message: `发现新的 Web 版本，建议立即刷新应用以获取最新体验。\n\n更新内容:\n${displayNotes}`,
        confirmButtonText: '立即刷新',
        cancelButtonText: '稍后提醒',
    }).then(() => {
        // 用户点击确认
        // 注意：简单 reload 可能不足以清除所有缓存，特别是 Service Worker
        // 生产环境可能需要更复杂的缓存清除策略
        window.location.reload(true);
    }).catch(() => {
        // 用户点击取消
    });
}

/**
 * 处理 Web 版本检查结果
 * @param {string} latestWebVersion 从服务器获取的最新 Web 版本
 * @param {string} latestWebVersion 从服务器获取的最新 Web 版本
 * @param {string} [releaseNotes] 更新日志 (可选，主要在 checkVersions 时提供)
 */
async function handleWebUpdateCheck(latestWebVersion, releaseNotes) { // 移除默认值 ""
    // 不再需要存储 fetchedLatestWebVersion

    if (BUILT_APP_VERSION === null) {
         console.error("错误：当前运行的 Web 版本未知！检查 vite.config.js define 配置。");
         return;
    }

    console.log(`检查 Web 版本: 最新 ${latestWebVersion}, 当前运行 ${BUILT_APP_VERSION}`);
    
    // 始终与构建时的版本比较
    if (latestWebVersion && latestWebVersion !== BUILT_APP_VERSION) {
        console.log('发现新的 Web 版本!');
        
        // 如果没有传入 releaseNotes (很可能是 SSE 触发的)，则去获取
        let notesToDisplay = releaseNotes;
        if (!notesToDisplay) {
            try {
                // 从 CDN 获取版本信息以获取日志
                const urlWithTimestamp = `${CDN_VERSION_URL}?t=${Date.now()}`;
                const response = await fetch(urlWithTimestamp);
                if (response.ok) {
                    const data = await response.json();
                    notesToDisplay = data.webReleaseNotes; // 获取日志
                } else {
                     console.warn('获取更新日志失败，状态码:', response.status);
                }
            } catch (error) {
                console.error('获取更新日志时出错:', error);
            }
        }
        
        // 不再修改 currentWebVersion 状态变量
        promptWebReload(notesToDisplay); // 传递获取到的或传入的更新日志
    } else if (latestWebVersion && latestWebVersion === BUILT_APP_VERSION) {
    } else if (!latestWebVersion) {
        console.warn('用于检查的最新 Web 版本无效。');
    }
}

/**
 * 处理 Native 版本检查结果
 * @param {string} latestNativeVersion 从服务器获取的最新 Native 版本
 * @param {string|null} apkUrl APK 下载链接
 * @param {string} releaseNotes 更新日志
 */
async function handleNativeUpdateCheck(latestNativeVersion, apkUrl, releaseNotes = "") {
    if (!latestNativeVersion || !apkUrl) {
        return;
    }

    try {
        // 获取当前安装的原生应用版本
        const appInfo = await App.getInfo();
        const currentNativeVersion = appInfo.version;
        console.log(`检查 Native 版本: 最新 ${latestNativeVersion}, 当前 ${currentNativeVersion}`);

        // 版本比较 (优先使用导入的 compareVersions)
        const comparison = compareVersions
            ? compareVersions(currentNativeVersion, latestNativeVersion)
            : (currentNativeVersion < latestNativeVersion ? -1 : (currentNativeVersion === latestNativeVersion ? 0 : 1)); // 备用简单比较

        if (comparison < 0) {
             console.log('发现新的 Native 版本!');
             // 检查 releaseNotes 是否为 null, undefined, 或空字符串/纯空格
             const displayNotes = (releaseNotes && releaseNotes.trim()) ? releaseNotes : '无';
             showConfirmDialog({
                 title: `发现新版本 ${latestNativeVersion}`,
                 message: `建议立即更新以获取最新功能和改进。\n\n更新内容:\n${displayNotes}`,
                 confirmButtonText: '立即下载',
                 cancelButtonText: '稍后提醒',
             }).then(async () => {
                 // 用户点击确认
                 // 确保 apkUrl 包含时间戳以防止缓存 (如果后端没加)
                 const finalApkUrl = apkUrl.includes('?') ? `${apkUrl}&t=${Date.now()}` : `${apkUrl}?t=${Date.now()}`;
                 // 注意：downloadAndInstallApk 内部应该有 loading 提示
                 await downloadAndInstallApk(finalApkUrl); 
             }).catch(() => {
                  // 用户点击取消
             });
        } else {
            console.log('Native 版本已是最新。');
        }
    } catch (error) {
        console.error('获取当前 Native 应用信息或比较版本时出错:', error);
    }
}


// --- SSE 相关函数 (已移除) ---

// --- 版本检查函数 ---

/**
 * 执行一次性版本检查 (Web + Native)
 */
async function checkVersions() {
    // console.log('执行版本检查 (Web + Native)...');
    try {
        // 添加时间戳以防止 CDN 缓存旧文件
        const urlWithTimestamp = `${CDN_VERSION_URL}?t=${Date.now()}`;
        const response = await fetch(urlWithTimestamp);
        if (!response.ok) {
            throw new Error(`获取 CDN 版本文件失败: ${response.statusText}`);
        }
        const data = await response.json(); // 解析 { webVersion, webReleaseNotes, nativeVersion, nativeReleaseNotes, apkUrl }
        // console.log('从后端获取的版本信息:', data);

        // 处理 Web 版本 (传递日志) - 使用正确的嵌套路径
        handleWebUpdateCheck(data.web?.version, data.web?.releaseNotes);

        // 处理 Native 版本 (传递日志)
        // 处理 Native 版本 (传递日志) - 使用正确的嵌套路径
        handleNativeUpdateCheck(data.native?.android?.version, data.native?.android?.apkUrl, data.native?.android?.releaseNotes);

    } catch (error) {
        console.error('执行版本检查时出错:', error);
    }
    // 移除 finally 块中的 SSE 启动逻辑
}

// --- 服务初始化 ---

/**
 * 初始化服务
 */
export function initializeUpdateService() {
    // 监听 Capacitor 应用状态变化
    App.addListener('appStateChange', ({ isActive }) => {
        console.log(`应用状态改变: ${isActive ? '活动' : '非活动'}`);
        isAppActive = isActive;
        if (isActive) {
            // 在变为活动状态时立即执行检查
            checkVersions();
            // checkVersions 函数内部会在需要时启动 SSE
        }
        // 进入后台时不再需要停止 SSE
    });

    // 执行初始检查，如果应用已处于活动状态则启动 SSE
    App.getState().then(state => {
        isAppActive = state.isActive;
        if (isAppActive) {
             // console.log('应用初始为活动状态。');
             checkVersions(); // 只需执行检查
        } else {
            // console.log('应用初始为非活动状态。');
        }
    });

    // console.log('更新服务已初始化。');
}

// 可选：如果其他地方需要，可以导出函数
// export { startSSE, stopSSE, checkVersions };
