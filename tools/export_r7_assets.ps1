Add-Type -AssemblyName System.Drawing
$assetDirectory=Join-Path $PSScriptRoot '../resources/assets/pixelmon_backport/textures/item'
$generatedDirectory='C:/Users/29910/.codex/generated_images/01a16d31-51dd-7541-aa90-3e7ebe59fda4'
$exports=@{
 'shrinking_lamp'='exec-0ad70130-709a-4254-b11b-e63f0175672f.png'
 'enlarging_lamp'='exec-3ccb1f34-f418-4025-9ed8-f632e4c92268.png'
 'moving_agreement'='exec-97aeeddc-dcd0-4690-9497-c85e5505e4a5.png'
}
foreach($entry in $exports.GetEnumerator()) {
 $source=[Drawing.Bitmap]::new((Join-Path $generatedDirectory $entry.Value))
 $left=$source.Width;$top=$source.Height;$right=0;$bottom=0
 for($y=0;$y -lt $source.Height;$y++){for($x=0;$x -lt $source.Width;$x++){if($source.GetPixel($x,$y).A -gt 20){$left=[Math]::Min($left,$x);$top=[Math]::Min($top,$y);$right=[Math]::Max($right,$x);$bottom=[Math]::Max($bottom,$y)}}}
 $output=[Drawing.Bitmap]::new(32,32,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $graphics=[Drawing.Graphics]::FromImage($output);$graphics.Clear([Drawing.Color]::Transparent)
 $graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 $rect=[Drawing.Rectangle]::new(2,2,28,28)
 if($entry.Key -eq 'moving_agreement'){$ratio=28/[Math]::Max(($right-$left+1),($bottom-$top+1));$w=[int][Math]::Round(($right-$left+1)*$ratio);$h=[int][Math]::Round(($bottom-$top+1)*$ratio);$rect=[Drawing.Rectangle]::new([int]((32-$w)/2),[int]((32-$h)/2),$w,$h)}
 $graphics.DrawImage($source,$rect,$left,$top,$right-$left+1,$bottom-$top+1,[Drawing.GraphicsUnit]::Pixel)
 $output.Save((Join-Path $assetDirectory ($entry.Key+'.png')),[Drawing.Imaging.ImageFormat]::Png)
 $graphics.Dispose();$source.Dispose();$output.Dispose()
 Write-Output ($entry.Key+' exported: 32x32 RGBA')
}
