package com.tlc.live.risk.orchestrator;

import com.tlc.live.common.exception.RiskRejectException;
import com.tlc.live.risk.check.BlacklistCheckService;
import com.tlc.live.risk.check.CreditScoreCheckService;
import com.tlc.live.risk.check.ParticipationDedupCheckService;
import com.tlc.live.risk.config.RiskProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

/**
 * 风控短路查杀编排器（JDK 25 结构化并发，JEP 505 第五次预览）。
 *
 * <p>三项校验（设备指纹 / 信誉分 / 重复参与）并发执行：
 * 任一校验抛出 {@link RiskRejectException} 时，{@code Joiner.allSuccessfulOrThrow()}
 * 会立即取消并销毁其余仍在 I/O 等待中的子任务——这就是"物理级别的并发短路"，
 * 黑产命中后不再为指纹库和信誉分的慢响应买单；整体还有 50ms 硬窗口兜底。
 */
@Service
public class RiskCheckOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RiskCheckOrchestrator.class);

    private final BlacklistCheckService blacklistCheck;
    private final CreditScoreCheckService creditCheck;
    private final ParticipationDedupCheckService dedupCheck;
    private final RiskProperties props;

    public RiskCheckOrchestrator(BlacklistCheckService blacklistCheck,
                                 CreditScoreCheckService creditCheck,
                                 ParticipationDedupCheckService dedupCheck,
                                 RiskProperties props) {
        this.blacklistCheck = blacklistCheck;
        this.creditCheck = creditCheck;
        this.dedupCheck = dedupCheck;
        this.props = props;
    }

    public RiskCheckResult check(RiskCheckRequest request) {
        long start = System.nanoTime();
        Duration window = Duration.ofMillis(props.getTimeoutMs());
        try (var scope = StructuredTaskScope.open(
                Joiner.<Boolean>allSuccessfulOrThrow(),
                config -> config.withTimeout(window))) {
            scope.fork(() -> {
                blacklistCheck.check(request.userId());
                return true;
            });
            scope.fork(() -> {
                creditCheck.check(request.userId());
                return true;
            });
            scope.fork(() -> {
                dedupCheck.check(request.userId(), request.redPacketId());
                return true;
            });

            scope.join();

            // 走到这里说明三项校验全部通过
            return RiskCheckResult.pass(request.userId(), elapsedMs(start));
        } catch (StructuredTaskScope.TimeoutException ex) {
            // 作用域配置的超时窗口触发，剩余子任务已被取消
            return fail(request,
                    new RiskRejectException(request.userId(), "风控窗口超时(" + props.getTimeoutMs() + "ms)熔断"),
                    start);
        } catch (StructuredTaskScope.FailedException ex) {
            // 短路查杀：某个子任务抛出异常，其余子任务已被作用域取消
            return fail(request, ex.getCause(), start);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fail(request, new RiskRejectException(request.userId(), "风控被中断"), start);
        }
    }

    private RiskCheckResult fail(RiskCheckRequest request, Throwable cause, long start) {
        if (cause instanceof RiskRejectException reject) {
            log.info("风控拒绝 userId={} reason={} latencyMs={}", request.userId(), reject.getMessage(), elapsedMs(start));
            return RiskCheckResult.reject(request.userId(), reject.getMessage(), elapsedMs(start));
        }
        log.error("风控子任务异常 userId={}", request.userId(), cause);
        return RiskCheckResult.reject(request.userId(), "风控内部异常", elapsedMs(start));
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
