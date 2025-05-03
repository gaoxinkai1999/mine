import { App } from '@capacitor/app';
import { useAppUpdate } from '@/utils/update.js'; // 导入原生更新逻辑
import { showConfirmDialog } from 'vant'; // 导入 Vant 对话框
import 'vant/es/dialog/style'; // 导入 Vant 对话框样式
// 不再需要导入本地 Web 版本信息
// import APP_VERSION_WEB from '@/../version.json'; 

// --- 配置 ---
// 后端 API 基础 URL (根据实际部署情况修改)
// 如果前端和后端部署在同一个域和端口下，可以使用相对路径，否则需要完整 URL
const API_BASE_URL = 'http://192.168.0.102:8085'; // 假设 Spring Boot 运行在 8080
const SSE_URL = `${API_BASE_URL}/api/updates/events`; // 指向 Spring Boot SSE 端点
const LATEST_VERSIONS_URL = `${API_BASE_URL}/api/updates/latest-versions`; // 指向 Spring Boot 版本检查端点

// --- 状态变量 ---
let eventSource = null;
let isAppActive = true; // 初始假设应用是活动的
// 使用 Vite 在构建时注入的全局常量来初始化当前 Web 版本
// 这个变量代表当前运行代码的真实版本，不应在运行时被修改
const BUILT_APP_VERSION = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : null; 
let fetchedLatestWebVersion = null; // 用于存储从服务器获取的最新版本，以便 SSE 事件比较
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
        console.log('用户确认刷新 Web 应用');
        // 注意：简单 reload 可能不足以清除所有缓存，特别是 Service Worker
        // 生产环境可能需要更复杂的缓存清除策略
        window.location.reload(true);
    }).catch(() => {
        // 用户点击取消
        console.log('用户取消刷新 Web 应用');
    });
}

/**
 * 处理 Web 版本检查结果
 * @param {string} latestWebVersion 从服务器获取的最新 Web 版本
 * @param {string} latestWebVersion 从服务器获取的最新 Web 版本
 * @param {string} [releaseNotes] 更新日志 (可选，主要在 checkVersions 时提供)
 */
async function handleWebUpdateCheck(latestWebVersion, releaseNotes) { // 移除默认值 ""
    // 记录从服务器获取的最新版本号 (如果提供了)
    if (latestWebVersion) {
        fetchedLatestWebVersion = latestWebVersion;
    }

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
            console.log('SSE 触发更新，尝试获取更新日志...');
            try {
                const response = await fetch(LATEST_VERSIONS_URL);
                if (response.ok) {
                    const data = await response.json();
                    notesToDisplay = data.webReleaseNotes; // 获取日志
                    console.log('成功获取到更新日志。');
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
        console.log('Web 版本已是最新。');
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
        console.log('未获取到有效的 Native 版本信息或 APK URL。');
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
                 console.log('用户确认下载 Native 更新');
                 // 确保 apkUrl 包含时间戳以防止缓存 (如果后端没加)
                 const finalApkUrl = apkUrl.includes('?') ? `${apkUrl}&t=${Date.now()}` : `${apkUrl}?t=${Date.now()}`;
                 // 注意：downloadAndInstallApk 内部应该有 loading 提示
                 await downloadAndInstallApk(finalApkUrl); 
             }).catch(() => {
                  // 用户点击取消
                 console.log('用户取消下载 Native 更新');
             });
        } else {
            console.log('Native 版本已是最新。');
        }
    } catch (error) {
        console.error('获取当前 Native 应用信息或比较版本时出错:', error);
    }
}


// --- SSE 相关函数 ---

/**
 * 启动 SSE 连接的函数
 */
function startSSE() {
    if (!isAppActive) {
        console.log('应用非活动状态，跳过启动 SSE。');
        return;
    }
    if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
        console.log('SSE 连接已打开或正在连接中。');
        return;
    }

    console.log('尝试连接 SSE 于', SSE_URL);
    eventSource = new EventSource(SSE_URL);

    eventSource.onopen = () => {
        console.log('SSE 连接已打开。');
    };

    // 监听特定事件名 'web-version' (与后端 Service 中 emitter.send 事件名对应)
    eventSource.addEventListener('web-version', (event) => {
        console.log('收到 SSE web-version 事件:', event.data);
        // 注意：SSE 通常只推送版本号，不推送日志。如果需要日志，需在 checkVersions 中获取
        handleWebUpdateCheck(event.data); 
    });

    // 可选：处理默认的 message 事件 (如果后端也发送了没有名字的事件)
    // eventSource.onmessage = (event) => {
    //     console.log('收到 SSE 默认消息:', event.data);
    //     // 根据需要处理，可能也是 Web 版本
    //     handleWebUpdateCheck(event.data);
    // };

    eventSource.onerror = (err) => {
        console.error('SSE 错误:', err);
        eventSource.close(); // 确保在重试前关闭
        // 仅当应用仍然活动时才重试
        if (isAppActive) {
            console.log('SSE 断开，尝试重连...');
            setTimeout(startSSE, 5000); // 5 秒后重试
        } else {
             console.log('SSE 断开，但应用已非活动状态，不重连。');
        }
    };
}

/**
 * 停止 SSE 连接的函数
 */
function stopSSE() {
    if (eventSource) {
        console.log('正在关闭 SSE 连接。');
        eventSource.close();
        eventSource = null;
    }
}

// --- 版本检查函数 ---

/**
 * 执行一次性版本检查 (Web + Native)
 */
async function checkVersions() {
    console.log('执行版本检查 (Web + Native)...');
    try {
        const response = await fetch(LATEST_VERSIONS_URL);
        if (!response.ok) {
            throw new Error(`网络响应不正常: ${response.statusText}`);
        }
        const data = await response.json(); // 解析 { webVersion, webReleaseNotes, nativeVersion, nativeReleaseNotes, apkUrl }
        console.log('从后端获取的版本信息:', data);

        // 处理 Web 版本 (传递日志)
        handleWebUpdateCheck(data.webVersion, data.webReleaseNotes);

        // 处理 Native 版本 (传递日志)
        handleNativeUpdateCheck(data.nativeVersion, data.apkUrl, data.nativeReleaseNotes);

    } catch (error) {
        console.error('执行版本检查时出错:', error);
    } finally {
        // 无论检查结果如何，如果应用是活动的，确保 SSE 连接是启动的
        if (isAppActive && (!eventSource || eventSource.readyState === EventSource.CLOSED)) {
            console.log('版本检查后，确保 SSE 已启动...');
            startSSE();
        }
    }
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
        } else {
            // 进入后台时停止 SSE
            stopSSE();
        }
    });

    // 执行初始检查，如果应用已处于活动状态则启动 SSE
    App.getState().then(state => {
        isAppActive = state.isActive;
        if (isAppActive) {
             console.log('应用初始为活动状态。');
             checkVersions();
             // checkVersions 函数内部会在需要时启动 SSE
        } else {
            console.log('应用初始为非活动状态。');
        }
    });

    console.log('更新服务已初始化。');
}

// 可选：如果其他地方需要，可以导出函数
// export { startSSE, stopSSE, checkVersions };
