Write-Host "开始检查Java文件的包声明和导入语句..." -ForegroundColor Green

# 获取所有重构后的Java文件
$javaFiles = Get-ChildItem -Path "src\main\java\com\example\domain", "src\main\java\com\example\infrastructure" -Filter "*.java" -Recurse

# 检查每个文件的包声明和导入
foreach ($file in $javaFiles) {
    $content = Get-Content -Path $file.FullName -Raw
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    $expectedPackage = $relativePath.Replace("\", ".").Replace("src.main.java.", "").Replace(".$($file.Name)", "")
    
    # 获取当前包声明
    if ($content -match "package\s+(.*?);") {
        $currentPackage = $matches[1]
        
        # 检查包声明是否正确
        if ($currentPackage -ne $expectedPackage) {
            Write-Host "需要更新包声明: $relativePath" -ForegroundColor Yellow
            Write-Host "  当前: package $currentPackage;" -ForegroundColor Red
            Write-Host "  应为: package $expectedPackage;" -ForegroundColor Green
        }
    } else {
        Write-Host "未找到包声明: $relativePath" -ForegroundColor Red
    }
    
    # 检查导入语句中是否包含旧的模块路径
    if ($content -match "import\s+com\.example\.modules\.") {
        Write-Host "需要更新导入语句: $relativePath" -ForegroundColor Yellow
        
        # 提取所有需要更新的导入语句
        $importMatches = [regex]::Matches($content, "import\s+(com\.example\.modules\.[^;]+);")
        foreach ($importMatch in $importMatches) {
            $importStatement = $importMatch.Groups[1].Value
            Write-Host "  发现旧导入: import $importStatement;" -ForegroundColor Red
        }
    }
    
    # 检查导入语句中是否包含旧的Config或exception路径
    if ($content -match "import\s+com\.example\.(Config|exception)\.") {
        Write-Host "需要更新基础设施导入语句: $relativePath" -ForegroundColor Yellow
        
        # 提取所有需要更新的导入语句
        $importMatches = [regex]::Matches($content, "import\s+(com\.example\.(Config|exception)\.[^;]+);")
        foreach ($importMatch in $importMatches) {
            $importStatement = $importMatch.Groups[1].Value
            Write-Host "  发现旧导入: import $importStatement;" -ForegroundColor Red
        }
    }
}

Write-Host "`n检查完成! 请根据上述信息更新包声明和导入语句。" -ForegroundColor Green
Write-Host "按任意键继续..."
$host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") | Out-Null 