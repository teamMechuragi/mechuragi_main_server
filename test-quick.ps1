# 빠른 테스트 실행 및 결과 확인 스크립트
param(
    [string]$TestClass = "VotePostServiceTest"
)

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  테스트 실행: $TestClass" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# 캐시 삭제 및 테스트 실행
Write-Host "테스트 실행 중..." -ForegroundColor Yellow
./gradlew cleanTest test --tests "*$TestClass" --console=plain 2>&1 | Tee-Object -Variable output

# 결과 파싱
$passed = ($output | Select-String "PASSED" | Measure-Object).Count
$failed = ($output | Select-String "FAILED" | Measure-Object).Count

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  테스트 결과 요약" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "성공: $passed 개" -ForegroundColor Green
Write-Host "실패: $failed 개" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host ""

# HTML 리포트 열기
Write-Host "HTML 리포트를 여시겠습니까? (y/n): " -ForegroundColor Yellow -NoNewline
$response = Read-Host

if ($response -eq 'y' -or $response -eq 'Y' -or $response -eq '') {
    if (Test-Path "build\reports\tests\test\index.html") {
        Write-Host "HTML 리포트 열기..." -ForegroundColor Green
        Start-Process "build\reports\tests\test\index.html"
    } else {
        Write-Host "HTML 리포트 파일을 찾을 수 없습니다." -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "완료!" -ForegroundColor Green
