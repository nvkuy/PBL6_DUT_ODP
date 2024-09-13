public class GlobalErrorCorrecter {

    /*
     * using Reed–Solomon code to calculate lost packet in file
     * https://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction
     * */

    static final long MOD = Const.MOD998244353;
    static final ModArithmetic MA = ModArithmetic998244353.INSTANCE;
    static final Convolution cnv = new ConvolutionNTTPrime(MA);
    static final ModPolynomialFactory mpf = new ModPolynomialFactory(cnv);
    static final ModArithmetic ma = mpf.ma;



    // UNIT TEST
//    public static void main(String[] args) {
//
//    }

}


final class ConvolutionNTTPrime extends Convolution {
    public final int PRIMITIVE_ROOT;

    private final long[] SUM_E;
    private final long[] SUM_IE;

    public ConvolutionNTTPrime(ModArithmetic MA) {
        super(MA);
        this.PRIMITIVE_ROOT = primitiveRoot(MA);
        this.SUM_E = new long[30];
        this.SUM_IE = new long[30];
        buildSum();
    }
    public ModArithmetic ma() {
        return MA;
    }
    public long[] convolution(long[] a, long[] b, int deg) {
        int n = Math.min(a.length, deg + 1);
        int m = Math.min(b.length, deg + 1);
        if (n <= 0 || m <= 0) return new long[]{0};
        if (Math.max(n, m) <= THRESHOLD_NAIVE_CONVOLUTION) {
            return convolutionNaive(a, b, n, m);
        }

        int z = 1 << ceilPow2(n + m - 1);
        {
            long[] na = new long[z];
            long[] nb = new long[z];
            System.arraycopy(a, 0, na, 0, n);
            System.arraycopy(b, 0, nb, 0, m);
            a = na;
            b = nb;
        }

        butterfly(a);
        butterfly(b);
        for (int i = 0; i < z; i++) {
            a[i] = MA.mul(a[i], b[i]);
        }
        butterflyInv(a);
        a = java.util.Arrays.copyOf(a, n + m - 1);

        long iz = MA.pow(z, MOD - 2);
        for (int i = 0; i < n + m - 1; i++) a[i] = MA.mul(a[i], iz);
        return a;
    }
    private void butterfly(long[] a) {
        int h = ceilPow2(a.length);
        for (int ph = 1; ph <= h; ph++) {
            int w = 1 << (ph - 1), p = 1 << (h - ph);
            long now = 1;
            for (int s = 0; s < w; s++) {
                int offset = s << (h - ph + 1);
                for (int i = 0; i < p; i++) {
                    long l = a[i + offset];
                    long r = MA.mul(a[i + offset + p], now);
                    a[i + offset] = MA.add(l, r);
                    a[i + offset + p] = MA.sub(l, r);
                }
                int x = Integer.numberOfTrailingZeros(~s);
                now = MA.mul(now, SUM_E[x]);
            }
        }
    }
    private void butterflyInv(long[] a) {
        int h = ceilPow2(a.length);
        for (int ph = h; ph >= 1; ph--) {
            int w = 1 << (ph - 1), p = 1 << (h - ph);
            long inow = 1;
            for (int s = 0; s < w; s++) {
                int offset = s << (h - ph + 1);
                for (int i = 0; i < p; i++) {
                    long l = a[i + offset];
                    long r = a[i + offset + p];
                    a[i + offset] = MA.add(l, r);
                    a[i + offset + p] = MA.mul(MA.sub(l, r), inow);
                }
                int x = Integer.numberOfTrailingZeros(~s);
                inow = MA.mul(inow, SUM_IE[x]);
            }
        }
    }
    private void buildSum() {
        int tlz = Integer.numberOfTrailingZeros(MOD - 1);
        long e = MA.pow(PRIMITIVE_ROOT, (MOD - 1) >> tlz);
        long ie = MA.pow(e, MOD - 2);
        long[] es = new long[30];
        long[] ies = new long[30];
        for (int i = tlz; i >= 2; i--) {
            es[i - 2] = e;
            ies[i - 2] = ie;
            e = MA.mul(e, e);
            ie = MA.mul(ie, ie);
        }
        long now;
        now = 1;
        for (int i = 0; i < tlz - 2; i++) {
            SUM_E[i] = MA.mul(es[i], now);
            now = MA.mul(now, ies[i]);
        }
        now = 1;
        for (int i = 0; i < tlz - 2; i++) {
            SUM_IE[i] = MA.mul(ies[i], now);
            now = MA.mul(now, es[i]);
        }
    }
    private static int ceilPow2(int n) {
        int x = 0;
        while (1L << x < n) x++;
        return x;
    }
    private static int primitiveRoot(final ModArithmetic MA) {
        final int m = (int) MA.getMod();
        if (m == 2) return 1;
        if (m == 167772161) return 3;
        if (m == 469762049) return 3;
        if (m == 754974721) return 11;
        if (m == 998244353) return 3;
        int[] divs = new int[20];
        divs[0] = 2;
        int cnt = 1;
        int x = (m - 1) >> 1;
        while ((x & 1) == 0) x >>= 1;
        for (int i = 3; (long) i * i <= x; i += 2) {
            if (x % i == 0) {
                divs[cnt++] = i;
                while (x % i == 0) x /= i;
            }
        }
        if (x > 1) divs[cnt++] = x;
        Outer : for (int g = 2; ; g++) {
            for (int i = 0; i < cnt; i++) {
                if (MA.pow(g, (m - 1) / divs[i]) == 1) {
                    continue Outer;
                }
            }
            return g;
        }
    }
}


