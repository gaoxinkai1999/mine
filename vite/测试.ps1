

Write-Host "构建前端代码..." -ForegroundColor Cyan
npm run build



# 是否在设备上运行（可选）
$RunOnDevice = $true
if ($RunOnDevice) {
    Write-Host "在设备上运行..." -ForegroundColor Cyan
    npx cap run android --target=49a9e8e
}