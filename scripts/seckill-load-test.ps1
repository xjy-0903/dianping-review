param(
    [int]$Users = 20,
    [int]$Rounds = 5,
    [long]$VoucherId = 2
)

Write-Host "压测开始: $Users 个用户并发抢购,每人重复请求 $Rounds 次, voucherId=$VoucherId"
$url = "http://localhost:8081/api/voucher-order/seckill/$VoucherId"
$jobs = 1..$Users | ForEach-Object {
    $userId = $_
    Start-Job -ScriptBlock {
        param($url, $userId, $rounds)
        $success = 0
        $rejected = 0
        $failed = 0
        for ($i = 0; $i -lt $rounds; $i++) {
            try {
                $resp = Invoke-WebRequest -Uri $url -Method Post -Headers @{ "X-User-Id" = "$userId" } -UseBasicParsing -TimeoutSec 10
                $body = $resp.Content | ConvertFrom-Json
                if ($body.success) { $success++ } else { $rejected++ }
            } catch { $failed++ }
        }
        "userId=$userId success=$success rejected=$rejected failed=$failed"
    } -ArgumentList $url, $userId, $Rounds
}
$results = $jobs | Wait-Job | ForEach-Object { Receive-Job $_ }
$jobs | Remove-Job
$results
$totalSuccess = ($results | ForEach-Object { if ($_ -match 'success=(\d+)') { [int]$Matches[1] } } | Measure-Object -Sum).Sum
$totalRejected = ($results | ForEach-Object { if ($_ -match 'rejected=(\d+)') { [int]$Matches[1] } } | Measure-Object -Sum).Sum
Write-Host "汇总: 总成功=$totalSuccess 总拒绝=$totalRejected"
Write-Host "预期: 每用户最多成功1次(一人一单),总成功数不超过秒杀券库存"