final class ModArithmetic998244353 implements ModArithmetic {
    public static final ModArithmetic INSTANCE = new ModArithmetic998244353();
    private ModArithmetic998244353(){}
    private static final long MOD = Const.MOD998244353;
    public long getMod() {return MOD;}
    public long mod(long a) {return (a %= MOD) < 0 ? a + MOD : a;}
    public long add(long a, long b) {return (a += b) >= MOD ? a - MOD : a;}
    public long sub(long a, long b) {return (a -= b) < 0 ? a + MOD : a;}
    public long mul(long a, long b) {return (a * b) % MOD;}
    public long inv(long a) {
        a = mod(a);
        long b = MOD;
        long u = 1, v = 0;
        while (b >= 1) {
            long t = a / b;
            a -= t * b;
            long tmp1 = a; a = b; b = tmp1;
            u -= t * v;
            long tmp2 = u; u = v; v = tmp2;
        }
        // if (a != 1) throw new ArithmeticException("divide by zero");
        return mod(u);
    }
    public long pow(long a, long b) {
        a = mod(a);
        long pow = 1;
        for (long p = a, c = 1; b > 0;) {
            long lsb = b & -b;
            while (lsb != c) {
                c <<= 1;
                p = (p * p) % MOD;
            }
            pow = (pow * p) % MOD;
            b ^= lsb;
        }
        return pow;
    }
}


final class ModPolynomialFactory {
    public final Convolution cnv;
    public final ModArithmetic ma;

    public ModPolynomialFactory(Convolution cnv) {
        this.cnv = cnv;
        this.ma = cnv.MA;
    }

    public ModPolynomial create(long[] c, int n) {
        return new ModPolynomial(_cut(_mod(c), n));
    }

    public ModPolynomial create(long[] c) {
        return new ModPolynomial(_normalize(_mod(c)));
    }

    public ModPolynomial interpolate(long[] xs, long[] ys) {
        final long[] ZERO = new long[]{0};
        final long[] ONE = new long[]{1};
        final int N = xs.length;
        int k = 1;
        while (k < N) k <<= 1;
        long[][] seg = new long[k << 1][];
        long[][] g = new long[k << 1][];
        for (int i = 0; i < N; i++) {
            seg[k + i] = new long[]{ma.mod(-xs[i]), 1};
        }
        for (int i = N; i < k; i++) {
            seg[k + i] = ONE;
        }
        for (int i = k - 1; i > 0; i--) {
            seg[i] = _mul(seg[i << 1], seg[i << 1 | 1]);
        }
        g[1] = _polyMod(_differentiate(seg[1]), seg[1]);
        for (int i = 2; i < k + N; i++) {
            g[i] = _polyMod(g[i >> 1], seg[i]);
        }
        for (int i = 0; i < N; i++) {
            g[k + i] = new long[]{ma.div(ma.mod(ys[i]), g[k + i][0])};
        }
        for (int i = N; i < k; i++) {
            g[k + i] = ZERO;
        }
        for (int i = k - 1; i > 0; i--) {
            g[i] = _add(_mul(g[i << 1], seg[i << 1 | 1]), _mul(g[i << 1 | 1], seg[i << 1]));
        }
        return new ModPolynomial(g[1]);
    }

