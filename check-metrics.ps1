# 투표 알림 시스템 메트릭 확인 스크립트

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  투표 알림 시스템 메트릭 확인" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/actuator/metrics"

# 메트릭 목록
$metrics = @(
    @{Name="알림 발송 성공"; Url="vote.notification.sent"},
    @{Name="알림 발송 실패"; Url="vote.notification.failed"},
    @{Name="Redis 메시지 수신"; Url="vote.redis.message.received"},
    @{Name="STOMP 메시지 발송"; Url="vote.stomp.message.sent"},
    @{Name="알림 처리 시간"; Url="vote.notification.duration"},
    @{Name="Redis 발행 시간"; Url="vote.redis.publish.duration"}
)

foreach ($metric in $metrics) {
    Write-Host "[$($metric.Name)]" -ForegroundColor Yellow

    try {
        $response = Invoke-RestMethod "$baseUrl/$($metric.Url)"

        Write-Host "  이름: $($response.name)" -ForegroundColor White
        Write-Host "  설명: $($response.description)" -ForegroundColor Gray

        if ($response.measurements) {
            Write-Host "  측정값:" -ForegroundColor White
            foreach ($measurement in $response.measurements) {
                $value = [math]::Round($measurement.value, 2)
                Write-Host "    - $($measurement.statistic): $value" -ForegroundColor Green
            }
        }

        if ($response.availableTags -and $response.availableTags.Count -gt 0) {
            Write-Host "  사용 가능한 태그:" -ForegroundColor White
            foreach ($tag in $response.availableTags) {
                Write-Host "    - $($tag.tag): $($tag.values -join ', ')" -ForegroundColor Cyan
            }
        }

    } catch {
        Write-Host "  오류: $_" -ForegroundColor Red
    }

    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  전체 메트릭 목록 확인" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

try {
    $allMetrics = Invoke-RestMethod $baseUrl
    $voteMetrics = $allMetrics.names | Where-Object { $_ -like "vote.*" }

    Write-Host "투표 관련 메트릭 ($($voteMetrics.Count)개):" -ForegroundColor Yellow
    foreach ($metric in $voteMetrics) {
        Write-Host "  - $metric" -ForegroundColor White
    }
} catch {
    Write-Host "전체 메트릭 목록 조회 실패: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "브라우저에서 보려면:" -ForegroundColor Yellow
Write-Host "  start http://localhost:8080/actuator/metrics" -ForegroundColor White
Write-Host ""
