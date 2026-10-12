"""Apply native hidden display to the addon legend tab, preserving earned IDs.

Native visibility reveals completed nodes and their ancestors. The addon grants
the root and category with each real achievement, so those ancestors are earned
too. Hidden siblings remain invisible even after their category is earned.
"""
import json,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
def main():
    target=ROOT/'resources/data/pixelmon_backport/advancements/legends'
    paths=sorted(target.glob('*.json'))
    for path in paths:
        node=json.loads(path.read_text(encoding='utf-8'))
        node['display']['hidden']=True
        path.write_text(json.dumps(node,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Hidden {len(paths)} addon legend nodes; IDs, criteria, toast and chat retained.')
if __name__=='__main__':main()
