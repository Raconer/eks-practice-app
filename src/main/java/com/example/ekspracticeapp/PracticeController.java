package com.example.ekspracticeapp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PracticeController {

    // 요청 하나가 스레드를 오래 점유하지 않도록 CPU 소모 시간 상한 지정
    static final long MAX_MS = 1000;

    // Pod 식별용. K8s는 HOSTNAME 환경 변수에 Pod 이름을 넣어 줌
    private final String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello", "hostname", hostname);
    }

    @GetMapping("/cpu")
    public Map<String, Object> cpu(@RequestParam(defaultValue = "200") long ms) {
        if (ms < 1 || ms > MAX_MS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ms는 1~" + MAX_MS + " 범위여야 합니다");
        }

        long start = System.nanoTime();
        long deadline = start + ms * 1_000_000;
        double acc = 0;
        // sleep이 아닌 실제 계산으로 CPU 사용률을 올려 HPA가 감지하게 함
        while (System.nanoTime() < deadline) {
            for (int i = 1; i <= 1_000; i++) {
                acc += Math.sqrt(i) * Math.sin(i);
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        return Map.of(
                "requestedMs", ms,
                "elapsedMs", elapsedMs,
                "hostname", hostname,
                // 계산 결과를 응답에 포함해 JIT가 루프를 제거하지 못하게 함
                "result", acc);
    }
}
