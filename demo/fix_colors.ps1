$javaDir = 'd:\semester 6\desktop\projek\Sistem-Informasi-Wisata\demo\src\main\java\com\example'
$fileList = @('HomeController.java','LoginController.java','SignupController.java','ItineraryPlannerController.java')
foreach ($fn in $fileList) {
    $path = Join-Path $javaDir $fn
    if (Test-Path $path) {
        $c = [System.IO.File]::ReadAllText($path)
        $c = $c.Replace('#DEFF9A','#2F4156')
        $c = $c.Replace('#f5f5f5','#2F4156')
        $c = $c.Replace('#DAFFDE','#567C8D')
        $c = $c.Replace('#888888','#567C8D')
        $c = $c.Replace('#121212','#C8D9E6')
        [System.IO.File]::WriteAllText($path, $c)
        Write-Output "Updated: $fn"
    } else {
        Write-Output "Skip: $fn"
    }
}