    private long[] _mod(long[] f) {
        int deg = f.length - 1;
        long[] fmod = new long[deg + 1];
        for (int i = 0; i <= deg; i++) {
            fmod[i] = ma.mod(f[i]);
        }
        return fmod;
    }

    private long[] _normalize(long[] f) {
        int degF = f.length - 1;
        if (f[degF] != 0) return f;
        while (degF > 0 && f[degF] == 0) {
            degF--;
        }
        return java.util.Arrays.copyOf(f, degF + 1);
    }

    private long[] _cut(long[] f, int deg) {
        deg = Math.min(f.length - 1, deg);
        while (deg > 0 && f[deg] == 0) {
            deg--;
        }
        return java.util.Arrays.copyOf(f, deg + 1);
    }

    private long[] _add(long[] f, long[] g) {
        final int degF = f.length - 1, degG = g.length - 1;
        int deg = Math.max(degF, degG);
        long[] res = java.util.Arrays.copyOf(f, deg + 1);
        for (int i = 0; i <= degG; i++) {
            res[i] = ma.add(res[i], g[i]);
        }
        return _normalize(res);
    }

    private long[] _sub(long[] f, long[] g) {
        final int degF = f.length - 1, degG = g.length - 1;
        int deg = Math.max(degF, degG);
        long[] res = java.util.Arrays.copyOf(f, deg + 1);
        for (int i = 0; i <= degG; i++) {
            res[i] = ma.sub(res[i], g[i]);
        }
        return _normalize(res);
    }

    private long[] _mul(long[] f, long[] g) {
        return _normalize(cnv.convolution(f, g));
    }

    private long[] _mul(long[] f, long[] g, int deg) {
        long[] h = cnv.convolution(f, g, deg);
        int degH = h.length - 1;
        if (degH <= deg) {
            return _normalize(h);
        } else {
            return _cut(h, deg);
        }
    }

    private long[] _muli(long[] f, long a) {
        a = ma.mod(a);
        if (a == 0) return new long[]{0};
        int deg = f.length - 1;
        long[] res = new long[deg + 1];
        for (int i = 0; i <= deg; i++) {
            res[i] = ma.mul(a, f[i]);
        }
        return _normalize(res);
    }

    private long[] _div(long[] f, long[] g, int deg) {
        return _mul(f, _inv(g, deg), deg);
    }

    private long[] _inv(long[] f, int deg) {
        long[] inv = new long[]{ma.inv(f[0])};
        for (int k = 1; k <= deg;) {
            k <<= 1;
            inv = _sub(_muli(inv, 2), _mul(_mul(inv, inv, k), f, k));
        }
        return _cut(inv, deg);
    }

    private long[] _differentiate(long[] f) {
        int deg = f.length - 1;
        if (deg == 0) return new long[]{0};
        long[] diff = new long[deg];
        for (int i = 1; i <= deg; i++) {
            diff[i - 1] = ma.mul(f[i], i);
        }
        return _normalize(diff);
    }

    private long[] _integrate(long[] f) {
        int deg = f.length - 1;
        long[] intg = new long[deg + 2];
        long[] invs = ma.rangeInv(deg + 1);
        for (int i = 0; i <= deg; i++) {
            intg[i + 1] = ma.mul(f[i], invs[i + 1]);
        }
        return _normalize(intg);
    }

    private long[] _log(long[] f, int deg) {
        long[] difF = _differentiate(f);
        long[] invF = _inv(f, deg);
        return _integrate(_mul(difF, invF, deg - 1));
    }

    private long[] _exp(long[] f, int deg) {
        long[] g = new long[]{1};
        int k = 1;
        while (k <= deg) {
            k <<= 1;
            long[] tmp = _sub(_cut(f, k), _log(g, k));
            tmp[0] = ma.add(tmp[0], 1);
            g = _mul(g, tmp, k);
        }
        return _cut(g, deg);
    }

