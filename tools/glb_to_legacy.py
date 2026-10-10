"""Convert the upstream skinned GLB to legacy SMD without a native dependency.

glTF 2.0: https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html
The converter rejects unsupported features instead of silently dropping them.
"""
import json, struct, math
import numpy as np

def quaternion_matrix(q):
    x,y,z,w=np.array(q)/np.linalg.norm(q)
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)], [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)], [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])

def euler(matrix):
    sy=math.hypot(matrix[0,0],matrix[1,0])
    if sy>1e-7:return (math.atan2(matrix[2,1],matrix[2,2]),math.atan2(-matrix[2,0],sy),math.atan2(matrix[1,0],matrix[0,0]))
    return (math.atan2(-matrix[1,2],matrix[1,1]),math.atan2(-matrix[2,0],sy),0)

def convert_glb(data):
    magic,version,length=struct.unpack_from('<III',data)
    assert magic==0x46546c67 and version==2 and length==len(data)
    size,kind=struct.unpack_from('<II',data,12);assert kind==0x4e4f534a
    doc=json.loads(data[20:20+size]);pos=20+size
    size,kind=struct.unpack_from('<II',data,pos);assert kind==0x004e4942
    blob=data[pos+8:pos+8+size]
    assert not doc.get('extensionsRequired'), 'Unsupported GLB extension'
    def accessor(idx):
        a=doc['accessors'][idx];assert 'sparse' not in a
        v=doc['bufferViews'][a['bufferView']];assert v.get('buffer',0)==0
        dtype={5120:'i1',5121:'u1',5122:'<i2',5123:'<u2',5125:'<u4',5126:'<f4'}[a['componentType']]
        count={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}[a['type']]
        item=np.dtype(dtype).itemsize;start=v.get('byteOffset',0)+a.get('byteOffset',0)
        stride=v.get('byteStride',count*item)
        result=np.ndarray((a['count'],count),dtype=dtype,buffer=blob,offset=start,strides=(stride,item)).copy()
        if a.get('normalized') and a['componentType']!=5126:result=result/np.iinfo(dtype).max
        return result
    nodes=doc['nodes'];parents={c:i for i,n in enumerate(nodes) for c in n.get('children',[])}
    mesh_nodes=[i for i,n in enumerate(nodes) if 'mesh' in n];assert len(mesh_nodes)==1
    mi=mesh_nodes[0];skin=doc['skins'][nodes[mi]['skin']];joints=skin['joints']
    used=set(joints)
    for j in joints:
        while j in parents:j=parents[j];used.add(j)
    order=[]
    def visit(i):
        if i in used:order.append(i)
        for j in nodes[i].get('children',[]):visit(j)
    for i in doc['scenes'][doc.get('scene',0)]['nodes']:visit(i)
    mapping={n:i for i,n in enumerate(order)}
    # glTF uses Y-up, SMD/BMD uses Z-up.
    axis=np.array([[1,0,0],[0,0,-1],[0,1,0]],float)
    def matrix(n):
        assert 'matrix' not in n
        m=np.eye(4);m[:3,:3]=quaternion_matrix(n.get('rotation',[0,0,0,1]))@np.diag(n.get('scale',[1,1,1]));m[:3,3]=n.get('translation',[0,0,0]);return m
    def global_matrix(i):return global_matrix(parents[i])@matrix(nodes[i]) if i in parents else matrix(nodes[i])
    mesh_transform=global_matrix(mi)
    inverse=accessor(skin['inverseBindMatrices']).reshape((-1,4,4)).transpose(0,2,1)
    # Bind pose consistency is essential for weighted animation.
    for i,j in enumerate(joints):
        assert np.allclose(global_matrix(j)@inverse[i],mesh_transform,atol=.003), 'GLB bind pose mismatch'
    def pose(transforms):
        lines=[]
        for i in order:
            n=nodes[i];t=np.array(transforms.get((i,'translation'),n.get('translation',[0,0,0])),float)
            rot=quaternion_matrix(transforms.get((i,'rotation'),n.get('rotation',[0,0,0,1])))
            # Legacy SMD has no scale column. Full transforms are retained in
            # the companion matrix stream and restored before native reform().
            if i not in parents:t=axis@t;rot=axis@rot
            values=[*t,*euler(rot)];lines.append(str(mapping[i])+' '+' '.join(f'{v:.9g}' for v in values))
        return lines
    bone_lines=[f'{mapping[i]} "{nodes[i].get("name",str(i))}" {mapping.get(parents.get(i),-1)}' for i in order]
    header=['version 1','nodes',*bone_lines,'end','skeleton','time 0',*pose({}),'end']
    lines=[*header,'triangles']
    for prim in doc['meshes'][nodes[mi]['mesh']]['primitives']:
        assert prim.get('mode',4)==4 and not prim.get('targets')
        at=prim['attributes'];positions=accessor(at['POSITION']);normals=accessor(at['NORMAL']);uv=accessor(at['TEXCOORD_0']);ji=accessor(at['JOINTS_0']);weights=accessor(at['WEIGHTS_0']);indices=accessor(prim['indices']).reshape(-1)
        assert not any(k.startswith('JOINTS_') and k!='JOINTS_0' for k in at)
        positions=(axis@(mesh_transform[:3,:3]@positions.T+mesh_transform[:3,3:4])).T
        normals=(axis@np.linalg.inv(mesh_transform[:3,:3]).T@normals.T).T
        for start in range(0,len(indices),3):
            lines.append('texture.png')
            for vi in indices[start:start+3]:
                links=[(mapping[joints[int(j)]],float(w)) for j,w in zip(ji[vi],weights[vi]) if w>1e-7]
                assert abs(sum(w for _,w in links)-1)<.001
                # SMD v is bottom-up; glTF v is top-down.
                values=[*positions[vi],*normals[vi],uv[vi,0],1-uv[vi,1]]
                lines.append('0 '+' '.join(f'{v:.9g}' for v in values)+' '+str(len(links))+' '+' '.join(f'{j} {w:.9g}' for j,w in links))
    lines.append('end');result={'model.smd':('\n'.join(lines)+'\n').encode()}
    def sample(times,vals,t,quat=False,mode='LINEAR'):
        k=int(np.searchsorted(times,t,side='right')-1);k=max(0,min(k,len(times)-1))
        if k==len(times)-1 or mode=='STEP':return vals[k]
        alpha=(t-times[k])/(times[k+1]-times[k]);a,b=vals[k],vals[k+1]
        if quat:
            dot=np.dot(a,b)
            if dot<0:b=-b;dot=-dot
            if dot<.9995:
                theta=math.acos(min(1,dot));return (math.sin((1-alpha)*theta)*a+math.sin(alpha*theta)*b)/math.sin(theta)
        return (1-alpha)*a+alpha*b
    for anim in doc.get('animations',[]):
        channels=[];duration=0
        for ch in anim['channels']:
            target=ch['target'];assert target['path'] in ['translation','rotation','scale']
            sampler=anim['samplers'][ch['sampler']];mode=sampler.get('interpolation','LINEAR');assert mode in ['LINEAR','STEP']
            times=accessor(sampler['input']).reshape(-1);vals=accessor(sampler['output']);duration=max(duration,float(times[-1]));channels.append((target,times,vals,mode))
        ls=['version 1','nodes',*bone_lines,'end','skeleton'];matrices=[]
        for frame in range(int(round(duration*30))+1):
            t=frame/30;ls.append('time '+str(frame));transforms={(ch['node'],ch['path']):sample(ts,vs,t,ch['path']=='rotation',mode) for ch,ts,vs,mode in channels};ls.extend(pose(transforms))
            flip=np.diag([1,-1,-1,1]);axis4=np.eye(4);axis4[:3,:3]=axis
            for i in order:
                node=dict(nodes[i])
                for path in ['translation','rotation','scale']:
                    if (i,path) in transforms:node[path]=transforms[i,path]
                local=matrix(node)
                if i not in parents:local=axis4@local
                local=flip@local@flip
                assert np.isfinite(local).all() and abs(np.linalg.det(local))>1e-9
                matrices.extend(local.flatten().tolist())
        ls.append('end');result[anim['name']+'.smd']=('\n'.join(ls)+'\n').encode()
        result[anim['name']+'.smd.matrices']=struct.pack('>iii',1,int(round(duration*30))+1,len(order))+struct.pack('>'+str(len(matrices))+'f',*matrices)
    assert len(doc['images'])==1
    view=doc['bufferViews'][doc['images'][0]['bufferView']];start=view.get('byteOffset',0);result['texture.png']=blob[start:start+view['byteLength']]
    return result
