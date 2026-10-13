package local.pixelmon.backport;

import java.awt.image.BufferedImage;
import java.util.*;

/** Rasterize the native texture silhouette at actual GUI size, then trace inside it. */
public final class BurstOutline {
    private BurstOutline(){}
    public static List<int[]> spans(BufferedImage image,int width,int height){
        if(image==null || width<2 || height<2)return Collections.emptyList();
        boolean[][] solid=new boolean[height][width];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)
            solid[y][x]=(image.getRGB(Math.min(image.getWidth()-1,(int)((x+.5)*image.getWidth()/width)),
                Math.min(image.getHeight()-1,(int)((y+.5)*image.getHeight()/height)))>>>24)>=128;
        List<int[]> result=new ArrayList<>();
        for(int y=0;y<height;y++){
            int start=-1;
            for(int x=0;x<=width;x++){
                boolean edge=x<width && solid[y][x] && (x==0 || x==width-1 || y==0 || y==height-1
                    || !solid[y][x-1] || !solid[y][x+1] || !solid[y-1][x] || !solid[y+1][x]);
                if(edge && start<0)start=x;
                if(!edge && start>=0){result.add(new int[]{start,y,x});start=-1;}
            }
        }
        return result;
    }
}
