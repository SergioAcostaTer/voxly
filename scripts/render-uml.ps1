param(
    [ValidateSet("png", "svg")]
    [string]$Format = "png",
    [string]$Diagram = "voxly-entities"
)

$ErrorActionPreference = "Stop"

$umlDir = Resolve-Path (Join-Path $PSScriptRoot "..\docs\uml")
$mountPath = $umlDir.Path -replace "\\", "/"
$outputFile = "$Diagram.$Format"
$typeFlag = if ($Format -eq "svg") { "-tsvg" } else { "-tpng" }

docker run --rm `
  -v "${mountPath}:/workdir" `
  -w /workdir `
  plantuml/plantuml `
  $typeFlag `
  "$Diagram.puml"

Write-Host ""
Write-Host "Rendered UML diagram:" -ForegroundColor Green
Write-Host (Join-Path $umlDir.Path $outputFile)