    private long[] _pow(long[] f, long k, int deg) {
        int tlz = 0;
        while (tlz < f.length && f[tlz] == 0) {
            tlz++;
        }
        if (tlz * k >= f.length) {
            return new long[]{0};
        }
        long[] g = java.util.Arrays.copyOfRange(f, tlz, f.length);
        long base = g[0];
        g = _muli(g, ma.inv(base));
        long[] h = _muli(_exp(_muli(_log(g, deg), k), deg), ma.pow(base, k));
        long[] c = new long[deg + 1];
        int zeros = (int) (tlz * k);
        System.arraycopy(h, 0, c, zeros, deg + 1 - zeros);
        return _cut(c, deg);
    }

    private java.util.Optional<long[]> _sqrt(long[] f, int deg) {
        int tlz = 0;
        while (tlz < f.length && f[tlz] == 0) {
            tlz++;
        }
        if (tlz == f.length) {
            return java.util.Optional.of(new long[]{0});
        }
        java.util.OptionalLong ops = ma.sqrt(f[tlz]);
        if ((tlz & 1) == 1 || ops.isEmpty()) {
            return java.util.Optional.empty();
        }
        long sq = ops.getAsLong();
        long[] g = java.util.Arrays.copyOfRange(f, tlz, f.length);
        g = _muli(_exp(_muli(_log(_muli(g, ma.inv(g[0])), deg), ma.inv(2)), deg), sq);
        long[] sqrt = new long[tlz / 2 + g.length];
        System.arraycopy(g, 0, sqrt, tlz / 2, g.length);
        return java.util.Optional.of(_cut(sqrt, deg));
    }

    /**
     * @return f(x + c)
     */
    private long[] _translate(long[] f, long c) {
        final int degF = f.length - 1;
        c = ma.mod(c);
        long[] pow = ma.rangePower(c, degF);
        long[] fac = ma.factorial(degF);
        long[] ifac = ma.factorialInv(degF);
        long[] expc = new long[degF + 1];
        long[] g = new long[degF + 1];
        for (int i = 0; i <= degF; i++) {
            g[degF - i] = ma.mul(f[i], fac[i]);
            expc[i] = ma.mul(pow[i], ifac[i]);
        }
        long[] prd = _mul(g, expc, degF);
        long[] h = new long[degF + 1];
        for (int i = 0; i <= degF; i++) {
            h[i] = ma.mul(ifac[i], prd[degF - i]);
        }
        return _normalize(h);
    }

    private long[] naivePolyDiv(long[] f, long[] g) {
        final int degF = f.length - 1, degG = g.length - 1, K = degF - degG;
        final long head = g[degG];
        long[] a = f.clone();
        long[] q = new long[K + 1];
        for (int i = K; i >= 0; i--) {
            long div = ma.div(a[degG + i], head);
            q[i] = div;
            for (int j = 0; j <= degG; j++) {
                a[i + j] = ma.sub(a[i + j], ma.mul(div, g[j]));
            }
        }
        return _normalize(q);
    }

    private static final int THRESHOLD_NAIVE_POLY_DIV = 256;

    private long[] _polyDiv(long[] f, long[] g) {
        final int degF = f.length - 1, degG = g.length - 1;
        if (degF < degG) {
            return new long[]{0};
        }
        if (degG == 0) {
            return _muli(f, ma.inv(g[0]));
        }
        if (degG <= THRESHOLD_NAIVE_POLY_DIV) {
            return naivePolyDiv(f, g);
        }
        int deg = degF - degG;
        long[] revF = new long[degF + 1];
        for (int i = 0; i <= degF; i++) {
            revF[i] = f[degF - i];
        }
        long[] revG = new long[degG + 1];
        for (int i = 0; i <= degG; i++) {
            revG[i] = g[degG - i];
        }
        long[] revH = cnv.convolution(revF, _inv(revG, deg));
        long[] res = new long[deg + 1];
        for (int i = 0; i <= deg; i++) {
            res[deg - i] = revH[i];
        }
        return _normalize(res);
    }

    private long[] _polyMod(long[] f, long[] g) {
        if (f.length < g.length) {
            return f.clone();
        }
        return _sub(f, _mul(_polyDiv(f, g), g));
    }

