function Send-StockMovement {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [ValidateSet("ENTRADA", "SALIDA")]
        [string]$Type,

        [Parameter(Mandatory)]
        [ValidateNotNullOrEmpty()]
        [string]$ProductCode,

        [Parameter(Mandatory)]
        [ValidateRange(1, [int]::MaxValue)]
        [int]$Quantity
    )

    $body = @{
        tipoMovimiento = $Type
        productos = @(
            @{
                codigoProducto = $ProductCode
                cantidad = $Quantity
            }
        )
    } | ConvertTo-Json -Depth 4

    Invoke-RestMethod -Method Post `
        -Uri "http://localhost:8080/api/movimientos" `
        -ContentType "application/json" `
        -Body $body
}
