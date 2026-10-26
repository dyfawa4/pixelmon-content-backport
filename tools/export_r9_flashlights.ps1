param([Parameter(Mandatory=$true)][string]$RedSource,[Parameter(Mandatory=$true)][string]$GreenSource)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assetDirectory=Join-Path $PSScriptRoot '../resources/assets/pixelmon_backport/textures/item'
foreach($entry in @{'shrinking_lamp'=$RedSource;'enlarging_lamp'=$GreenSource}.GetEnumerator()){
 $source=[Drawing.Bitmap]::new($entry.Value);$left=$source.Width;$top=$source.Height;$right=0;$bottom=0
 for($y=0;$y -lt $source.Height;$y++){for($x=0;$x -lt $source.Width;$x++){if($source.GetPixel($x,$y).A -gt 20){$left=[Math]::Min($left,$x);$top=[Math]::Min($top,$y);$right=[Math]::Max($right,$x);$bottom=[Math]::Max($bottom,$y)}}}
 if($right -le $left -or $bottom -le $top){throw 'Empty flashlight source'}
 $output=[Drawing.Bitmap]::new(32,32,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $graphics=[Drawing.Graphics]::FromImage($output);$graphics.Clear([Drawing.Color]::Transparent)
 $graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 $graphics.DrawImage($source,[Drawing.Rectangle]::new(2,2,28,28),$left,$top,$right-$left+1,$bottom-$top+1,[Drawing.GraphicsUnit]::Pixel)
 $output.Save((Join-Path $assetDirectory ($entry.Key+'.png')),[Drawing.Imaging.ImageFormat]::Png)
 $graphics.Dispose();$source.Dispose();$output.Dispose();Write-Output ($entry.Key+' exported: 32x32 RGBA')
}
