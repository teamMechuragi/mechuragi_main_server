# 투표 알림 시스템 테스트 실행 스크립트

Write-Host "================================" -ForegroundColor Cyan
Write-Host "투표 알림 시스템 테스트 실행" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1. VotePostServiceTest
Write-Host "[1/4] VotePostServiceTest 실행 중..." -ForegroundColor Yellow
./gradlew test --tests "*VotePostServiceTest" --info | Select-String -Pattern "VotePostServiceTest|PASSED|FAILED|테스트"
Write-Host ""

# 2. VoteNotificationSubscriberTest
Write-Host "[2/4] VoteNotificationSubscriberTest 실행 중..." -ForegroundColor Yellow
./gradlew test --tests "*VoteNotificationSubscriberTest" --info | Select-String -Pattern "VoteNotificationSubscriberTest|PASSED|FAILED|테스트"
Write-Host ""

# 3. VoteNotificationSchedulerTest
Write-Host "[3/4] VoteNotificationSchedulerTest 실행 중..." -ForegroundColor Yellow
./gradlew test --tests "*VoteNotificationSchedulerTest" --info | Select-String -Pattern "VoteNotificationSchedulerTest|PASSED|FAILED|테스트"
Write-Host ""

# 4. 통합 테스트
Write-Host "[4/4] RedisPubSubIntegrationTest 실행 중..." -ForegroundColor Yellow
Write-Host "(Redis 서버가 실행 중이어야 합니다)" -ForegroundColor Gray
./gradlew test --tests "*RedisPubSubIntegrationTest" --info | Select-String -Pattern "RedisPubSubIntegrationTest|PASSED|FAILED|테스트"
Write-Host ""

# 결과 요약
Write-Host "================================" -ForegroundColor Cyan
Write-Host "테스트 완료!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "자세한 결과는 다음 파일을 확인하세요:" -ForegroundColor Yellow
Write-Host "build\reports\tests\test\index.html" -ForegroundColor White
Write-Host ""
Write-Host "HTML 리포트 열기:" -ForegroundColor Yellow
Write-Host "Start-Process build\reports\tests\test\index.html" -ForegroundColor White