    private long[] _multipointEval(long[] f, long[] xs) {
        final long[] ONE = new long[]{1};
        int m = xs.length;
        int k = 1;
        while (k < m) k <<= 1;
        long[][] seg = new long[k << 1][];
        for (int i = 0; i < m; i++) {
            seg[k + i] = new long[]{ma.mod(-xs[i]), 1};
        }
        for (int i = m; i < k; i++) {
            seg[k + i] = ONE;
        }
        for (int i = k - 1; i > 0; i--) {
            seg[i] = _mul(seg[i << 1], seg[i << 1 | 1]);
        }
        seg[1] = _polyMod(f, seg[1]);
        for (int i = 2; i < k + m; i++) {
            seg[i] = _polyMod(seg[i >> 1], seg[i]);
        }
        long[] ys = new long[m];
        for (int i = 0; i < m; i++) {
            ys[i] = seg[k + i][0];
        }
        return ys;
    }

    public final class ModPolynomial implements ArithmeticOperations<ModPolynomial> {
        private final int N;
        private final long[] C;
        private ModPolynomial(long[] c) {
            this.C = c;
            this.N = c.length;
        }
        public long apply(long x) {
            long ret = C[N - 1];
            for (int i = N - 2; i >= 0; i--) ret = ma.mod(ret * x + C[i]);
            return ret;
        }
        public ModPolynomial expand(int deg) {
            return new ModPolynomial(java.util.Arrays.copyOf(C, deg + 1));
        }
        public ModPolynomial cut(int deg) {
            return new ModPolynomial(_cut(C, deg));
        }
        public ModPolynomial add(ModPolynomial f) {
            return new ModPolynomial(_add(C, f.C));
        }
        public ModPolynomial sub(ModPolynomial f) {
            return new ModPolynomial(_sub(C, f.C));
        }
        public ModPolynomial mul(ModPolynomial f) {
            return new ModPolynomial(_mul(C, f.C));
        }
        public ModPolynomial mul(ModPolynomial f, int deg) {
            return new ModPolynomial(_mul(C, f.C, deg));
        }
        public ModPolynomial muli(long a) {
            return new ModPolynomial(_muli(C, a));
        }
        public ModPolynomial div(ModPolynomial f) {
            return div(f, N - 1);
        }
        public ModPolynomial div(ModPolynomial f, int deg) {
            return new ModPolynomial(_div(C, f.C, deg));
        }
        public ModPolynomial inv(int deg) {
            return new ModPolynomial(_inv(C, deg));
        }
        public ModPolynomial differentiate() {
            return new ModPolynomial(_differentiate(C));
        }
        public ModPolynomial integrate() {
            return new ModPolynomial(_integrate(C));
        }
        public ModPolynomial log(int deg) {
            return new ModPolynomial(_log(C, deg));
        }
        public ModPolynomial exp(int deg) {
            return new ModPolynomial(_exp(C, deg));
        }
        public ModPolynomial pow(long n, int deg) {
            return new ModPolynomial(_pow(C, n, deg));
        }
        public java.util.Optional<ModPolynomial> sqrt(int deg) {
            java.util.Optional<long[]> sqrt = _sqrt(C, deg);
            if (sqrt.isEmpty()) {
                return java.util.Optional.empty();
            } else {
                return java.util.Optional.of(new ModPolynomial(sqrt.get()));
            }
        }
        /**
         * @return f(x + c)
         */
        public ModPolynomial translate(long c) {
            return new ModPolynomial(_translate(C, c));
        }
        public ModPolynomial polyDiv(ModPolynomial f) {
            return new ModPolynomial(_polyDiv(C, f.C));
        }
        public ModPolynomial polyMod(ModPolynomial f) {
            return new ModPolynomial(_polyMod(C, f.C));
        }
        public long[] multipointEval(long[] xs) {
            return _multipointEval(C, xs);
        }
        public long getCoef(int deg) {
            return C[deg];
        }
        public long[] getCoefs(){
            return C;
        }
    }
}


interface ArithmeticOperations<T> {
    public T add(T t);
    public T sub(T t);
    public T mul(T t);
    public T div(T t);
}

class Const {
    public static final long   LINF   = 1L << 59;
    public static final int    IINF   = (1  << 30) - 1;
    public static final double DINF   = 1e150;

    public static final double SMALL  = 1e-12;
    public static final double MEDIUM = 1e-9;
    public static final double LARGE  = 1e-6;

