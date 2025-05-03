package com.example.service;

import com.example.dto.LatestVersionsDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UpdateService {

    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);
    private static final long SSE_EMITTER_TIMEOUT = 60_000L; // 60 秒 SSE 连接超时
    private static final String CDN_VERSION_URL = "/version.json"; // CDN 文件路径
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final WebClient webClient;

    // 使用 AtomicReference 来原子地更新缓存的整个版本信息 Map
    private final AtomicReference<Map<String, Object>> cachedVersionInfo = new AtomicReference<>(null);
    // 用于存储当前已知的最新Web版本，避免重复推送
    private volatile String currentWebVersion = null;

    // 构造函数注入 WebClient Bean
    public UpdateService(WebClient.Builder webClientBuilder) {
        // 注意：baseUrl 设置在这里，后续请求使用相对路径
        this.webClient = webClientBuilder.baseUrl("https://cdn.abocidee.com/demo").build();
    }

    @PostConstruct
    private void initializeCurrentWebVersion() {
        // 应用启动时尝试从 CDN 加载一次初始版本信息
        fetchLatestVersionInfoFromCDN().subscribe(
                versionInfo -> {
                    this.currentWebVersion = getNestedString(versionInfo, "web", "version");
                    logger.info("从 CDN 初始化 Web 版本: {}", this.currentWebVersion);
                },
                error -> logger.error("启动时从 CDN 初始化版本信息失败", error)
        );
    }

    /**
     * 创建并注册一个新的 SSE Emitter。
     * 立即发送当前已知的最新 Web 版本给新连接的客户端。
     * @return 配置好的 SseEmitter 实例
     */
    public SseEmitter createSseEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        this.emitters.add(emitter);
        logger.info("添加了新的 SSE emitter。总数: {}", emitters.size());

        emitter.onCompletion(() -> {
            logger.info("SSE emitter 完成。");
            this.emitters.remove(emitter);
            logger.info("完成时移除 emitter。总数: {}", emitters.size());
        });
        emitter.onError(e -> {
            logger.error("SSE emitter 错误: {}", e.getMessage());
            this.emitters.remove(emitter);
            logger.info("出错时移除 emitter。总数: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            logger.info("SSE emitter 超时。");
            this.emitters.remove(emitter);
            logger.info("超时时移除 emitter。总数: {}", emitters.size());
        });

        // 立即发送当前版本给新客户端
        sendInitialWebVersion(emitter);

        return emitter;
    }

    /**
     * 尝试向新连接的 Emitter 发送当前已知的 Web 版本。
     * 如果当前版本未知，则尝试从 CDN 加载一次。
     * @param emitter 新的 SseEmitter
     */
    private void sendInitialWebVersion(SseEmitter emitter) {
        String versionToSend = this.currentWebVersion;
        if (versionToSend != null) {
            try {
                emitter.send(SseEmitter.event().name("web-version").data(versionToSend));
                logger.debug("已发送初始 Web 版本 {} 给新的 emitter", versionToSend);
            } catch (IOException e) {
                logger.error("发送初始 Web 版本给新 emitter 时出错: {}", e.getMessage());
                this.emitters.remove(emitter); // 发送失败也移除
            }
        } else {
            logger.warn("当前 Web 版本未知，尝试从 CDN 获取后发送...");
            // 异步获取，获取到后发送
            fetchLatestVersionInfoFromCDN().subscribe(
                versionInfo -> {
                    String latestWebVersion = getNestedString(versionInfo, "web", "version");
                    if (latestWebVersion != null) {
                        this.currentWebVersion = latestWebVersion; // 更新内部状态
                        try {
                            // 再次检查 emitter 是否仍然有效
                            if (this.emitters.contains(emitter)) {
                                emitter.send(SseEmitter.event().name("web-version").data(latestWebVersion));
                                logger.info("获取后发送了初始 Web 版本 {} 给新的 emitter", latestWebVersion);
                            } else {
                                logger.warn("获取版本后，emitter 已被移除，无法发送初始版本。");
                            }
                        } catch (IOException e) {
                            logger.error("获取版本后，发送初始 Web 版本给新 emitter 时出错: {}", e.getMessage());
                            this.emitters.remove(emitter);
                        }
                    } else {
                         logger.error("从 CDN 获取的版本信息中缺少 Web 版本。");
                    }
                },
                error -> logger.error("尝试为新 emitter 获取初始 Web 版本失败", error)
            );
        }
    }


    /**
     * 获取最新的 Web 和 Native 版本信息 (从缓存或 CDN)。
     * 注意：此方法现在返回 Mono 以支持异步操作。Controller 也需要调整。
     * @return 包含版本信息的 DTO 的 Mono
     */
    public Mono<LatestVersionsDto> getLatestVersions() {
        return fetchLatestVersionInfoFromCDN().map(versionInfo -> {
            // Extract versions
            String webVersion = getNestedString(versionInfo, "web", "version");
            String nativeVersion = getNestedString(versionInfo, "native", "android", "version");
            String apkUrl = getNestedString(versionInfo, "native", "android", "apkUrl");
            
            // Extract release notes safely
            String webReleaseNotes = getNestedString(versionInfo, "web", "releaseNotes");
            String nativeReleaseNotes = getNestedString(versionInfo, "native", "android", "releaseNotes");

            // 更新内部记录的 Web 版本，如果它更新了
            if (webVersion != null && !webVersion.equals(this.currentWebVersion)) {
                 logger.info("通过 /latest-versions 接口发现 Web 版本更新: {} -> {}", this.currentWebVersion, webVersion);
                 this.currentWebVersion = webVersion;
                 // 注意：这里不广播，广播由定时任务负责
            }
            // Populate DTO with all fields, providing defaults for null notes
            return new LatestVersionsDto(
                webVersion, 
                webReleaseNotes != null ? webReleaseNotes : "", 
                nativeVersion, 
                nativeReleaseNotes != null ? nativeReleaseNotes : "", 
                apkUrl
            );
        }).defaultIfEmpty(new LatestVersionsDto("N/A", "", "N/A", "", null)); // 如果 CDN 请求失败或为空，返回包含空字符串注释的默认值
    }

    /**
     * 从 CDN 获取最新的统一版本信息。包含缓存逻辑。
     * @return 包含所有版本信息的 Map 的 Mono，失败时返回 Mono.empty()。
     */
    @SuppressWarnings("unchecked") // 压制 bodyToMono(Map.class) 可能的未检查转换警告
    private Mono<Map<String, Object>> fetchLatestVersionInfoFromCDN() {
        // 优先返回缓存
        Map<String, Object> cachedData = cachedVersionInfo.get();
        if (cachedData != null) {
            logger.debug("返回缓存的版本信息。");
            return Mono.just(cachedData);
        }

        logger.debug("正在从 CDN 获取统一版本信息...");
        // 添加时间戳防止缓存
        String url = CDN_VERSION_URL + "?t=" + System.currentTimeMillis();
        return webClient.get()
                .uri(url)
                .retrieve()
                // 使用 Map.class，假设 JSON 结构符合 Map<String, Object>
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map) // 进行类型转换
                .doOnSuccess(versionInfo -> {
                    if (versionInfo != null) {
                        logger.info("获取到的统一版本信息: {}", versionInfo);
                        // 原子地更新缓存
                        cachedVersionInfo.set(versionInfo);
                    } else {
                        logger.warn("从 CDN 获取到的版本信息为 null。");
                    }
                })
                .doOnError(WebClientResponseException.class, e ->
                    logger.error("从 CDN 获取版本信息失败 - HTTP Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString())
                )
                .doOnError(e -> !(e instanceof WebClientResponseException), e ->
                    logger.error("从 CDN 获取版本信息时发生非 HTTP 错误", e)
                )
                .onErrorResume(e -> {
                    // 发生错误时清除缓存并返回空 Mono
                    cachedVersionInfo.set(null);
                    return Mono.empty();
                });
    }

    /**
     * 定时任务，每分钟检查一次 CDN 上的 Web 版本是否有更新。
     * 如果有更新，则向所有连接的 SSE 客户端广播新版本。
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒执行一次
    public void checkAndBroadcastWebVersionUpdate() {
        logger.debug("定时检查 CDN Web 版本更新...");
        // 强制清除缓存，从 CDN 获取最新数据
        cachedVersionInfo.set(null);
        fetchLatestVersionInfoFromCDN().subscribe(
            versionInfo -> {
                String latestWebVersion = getNestedString(versionInfo, "web", "version");
                if (latestWebVersion != null && !latestWebVersion.equals(this.currentWebVersion)) {
                    logger.info("CDN Web 版本从 {} 变为 {}。正在广播更新。", this.currentWebVersion, latestWebVersion);
                    this.currentWebVersion = latestWebVersion; // 更新当前版本记录
                    broadcastWebVersionUpdate(latestWebVersion);
                } else if (latestWebVersion == null) {
                     logger.warn("定时任务无法从 CDN 获取有效的 Web 版本号。");
                } else {
                    logger.debug("CDN Web 版本未改变 ({})。", this.currentWebVersion);
                }
            },
            error -> logger.error("定时任务检查 CDN 版本更新失败", error)
        );
    }


    /**
     * 向所有活动的 SSE Emitter 广播 Web 版本更新。
     * @param newVersion 最新的 Web 版本号
     */
    private void broadcastWebVersionUpdate(String newVersion) {
        if (emitters.isEmpty()) {
             logger.debug("没有活动的 SSE emitter 可供广播。");
             return;
        }
        if (newVersion == null) {
             logger.warn("尝试广播的 Web 版本为 null，已跳过。");
             return;
        }

        logger.debug("正在向 {} 个 emitter 广播 Web 版本更新 '{}'。", emitters.size(), newVersion);
        SseEmitter.SseEventBuilder event = SseEmitter.event().name("web-version").data(newVersion);
        // 使用迭代器或列表副本以允许在迭代中安全删除
        for (SseEmitter emitter : new CopyOnWriteArrayList<>(emitters)) {
            try {
                emitter.send(event);
                logger.trace("已发送更新给 emitter。");
            } catch (IOException e) {
                logger.error("发送更新给 emitter 时出错: {}。正在移除 emitter。", e.getMessage());
                this.emitters.remove(emitter); // 发送失败时移除
            } catch (Exception ex) {
                 logger.error("发送更新给 emitter 时发生意外错误: {}。正在移除 emitter。", ex.getMessage(), ex);
                 this.emitters.remove(emitter); // 其他异常也移除
            }
        }
         logger.debug("完成广播 Web 版本更新。");
    }

    /**
     * 安全地从嵌套 Map 结构中获取 String 值。
     * @param map 顶层 Map
     * @param keys 嵌套的键序列
     * @return 找到的 String 值，或 null
     */
    private String getNestedString(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        Object value = map;
        for (String key : keys) {
            if (!(value instanceof Map)) return null;
            // 使用 getOrDefault 避免空指针，虽然理论上上层检查已覆盖
            value = ((Map<?, ?>) value).get(key);
            if (value == null) return null;
        }
        // 最终值检查是否为 String
        return (value instanceof String) ? (String) value : null;
    }
}
