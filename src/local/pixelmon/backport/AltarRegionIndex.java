package local.pixelmon.backport;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Read only legacy structure metadata. No chunk requests, generation, or region writes. */
public final class AltarRegionIndex {
 public static final class Point {public final String type;public final int x,z;public Point(String t,int x,int z){type=t;this.x=x;this.z=z;}}
 public static List<Point> read(Path file,int rx,int rz)throws IOException{
  List<Point> out=new ArrayList<>();if(!Files.isRegularFile(file))return out;
  try(RandomAccessFile region=new RandomAccessFile(file.toFile(),"r")){
   if(region.length()<8192)return out;int[] offsets=new int[1024];for(int i=0;i<1024;i++)offsets[i]=region.readInt();
   for(int i=0;i<1024;i++){int offset=offsets[i]>>>8;if(offset<2)continue;long address=(long)offset*4096;if(address+5>region.length())continue;
    region.seek(address);int length=region.readInt(),compression=region.readUnsignedByte();if(length<1||length>16*1024*1024||address+4L+length>region.length())continue;
    InputStream raw;if((compression&128)!=0){Path external=file.resolveSibling("c."+(rx*32+(i&31))+"."+(rz*32+(i>>5))+".mcc");if(!Files.isRegularFile(external))continue;raw=Files.newInputStream(external);}else{byte[] data=new byte[length-1];region.readFully(data);raw=new ByteArrayInputStream(data);}
    try(InputStream payload=(compression&127)==1?new GZIPInputStream(raw):(compression&127)==2?new InflaterInputStream(raw):raw;DataInputStream in=new DataInputStream(new BufferedInputStream(payload))){if(in.readUnsignedByte()!=10)continue;in.readUTF();compound(in,0,out);}catch(IOException malformed){/* A concurrently saved chunk is observed again on its normal load event. */}
   }
  }return out;
 }
 private static void compound(DataInputStream in,int depth,List<Point> out)throws IOException{
  for(int type;(type=in.readUnsignedByte())!=0;){String key=in.readUTF();if(type==10&&((depth==0&&key.equals("Level"))||(depth==1&&key.equals("Structures"))||(depth==2&&key.equals("Starts"))))compound(in,depth+1,out);else if(depth==3&&type==10)start(in,out);else skip(in,type,0);}
 }
 private static void start(DataInputStream in,List<Point> out)throws IOException{
  String id="";int[] box=null;for(int type;(type=in.readUnsignedByte())!=0;){String key=in.readUTF();if(type==8&&key.equals("id"))id=in.readUTF().toLowerCase(Locale.ROOT);else if(type==11&&key.equals("BB")){int n=in.readInt();if(n==6){box=new int[6];for(int i=0;i<6;i++)box[i]=in.readInt();}else bytes(in,(long)n*4);}else skip(in,type,0);}
  if(box!=null&&AltarSpacing.managed(id))out.add(new Point(id,(int)(((long)box[0]+box[3])/2),(int)(((long)box[2]+box[5])/2)));
 }
 private static void bytes(DataInputStream in,long n)throws IOException{if(n<0||n>256L*1024*1024)throw new IOException("NBT length");while(n>0){int skipped=in.skipBytes((int)Math.min(n,1<<20));if(skipped==0){in.readByte();skipped=1;}n-=skipped;}}
 private static void skip(DataInputStream in,int type,int depth)throws IOException{
  if(depth>64)throw new IOException("NBT depth");switch(type){case 0:return;case 1:bytes(in,1);return;case 2:bytes(in,2);return;case 3:case 5:bytes(in,4);return;case 4:case 6:bytes(in,8);return;case 7:bytes(in,in.readInt());return;case 8:bytes(in,in.readUnsignedShort());return;case 9:{int child=in.readUnsignedByte(),n=in.readInt();if(n<0||n>16777216)throw new IOException("NBT list");for(int i=0;i<n;i++)skip(in,child,depth+1);return;}case 10:for(int child;(child=in.readUnsignedByte())!=0;){in.readUTF();skip(in,child,depth+1);}return;case 11:bytes(in,(long)in.readInt()*4);return;case 12:bytes(in,(long)in.readInt()*8);return;default:throw new IOException("NBT type");}
 }
}
