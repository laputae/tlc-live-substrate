package com.tlc.live.risk.web;

import com.tlc.live.risk.orchestrator.RiskCheckOrchestrator;
import com.tlc.live.risk.orchestrator.RiskCheckRequest;
import com.tlc.live.risk.orchestrator.RiskCheckResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风控判定入口：网关把通过 DOP 路由的抢红包事件转发到这里。
 */
@RestController
@RequestMapping("/api/risk")
public class RiskCheckController {

    private final RiskCheckOrchestrator orchestrator;

    public RiskCheckController(RiskCheckOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/check")
    public RiskCheckResult check(@RequestBody RiskCheckRequest request) {
        return orchestrator.check(request);
    }
}
