import wave, struct, math, random, sys
SR = 44100
random.seed(7)

def biquad_bp(x, f, q):
    w = 2*math.pi*f/SR; al = math.sin(w)/(2*q); cw = math.cos(w)
    b0, b1, b2 = al, 0.0, -al
    a0, a1, a2 = 1+al, -2*cw, 1-al
    b0/=a0; b1/=a0; b2/=a0; a1/=a0; a2/=a0
    y=[0.0]*len(x); x1=x2=y1=y2=0.0
    for i,s in enumerate(x):
        o=b0*s+b1*x1+b2*x2-a1*y1-a2*y2
        x2=x1; x1=s; y2=y1; y1=o; y[i]=o
    return y

def lowpass(x, fc):
    a = 1-math.exp(-2*math.pi*fc/SR); y=[]; p=0.0
    for s in x: p += a*(s-p); y.append(p)
    return y

def reverb(x, wet=0.14, length=0.35):
    # pequeno reverb tipo Schroeder (4 combs + 2 allpass)
    n = len(x) + int(SR*length)
    src = x + [0.0]*(n-len(x))
    out = [0.0]*n
    for d_ms, g in ((29.7,0.72),(37.1,0.70),(41.1,0.68),(43.7,0.66)):
        d = int(SR*d_ms/1000); buf=[0.0]*d; idx=0; lp=0.0
        for i in range(n):
            o = buf[idx]; lp = o*0.6 + lp*0.4
            buf[idx] = src[i] + lp*g; idx=(idx+1)%d
            out[i] += o*0.25
    for d_ms, g in ((5.0,0.7),(1.7,0.7)):
        d=int(SR*d_ms/1000); buf=[0.0]*d; idx=0; y=[0.0]*n
        for i in range(n):
            b=buf[idx]; v=out[i]+(-g)*b
            y[i]=b+g*v; buf[idx]=v; idx=(idx+1)%d
        out=y
    return [src[i]*(1-wet) + out[i]*wet for i in range(n)]

def env_exp(n, tau, attack=0.0015):
    return [(0.5-0.5*math.cos(math.pi*min(1,(i/SR)/attack))) * math.exp(-(i/SR)/tau) for i in range(n)]

def norm(x, peak):
    m = max(abs(s) for s in x) or 1
    return [s*peak/m for s in x]

def fade_tail(x, ms=25):
    n=int(SR*ms/1000)
    for i in range(n):
        x[-1-i] *= i/n
    return x

def write(path, x):
    w=wave.open(path,'wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(b''.join(struct.pack('<h', int(max(-1,min(1,s))*32767)) for s in x)); w.close()

# --- navegar: toque de dedo em madeira (ruído curto -> ressonâncias de corpo)
def tap():
    n = int(SR*0.09)
    exc = [0.0]*n
    for i in range(int(SR*0.003)):
        exc[i] = (random.random()*2-1) * (0.5-0.5*math.cos(2*math.pi*i/(SR*0.003)))
    body = [0.0]*n
    for f,q,g in ((1150,9,1.0),(2300,7,0.45),(420,5,0.9),(3600,6,0.15)):
        r = biquad_bp(exc, f, q)
        for i in range(n): body[i]+=g*r[i]
    e = env_exp(n, 0.018, 0.0008)
    x = [body[i]*e[i] for i in range(n)]
    x = lowpass(x, 5200)
    x = reverb(x, wet=0.12, length=0.18)
    return fade_tail(norm(x, 0.55))

# --- marimba macia (parciais reais de barra de madeira: 1, 3.93, 9.2)
def marimba(f0, dur=0.55):
    n = int(SR*dur)
    parts = ((1.0,1.0,0.32),(3.93,0.30,0.09),(9.2,0.06,0.035),(2.0,0.05,0.12))
    x=[0.0]*n
    for mult,g,tau in parts:
        w=2*math.pi*f0*mult/SR
        for i in range(n):
            t=i/SR
            x[i]+=g*math.sin(w*i)*math.exp(-t/tau)
    # baqueta de feltro: ataque suave + pouco ruído filtrado
    att=int(SR*0.004)
    for i in range(n):
        x[i]*= (0.5-0.5*math.cos(math.pi*min(1,i/att)))
    mallet=[(random.random()*2-1)*math.exp(-(i/SR)/0.004) for i in range(n)]
    mallet=biquad_bp(mallet, f0*2.5, 3)
    x=[x[i]+0.08*mallet[i] for i in range(n)]
    x=lowpass(x, 4500)
    x=reverb(x, wet=0.18, length=0.35)
    return fade_tail(norm(x, 0.6), 60)

out = sys.argv[1]
write(out+'/tick.wav', tap())
write(out+'/select.wav', marimba(392.0))   # sol
write(out+'/back.wav', marimba(293.66, 0.5))  # ré
print('ok')
