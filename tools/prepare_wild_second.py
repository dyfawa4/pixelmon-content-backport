"""Convert the supplied second wild track to a normalized Minecraft stream."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('source',type=Path);parser.add_argument('--ffmpeg',type=Path);args=parser.parse_args()
 if args.ffmpeg:ffmpeg=str(args.ffmpeg)
 else:
  sys.path.insert(0,str(ROOT/'.cache/media-tools'));import imageio_ffmpeg;ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
 target=-12.0
 def measure(path):
  r=subprocess.run([ffmpeg,'-hide_banner','-i',str(path),'-af',f'loudnorm=I={target}:TP=-2:LRA=11:print_format=json','-f','null','-'],capture_output=True,text=True,check=True)
  return json.loads(r.stderr[r.stderr.rfind('{'):r.stderr.rfind('}')+1])
 before=measure(args.source);out=ROOT/'resources/assets/pixelmon_backport/sounds/music/wild_two.ogg'
 filt=f"loudnorm=I={target}:TP=-2:LRA=11:measured_I={before['input_i']}:measured_TP={before['input_tp']}:measured_LRA={before['input_lra']}:measured_thresh={before['input_thresh']}:offset={before['target_offset']}:linear=true"
 subprocess.run([ffmpeg,'-v','error','-y','-i',str(args.source),'-vn','-af',filt,'-ar','44100','-ac','2','-c:a','libvorbis','-q:a','5',str(out)],check=True)
 after=measure(out);assert abs(float(after['input_i'])-target)<.6;assert float(after['input_tp'])<-.5
 p=ROOT/'resources/assets/pixelmon_backport/sounds.json';data=json.loads(p.read_text('utf-8'));data['music.battle.wild_two']={'sounds':[{'name':'pixelmon_backport:music/wild_two','stream':True}]};p.write_text(json.dumps(data,indent=2)+'\n','utf-8')
 receipt={'source':str(args.source),'source_sha256':hashlib.sha256(args.source.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'before_lufs':before['input_i'],'after_lufs':after['input_i'],'true_peak_dbtp':after['input_tp'],'full_decode':'PASS'}
 (ROOT/'.cache/update-20261023/wild-second-audio.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n','utf-8');print(json.dumps(receipt,ensure_ascii=False))
if __name__=='__main__':main()
