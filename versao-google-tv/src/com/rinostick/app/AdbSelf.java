package com.rinostick.app;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Cliente ADB mínimo que conversa com o adbd da própria TV (127.0.0.1:5555).
 * Permite que o app libere as próprias permissões (o que normalmente exige um
 * computador): o usuário só liga a "Depuração ADB" e aceita o aviso na TV.
 */
final class AdbSelf implements AutoCloseable {

    interface Listener { void onWaitingApproval(); }

    static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f,
            OKAY = 0x59414b4f, CLSE = 0x45534c43, WRTE = 0x45545257;
    static final int VERSION = 0x01000000, MAXDATA = 256 * 1024;
    static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_RSAPUBLICKEY = 3;

    final Socket sock;
    final InputStream in;
    final OutputStream out;
    int nextId = 1;

    static final class Msg { int cmd, a0, a1; byte[] data; }

    /** Conecta e autentica. Lança IOException com mensagem amigável em português. */
    AdbSelf(Context c, Listener l) throws Exception {
        sock = new Socket();
        try {
            sock.connect(new InetSocketAddress("127.0.0.1", 5555), 4000);
        } catch (IOException e) {
            throw new IOException("A Depuração ADB está desligada.");
        }
        sock.setSoTimeout(15000);
        in = sock.getInputStream();
        out = sock.getOutputStream();

        KeyPair kp = keys(c);
        send(CNXN, VERSION, MAXDATA, "host::\0".getBytes(StandardCharsets.UTF_8));
        boolean sentSig = false, sentKey = false;
        while (true) {
            Msg m;
            try {
                m = read();
            } catch (java.net.SocketTimeoutException e) {
                throw new IOException(sentKey ? "O aviso na TV não foi aceito a tempo." : "A TV não respondeu na porta 5555.");
            } catch (EOFException e) {
                throw new IOException(sentKey ? "O aviso na TV foi recusado." : "Conexão recusada pela TV.");
            }
            if (m.cmd == CNXN) break;
            if (m.cmd != AUTH || m.a0 != AUTH_TOKEN) continue;
            if (!sentSig) {
                send(AUTH, AUTH_SIGNATURE, 0, sign(kp.getPrivate(), m.data));
                sentSig = true;
            } else if (!sentKey) {
                send(AUTH, AUTH_RSAPUBLICKEY, 0, publicKey((RSAPublicKey) kp.getPublic()));
                sentKey = true;
                sock.setSoTimeout(120000); // tempo para aceitar o aviso na TV
                if (l != null) l.onWaitingApproval();
            } else {
                throw new IOException("O aviso na TV foi recusado.");
            }
        }
        sock.setSoTimeout(30000);
    }

    /** Roda um comando de shell e devolve a saída. */
    String shell(String cmd) throws IOException {
        int local = nextId++, remote = 0;
        send(OPEN, local, 0, ("shell:" + cmd + "\0").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        while (true) {
            Msg m = read();
            if (m.a1 != local) continue;
            if (m.cmd == OKAY) {
                remote = m.a0;
            } else if (m.cmd == WRTE) {
                res.write(m.data);
                send(OKAY, local, m.a0, null);
            } else if (m.cmd == CLSE) {
                if (remote != 0) send(CLSE, local, remote, null);
                break;
            }
        }
        return new String(res.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override public void close() {
        try { sock.close(); } catch (IOException ignored) { }
    }

    // ------------------------------------------------------------------ protocolo

    void send(int cmd, int a0, int a1, byte[] data) throws IOException {
        int len = data == null ? 0 : data.length, sum = 0;
        if (data != null) for (byte b : data) sum += b & 0xff;
        ByteBuffer h = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        h.putInt(cmd).putInt(a0).putInt(a1).putInt(len).putInt(sum).putInt(~cmd);
        out.write(h.array());
        if (len > 0) out.write(data);
        out.flush();
    }

    Msg read() throws IOException {
        byte[] hb = readFully(24);
        ByteBuffer h = ByteBuffer.wrap(hb).order(ByteOrder.LITTLE_ENDIAN);
        Msg m = new Msg();
        m.cmd = h.getInt();
        m.a0 = h.getInt();
        m.a1 = h.getInt();
        int len = h.getInt();
        m.data = len > 0 ? readFully(len) : new byte[0];
        return m;
    }

    byte[] readFully(int n) throws IOException {
        byte[] b = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(b, off, n - off);
            if (r < 0) throw new EOFException();
            off += r;
        }
        return b;
    }

    // ------------------------------------------------------------------ chave RSA

    static KeyPair keys(Context c) throws Exception {
        File pf = new File(c.getFilesDir(), "adbkey"), uf = new File(c.getFilesDir(), "adbkey.pub");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        if (pf.exists() && uf.exists()) {
            PrivateKey pr = kf.generatePrivate(new PKCS8EncodedKeySpec(readFile(pf)));
            PublicKey pu = kf.generatePublic(new X509EncodedKeySpec(readFile(uf)));
            return new KeyPair(pu, pr);
        }
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        writeFile(pf, kp.getPrivate().getEncoded());
        writeFile(uf, kp.getPublic().getEncoded());
        return kp;
    }

    static final byte[] SHA1_DIGEST_INFO = {
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    /** Assinatura PKCS#1 v1.5 do token (o adbd trata o token como um SHA-1 já calculado). */
    static byte[] sign(PrivateKey key, byte[] token) throws Exception {
        byte[] block = new byte[256];
        int t = SHA1_DIGEST_INFO.length + token.length;
        block[0] = 0;
        block[1] = 1;
        for (int i = 2; i < 256 - t - 1; i++) block[i] = (byte) 0xff;
        block[256 - t - 1] = 0;
        System.arraycopy(SHA1_DIGEST_INFO, 0, block, 256 - t, SHA1_DIGEST_INFO.length);
        System.arraycopy(token, 0, block, 256 - token.length, token.length);
        Cipher ci = Cipher.getInstance("RSA/ECB/NoPadding");
        ci.init(Cipher.ENCRYPT_MODE, key);
        return ci.doFinal(block);
    }

    /** Chave pública no formato do Android (struct RSAPublicKey em base64) + nome. */
    static byte[] publicKey(RSAPublicKey pub) {
        BigInteger n = pub.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = r32.subtract(n.mod(r32).modInverse(r32));
        BigInteger rr = BigInteger.ONE.shiftLeft(4096).mod(n);
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(64);
        bb.putInt(n0inv.intValue());
        putLE(bb, n);
        putLE(bb, rr);
        bb.putInt(pub.getPublicExponent().intValue());
        String b64 = Base64.encodeToString(bb.array(), Base64.NO_WRAP);
        return (b64 + " RinoStick@TV\0").getBytes(StandardCharsets.UTF_8);
    }

    static void putLE(ByteBuffer bb, BigInteger v) {
        byte[] be = v.toByteArray();
        for (int i = 0; i < 256; i++) bb.put(i < be.length ? be[be.length - 1 - i] : 0);
    }

    static byte[] readFile(File f) throws IOException {
        try (FileInputStream fi = new FileInputStream(f)) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = fi.read(buf)) > 0) o.write(buf, 0, r);
            return o.toByteArray();
        }
    }

    static void writeFile(File f, byte[] d) throws IOException {
        try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(d); }
    }
}