    public static final long MOD1000000007 = 1000000007;
    public static final long MOD998244353  = 998244353 ;
    public static final long MOD754974721  = 754974721 ;
    public static final long MOD167772161  = 167772161 ;
    public static final long MOD469762049  = 469762049 ;

    public static final int[] dx8 = {1, 0, -1, 0, 1, -1, -1, 1};
    public static final int[] dy8 = {0, 1, 0, -1, 1, 1, -1, -1};
    public static final int[] dx4 = {1, 0, -1, 0};
    public static final int[] dy4 = {0, 1, 0, -1};
}


abstract class Convolution {
    static final int THRESHOLD_NAIVE_CONVOLUTION = 150;
    public final ModArithmetic MA;
    public final int MOD;
    Convolution(ModArithmetic MA) {
        this.MA = MA;
        this.MOD = (int) MA.getMod();
    }
    public abstract long[] convolution(long[] a, long[] b, int deg);
    public long[] convolution(long[] a, long[] b) {
        return convolution(a, b, Const.IINF);
    }
    long[] convolutionNaive(long[] a, long[] b, int n, int m) {
        int k = n + m - 1;
        long[] ret = new long[k];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) {
            ret[i + j] += MA.mul(a[i], b[j]);
        }
        for (int i = 0; i < k; i++) {
            ret[i] = MA.mod(ret[i]);
        }
        return ret;
    }
}

/**
 * @author https://atcoder.jp/users/suisen
 */
interface ModArithmetic {
    public long getMod();
    public long mod(long a);
    public long add(long a, long b);
    public long sub(long a, long b);
    public long mul(long a, long b);
    public long inv(long a);
    public long pow(long a, long b);
    public default long add(long a, long b, long c) {
        return add(a, add(b, c));
    }
    public default long add(long a, long b, long c, long d) {
        return add(a, add(b, add(c, d)));
    }
    public default long add(long a, long b, long c, long d, long e) {
        return add(a, add(b, add(c, add(d, e))));
    }
    public default long add(long a, long b, long c, long d, long e, long f) {
        return add(a, add(b, add(c, add(d, add(e, f)))));
    }
    public default long add(long a, long b, long c, long d, long e, long f, long g) {
        return add(a, add(b, add(c, add(d, add(e, add(f, g))))));
    }
    public default long add(long a, long b, long c, long d, long e, long f, long g, long h) {
        return add(a, add(b, add(c, add(d, add(e, add(f, add(g, h)))))));
    }
    public default long add(long... xs) {
        long s = 0;
        for (long x : xs) s += x;
        return mod(s);
    }
    public default long mul(long a, long b, long c) {
        return mul(a, mul(b, c));
    }
    public default long mul(long a, long b, long c, long d) {
        return mul(a, mul(b, mul(c, d)));
    }
    public default long mul(long a, long b, long c, long d, long e) {
        return mul(a, mul(b, mul(c, mul(d, e))));
    }
    public default long mul(long a, long b, long c, long d, long e, long f) {
        return mul(a, mul(b, mul(c, mul(d, mul(e, f)))));
    }
    public default long mul(long a, long b, long c, long d, long e, long f, long g) {
        return mul(a, mul(b, mul(c, mul(d, mul(e, mul(f, g))))));
    }
    public default long mul(long a, long b, long c, long d, long e, long f, long g, long h) {
        return mul(a, mul(b, mul(c, mul(d, mul(e, mul(f, mul(g, h)))))));
    }
    public default long mul(long... xs) {
        long s = 1;
        for (long x : xs) s = mul(s, x);
        return s;
    }
    public default long div(long a, long b) {
        return mul(a, inv(b));
    }
    public default java.util.OptionalLong sqrt(long a) {
        a = mod(a);
        if (a == 0) return java.util.OptionalLong.of(0);
        if (a == 1) return java.util.OptionalLong.of(1);
        long p = getMod();
        if (pow(a, (p - 1) >> 1) != 1) return java.util.OptionalLong.empty();
        if ((p & 3) == 3) {
            return java.util.OptionalLong.of(pow(a, (p + 1) >> 2));
        }
        if ((p & 7) == 5) {
            if (pow(a, (p - 1) >> 2) == 1) {
                return java.util.OptionalLong.of(pow(a, (p + 3) >> 3));
            } else {
                return java.util.OptionalLong.of(mul(pow(2, (p - 1) >> 2), pow(a, (p + 3) >> 3)));
            }
        }
        long S = 0;
        long Q = p - 1;
        while ((Q & 1) == 0) {
            ++S;
            Q >>= 1;
        }
        long z = 1;
        while (pow(z, (p - 1) >> 1) != p - 1) ++z;
        long c = pow(z, Q);
        long R = pow(a, (Q + 1) / 2);
        long t = pow(a, Q);
        long M = S;
        while (t != 1) {
            long cur = t;
            int i;
            for (i = 1; i < M; i++) {
                cur = mul(cur, cur);
                if (cur == 1) break;
            }
            long b = pow(c, 1L << (M - i - 1));
            R = mul(R, b);
            t = mul(t, b, b);
            c = mul(b, b);
            M = i;
        }
        return java.util.OptionalLong.of(R);
    }

