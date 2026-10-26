param([Parameter(Mandatory=$true)][string]$ShrinkingSource,[Parameter(Mandatory=$true)][string]$EnlargingSource,[Parameter(Mandatory=$true)][string]$ProtocolSource)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assetDirectory=Join-Path $PSScriptRoot '../resources/assets/pixelmon_backport/textures/item'
$exports=@{'shrinking_lamp'=$ShrinkingSource;'enlarging_lamp'=$EnlargingSource;'moving_agreement'=$ProtocolSource}
foreach($entry in $exports.GetEnumerator()){
 $source=[Drawing.Bitmap]::new($entry.Value)
 $left=$source.Width;$top=$source.Height;$right=0;$bottom=0
 for($y=0;$y -lt $source.Height;$y++){for($x=0;$x -lt $source.Width;$x++){if($source.GetPixel($x,$y).A -gt 20){$left=[Math]::Min($left,$x);$top=[Math]::Min($top,$y);$right=[Math]::Max($right,$x);$bottom=[Math]::Max($bottom,$y)}}}
 if($right -le $left -or $bottom -le $top){throw ('Empty source: '+$entry.Value)}
 $isLamp=$entry.Key -ne 'moving_agreement';$size=32;if($isLamp){$size=16}
 $sprite=[Drawing.Bitmap]::new($size,$size,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $graphics=[Drawing.Graphics]::FromImage($sprite);$graphics.Clear([Drawing.Color]::Transparent)
 $graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 $rect=[Drawing.Rectangle]::new(1,1,14,14)
 if(!$isLamp){$ratio=28/[Math]::Max(($right-$left+1),($bottom-$top+1));$w=[int][Math]::Round(($right-$left+1)*$ratio);$h=[int][Math]::Round(($bottom-$top+1)*$ratio);$rect=[Drawing.Rectangle]::new([int]((32-$w)/2),[int]((32-$h)/2),$w,$h)}
 $graphics.DrawImage($source,$rect,$left,$top,$right-$left+1,$bottom-$top+1,[Drawing.GraphicsUnit]::Pixel);$graphics.Dispose();$source.Dispose()
 $target=Join-Path $assetDirectory ($entry.Key+'.png')
 if($isLamp){
  $output=[Drawing.Bitmap]::new(32,32,[Drawing.Imaging.PixelFormat]::Format32bppArgb);$graphics=[Drawing.Graphics]::FromImage($output)
  $graphics.Clear([Drawing.Color]::Transparent);$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
  $graphics.DrawImage($sprite,[Drawing.Rectangle]::new(0,0,32,32),0,0,16,16,[Drawing.GraphicsUnit]::Pixel)
  $output.Save($target,[Drawing.Imaging.ImageFormat]::Png);$graphics.Dispose();$output.Dispose()
 }else{$sprite.Save($target,[Drawing.Imaging.ImageFormat]::Png)}
 $sprite.Dispose();Write-Output ($entry.Key+' exported: 32x32 RGBA')
}
