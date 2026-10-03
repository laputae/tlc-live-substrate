$ErrorActionPreference = 'Stop'
[System.Net.WebRequest]::DefaultWebProxy = $null
$cts = [System.Threading.CancellationTokenSource]::new()

function Connect-Ws([string]$url) {
    $ws = [System.Net.WebSockets.ClientWebSocket]::new()
    try { $ws.Options.Proxy = $null } catch {}
    try {
        $ws.ConnectAsync([Uri]$url, $cts.Token).Wait()
    } catch {
        $e = $_.Exception
        while ($e) { Write-Host "   ERR: $($e.GetType().Name) :: $($e.Message)"; $e = $e.InnerException }
        throw
    }
    return $ws
}

function Send-Ws($ws, [string]$msg) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($msg)
    $seg = [ArraySegment[byte]]::new($bytes)
    $ws.SendAsync($seg, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).Wait()
}

function Recv-Ws($ws, [int]$timeoutSec) {
    $buf = New-Object byte[] 16384
    $sb = [System.Text.StringBuilder]::new()
    $inner = [System.Threading.CancellationTokenSource]::new()
    $inner.CancelAfter($timeoutSec * 1000)
    try {
        do {
            $seg = [ArraySegment[byte]]::new($buf)
            $r = $ws.ReceiveAsync($seg, $inner.Token).Result
            if ($r.Count -le 0) { return $null }
            [void]$sb.Append([System.Text.Encoding]::UTF8.GetString($buf, 0, $r.Count))
        } while (-not $r.EndOfMessage)
        return $sb.ToString()
    } catch {
        return $null
    }
}

Write-Host "1. connect push/gateway..."
$push = Connect-Ws "ws://100.70.150.101:8085/ws/live"
$gw   = Connect-Ws "ws://100.70.150.101:8081/ws/upstream"
Write-Host "   push=$($push.State) gateway=$($gw.State)"

Write-Host "2. send 6 cold danmaku..."
$danmaku = @(
    @{userId=201; roomId='room-1'; content='主播在吗，怎么不说话'},
    @{userId=202; roomId='room-1'; content='好无聊啊这个直播间'},
    @{userId=203; roomId='room-1'; content='没意思，散了吧'},
    @{userId=204; roomId='room-1'; content='困了，主播也不互动'},
    @{userId=205; roomId='room-1'; content='冷场了都'},
    @{userId=206; roomId='room-1'; content='直播间好安静'}
)
foreach ($d in $danmaku) {
    Send-Ws $gw ($d | ConvertTo-Json -Compress)
    Start-Sleep -Milliseconds 300
}

Write-Host "3. listen downstream (max 75s)..."
$dropId = $null
$sw = [Diagnostics.Stopwatch]::StartNew()
while ($sw.Elapsed.TotalSeconds -lt 75 -and -not $dropId) {
    $msg = Recv-Ws $push 20
    if ($msg) {
        Write-Host "   [push] $msg"
        if ($msg -match '"type":"REDPACKET_DROP"' -and $msg -match '"redPacketId":(\d+)') {
            $dropId = $Matches[1]
        }
    }
}

if (-not $dropId) {
    Write-Host "NO REDPACKET_DROP (LLM may decide no action)."
    exit 0
}

Write-Host "4. rush via gateway, id=$dropId ..."
Send-Ws $gw (@{userId=201; roomId='room-1'; redPacketId=[long]$dropId; type='RUSH'} | ConvertTo-Json -Compress)
$ack = Recv-Ws $gw 15
Write-Host "   [ack1] $ack"

Write-Host "5. rush again (idempotency)..."
Send-Ws $gw (@{userId=201; roomId='room-1'; redPacketId=[long]$dropId; type='RUSH'} | ConvertTo-Json -Compress)
$ack2 = Recv-Ws $gw 15
Write-Host "   [ack2] $ack2"

$push.Dispose(); $gw.Dispose(); $cts.Dispose()
Write-Host "DONE."