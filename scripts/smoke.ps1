param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
function Invoke-ClaimCommand([string]$Path, [object]$Body) {
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/claims$Path" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 5)
}
$health = Invoke-RestMethod "$BaseUrl/actuator/health"
if ($health.status -ne 'UP') { throw 'Application is not healthy' }
$claim = Invoke-ClaimCommand '' @{
    claimType='MOTOR'; market='SG'; claimantName='Demo claimant'; incidentDescription='Collision at junction'
    incidentDate=(Get-Date).AddDays(-1).ToString('yyyy-MM-dd'); estimatedLiability=1000.00
}
$id = $claim.id
$claim = Invoke-ClaimCommand "/$id/assignment" @{ expectedVersion=$claim.version; officerId='officer-1'; officerName='Demo officer' }
$claim = Invoke-ClaimCommand "/$id/review" @{ expectedVersion=$claim.version }
$claim = Invoke-ClaimCommand "/$id/information-requests" @{ expectedVersion=$claim.version; question='Provide repair estimate' }
$requestId = $claim.informationRequests[0].id
$claim = Invoke-ClaimCommand "/$id/information-requests/$requestId/response" @{ expectedVersion=$claim.version; response='Repair estimate supplied' }
$claim = Invoke-ClaimCommand "/$id/assessment" @{ expectedVersion=$claim.version; estimatedLiability=900.00; approvedSettlementAmount=800.00; reason='Covered loss' }
$claim = Invoke-ClaimCommand "/$id/settlement" @{ expectedVersion=$claim.version }
if ($claim.status -ne 'SETTLED') { throw 'Claim did not settle' }
$history = Invoke-RestMethod "$BaseUrl/api/claims/$id/history"
if ($history.Count -ne 7) { throw 'Unexpected lifecycle history count' }
Invoke-RestMethod "$BaseUrl/api/exposure"
Invoke-RestMethod "$BaseUrl/api/workload"
Write-Output "PASS: claim $id settled through 7 lifecycle operations"