    /** array operations */

    public default long[] rangeInv(int n) {
        final long MOD = getMod();
        if (n >= MOD) throw new ArithmeticException("divide by zero");
        long[] invs = new long[n + 1];
        if (n == 0) return invs;
        invs[1] = 1;
        for (int i = 2; i <= n; i++) {
            long q = MOD - MOD / i;
            long r = invs[(int) (MOD % i)];
            invs[i] = mul(q, r);
        }
        return invs;
    }
    public default long[] arrayInv(long[] a) {
        int n = a.length;
        long[] dp = new long[n + 1];
        long[] pd = new long[n + 1];
        dp[0] = pd[n] = 1;
        for (int i = 0; i < n; i++) dp[i + 1] = mul(dp[i], a[i    ]);
        for (int i = n; i > 0; i--) pd[i - 1] = mul(pd[i], a[i - 1]);
        long inv = inv(dp[n]);
        long[] invs = new long[n];
        for (int i = 0; i < n; i++) {
            long lr = mul(dp[i], pd[i + 1]);
            invs[i] = mul(lr, inv);
        }
        return invs;
    }
    public default long[] factorial(int n) {
        long[] ret = new long[n + 1];
        ret[0] = 1;
        for (int i = 1; i <= n; i++) ret[i] = mul(ret[i - 1], i);
        return ret;
    }
    public default long[] factorialInv(int n) {
        long facN = 1;
        for (int i = 2; i <= n; i++) facN = mul(facN, i);
        long[] invs = new long[n + 1];
        invs[n] = inv(facN);
        for (int i = n; i > 0; i--) invs[i - 1] = mul(invs[i], i);
        return invs;
    }
    public default long[] rangePower(long a, int n) {
        a = mod(a);
        long[] pows = new long[n + 1];
        pows[0] = 1;
        for (int i = 1; i <= n; i++) pows[i] = mul(pows[i - 1], a);
        return pows;
    }

    /** combinatric operations */

    public default long[][] combTable(int n) {
        long[][] comb = new long[n + 1][];
        for (int i = 0; i <= n; i++) {
            comb[i] = new long[i + 1];
            comb[i][0] = comb[i][i] = 1;
            for (int j = 1; j < i; j++) {
                comb[i][j] = add(comb[i - 1][j - 1], comb[i - 1][j]);
            }
        }
        return comb;
    }
    public default long comb(int n, int r, long[] factorial, long[] invFactorial) {
        if (r < 0 || r > n) return 0;
        long inv = mul(invFactorial[r], invFactorial[n - r]);
        return mul(factorial[n], inv);
    }
    public default long naiveComb(long n, long r) {
        if (r < 0 || r > n) return 0;
        r = Math.min(r, n - r);
        if (r == 0) return 1;
        long res = 1;
        long[] invs = rangeInv(Math.toIntExact(r));
        for (int d = 1; d <= r; d++) {
            res = mul(res, n--, invs[d]);
        }
        return res;
    }
    public default long perm(int n, int r, long[] factorial, long[] invFactorial) {
        if (r < 0 || r > n) return 0;
        return mul(factorial[n], invFactorial[n - r]);
    }
    public default long naivePerm(long n, long r) {
        if (r < 0 || r > n) return 0;
        long res = 1;
        for (long i = n - r + 1; i <= n; i++) res = mul(res, i);
        return res;
    }
}
