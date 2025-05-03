package com.example.interfaces.web;

import com.example.dto.LatestVersionsDto;
import com.example.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono; // 导入 Mono

@RestController
@RequestMapping("/updates") // 定义基础路径
public class UpdateController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);
    private final UpdateService updateService;

    // 通过构造函数注入 UpdateService
    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    /**
     * SSE 端点，用于客户端订阅版本更新事件。
     * @return SseEmitter 实例
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseEvents() {
        logger.info("收到 SSE 事件流请求。");
        try {
            return updateService.createSseEmitter();
        } catch (Exception e) {
             logger.error("创建 SSE emitter 时出错", e);
             // 可以返回一个立即完成的 emitter 或抛出异常，具体取决于错误处理策略
             SseEmitter errorEmitter = new SseEmitter();
             errorEmitter.completeWithError(e);
             return errorEmitter;
        }
    }

    /**
     * 获取最新的 Web 和 Native 版本信息。
     * @return 包含版本信息的 DTO 的 Mono
     */
    @GetMapping("/latest-versions")
    public Mono<LatestVersionsDto> getLatestVersions() { // 修改返回类型为 Mono<LatestVersionsDto>
        logger.info("收到获取最新版本的请求。");
        return updateService.getLatestVersions(); // 直接返回 Mono
    }
}
