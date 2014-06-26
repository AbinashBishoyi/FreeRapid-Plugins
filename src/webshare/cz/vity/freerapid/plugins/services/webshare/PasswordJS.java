package cz.vity.freerapid.plugins.services.webshare;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import org.apache.commons.codec.digest.DigestUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author birchie
 */
public class PasswordJS {

    public String encryptPassword(String pass, String salt) throws Exception {
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            if (engine == null)
                throw new RuntimeException("JavaScript engine not found");
            final String md5enc = (String) engine.eval(JavaScript_MD5Crypt + "md5crypt(\"" + pass + "\", \"" + salt + "\")");
            return DigestUtils.shaHex(md5enc);
        } catch (Exception e) {
            throw new ServiceConnectionProblemException("Can't execute javascript");
        }
    }


    // not the best way of including javascript file, but it works
    final private static String JavaScript_MD5Crypt =
            "var ascii64 = \"./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz\";\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "/* (the following code was stripped down from its original form)\n" +
                    " * A JavaScript implementation of the RSA Data Security, Inc. MD5 Message\n" +
                    " * Digest Algorithm, as defined in RFC 1321.\n" +
                    " * Version 2.1 Copyright (C) Paul Johnston 1999 - 2002.\n" +
                    " * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet\n" +
                    " * Distributed under the BSD License\n" +
                    " * See http://pajhome.org.uk/crypt/md5 for more info.\n" +
                    " */\n" +
                    "\n" +
                    "/*\n" +
                    " * Configurable variables. You may need to tweak these to be compatible with\n" +
                    " * the server-side, but the defaults work in most cases.\n" +
                    " */\n" +
                    "var hexcase = 0; /* hex output format. 0 - lowercase; 1 - uppercase        */\n" +
                    "var b64pad = \"\"; /* base-64 pad character. \"=\" for strict RFC compliance   */\n" +
                    "var chrsz = 8; /* bits per input character. 8 - ASCII; 16 - Unicode      */\n" +
                    "\n" +
                    "function str_md5(s)\n" +
                    "{\n" +
                    "  return binl2str(core_md5(str2binl(s), s.length * chrsz));\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * Calculate the MD5 of an array of little-endian words, and a bit length\n" +
                    " */\n" +
                    "function core_md5(x, len)\n" +
                    "{\n" +
                    "  /* append padding */\n" +
                    "  x[len >> 5] |= 0x80 << ((len) % 32);\n" +
                    "  x[(((len + 64) >>> 9) << 4) + 14] = len;\n" +
                    "\n" +
                    "  var a = 1732584193;\n" +
                    "  var b = -271733879;\n" +
                    "  var c = -1732584194;\n" +
                    "  var d = 271733878;\n" +
                    "\n" +
                    "  for (var i = 0; i < x.length; i += 16)\n" +
                    "  {\n" +
                    "    var olda = a;\n" +
                    "    var oldb = b;\n" +
                    "    var oldc = c;\n" +
                    "    var oldd = d;\n" +
                    "\n" +
                    "    a = md5_ff(a, b, c, d, x[i + 0], 7, -680876936);\n" +
                    "    d = md5_ff(d, a, b, c, x[i + 1], 12, -389564586);\n" +
                    "    c = md5_ff(c, d, a, b, x[i + 2], 17, 606105819);\n" +
                    "    b = md5_ff(b, c, d, a, x[i + 3], 22, -1044525330);\n" +
                    "    a = md5_ff(a, b, c, d, x[i + 4], 7, -176418897);\n" +
                    "    d = md5_ff(d, a, b, c, x[i + 5], 12, 1200080426);\n" +
                    "    c = md5_ff(c, d, a, b, x[i + 6], 17, -1473231341);\n" +
                    "    b = md5_ff(b, c, d, a, x[i + 7], 22, -45705983);\n" +
                    "    a = md5_ff(a, b, c, d, x[i + 8], 7, 1770035416);\n" +
                    "    d = md5_ff(d, a, b, c, x[i + 9], 12, -1958414417);\n" +
                    "    c = md5_ff(c, d, a, b, x[i + 10], 17, -42063);\n" +
                    "    b = md5_ff(b, c, d, a, x[i + 11], 22, -1990404162);\n" +
                    "    a = md5_ff(a, b, c, d, x[i + 12], 7, 1804603682);\n" +
                    "    d = md5_ff(d, a, b, c, x[i + 13], 12, -40341101);\n" +
                    "    c = md5_ff(c, d, a, b, x[i + 14], 17, -1502002290);\n" +
                    "    b = md5_ff(b, c, d, a, x[i + 15], 22, 1236535329);\n" +
                    "\n" +
                    "    a = md5_gg(a, b, c, d, x[i + 1], 5, -165796510);\n" +
                    "    d = md5_gg(d, a, b, c, x[i + 6], 9, -1069501632);\n" +
                    "    c = md5_gg(c, d, a, b, x[i + 11], 14, 643717713);\n" +
                    "    b = md5_gg(b, c, d, a, x[i + 0], 20, -373897302);\n" +
                    "    a = md5_gg(a, b, c, d, x[i + 5], 5, -701558691);\n" +
                    "    d = md5_gg(d, a, b, c, x[i + 10], 9, 38016083);\n" +
                    "    c = md5_gg(c, d, a, b, x[i + 15], 14, -660478335);\n" +
                    "    b = md5_gg(b, c, d, a, x[i + 4], 20, -405537848);\n" +
                    "    a = md5_gg(a, b, c, d, x[i + 9], 5, 568446438);\n" +
                    "    d = md5_gg(d, a, b, c, x[i + 14], 9, -1019803690);\n" +
                    "    c = md5_gg(c, d, a, b, x[i + 3], 14, -187363961);\n" +
                    "    b = md5_gg(b, c, d, a, x[i + 8], 20, 1163531501);\n" +
                    "    a = md5_gg(a, b, c, d, x[i + 13], 5, -1444681467);\n" +
                    "    d = md5_gg(d, a, b, c, x[i + 2], 9, -51403784);\n" +
                    "    c = md5_gg(c, d, a, b, x[i + 7], 14, 1735328473);\n" +
                    "    b = md5_gg(b, c, d, a, x[i + 12], 20, -1926607734);\n" +
                    "\n" +
                    "    a = md5_hh(a, b, c, d, x[i + 5], 4, -378558);\n" +
                    "    d = md5_hh(d, a, b, c, x[i + 8], 11, -2022574463);\n" +
                    "    c = md5_hh(c, d, a, b, x[i + 11], 16, 1839030562);\n" +
                    "    b = md5_hh(b, c, d, a, x[i + 14], 23, -35309556);\n" +
                    "    a = md5_hh(a, b, c, d, x[i + 1], 4, -1530992060);\n" +
                    "    d = md5_hh(d, a, b, c, x[i + 4], 11, 1272893353);\n" +
                    "    c = md5_hh(c, d, a, b, x[i + 7], 16, -155497632);\n" +
                    "    b = md5_hh(b, c, d, a, x[i + 10], 23, -1094730640);\n" +
                    "    a = md5_hh(a, b, c, d, x[i + 13], 4, 681279174);\n" +
                    "    d = md5_hh(d, a, b, c, x[i + 0], 11, -358537222);\n" +
                    "    c = md5_hh(c, d, a, b, x[i + 3], 16, -722521979);\n" +
                    "    b = md5_hh(b, c, d, a, x[i + 6], 23, 76029189);\n" +
                    "    a = md5_hh(a, b, c, d, x[i + 9], 4, -640364487);\n" +
                    "    d = md5_hh(d, a, b, c, x[i + 12], 11, -421815835);\n" +
                    "    c = md5_hh(c, d, a, b, x[i + 15], 16, 530742520);\n" +
                    "    b = md5_hh(b, c, d, a, x[i + 2], 23, -995338651);\n" +
                    "\n" +
                    "    a = md5_ii(a, b, c, d, x[i + 0], 6, -198630844);\n" +
                    "    d = md5_ii(d, a, b, c, x[i + 7], 10, 1126891415);\n" +
                    "    c = md5_ii(c, d, a, b, x[i + 14], 15, -1416354905);\n" +
                    "    b = md5_ii(b, c, d, a, x[i + 5], 21, -57434055);\n" +
                    "    a = md5_ii(a, b, c, d, x[i + 12], 6, 1700485571);\n" +
                    "    d = md5_ii(d, a, b, c, x[i + 3], 10, -1894986606);\n" +
                    "    c = md5_ii(c, d, a, b, x[i + 10], 15, -1051523);\n" +
                    "    b = md5_ii(b, c, d, a, x[i + 1], 21, -2054922799);\n" +
                    "    a = md5_ii(a, b, c, d, x[i + 8], 6, 1873313359);\n" +
                    "    d = md5_ii(d, a, b, c, x[i + 15], 10, -30611744);\n" +
                    "    c = md5_ii(c, d, a, b, x[i + 6], 15, -1560198380);\n" +
                    "    b = md5_ii(b, c, d, a, x[i + 13], 21, 1309151649);\n" +
                    "    a = md5_ii(a, b, c, d, x[i + 4], 6, -145523070);\n" +
                    "    d = md5_ii(d, a, b, c, x[i + 11], 10, -1120210379);\n" +
                    "    c = md5_ii(c, d, a, b, x[i + 2], 15, 718787259);\n" +
                    "    b = md5_ii(b, c, d, a, x[i + 9], 21, -343485551);\n" +
                    "\n" +
                    "    a = safe_add(a, olda);\n" +
                    "    b = safe_add(b, oldb);\n" +
                    "    c = safe_add(c, oldc);\n" +
                    "    d = safe_add(d, oldd);\n" +
                    "  }\n" +
                    "  return Array(a, b, c, d);\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * These functions implement the four basic operations the algorithm uses.\n" +
                    " */\n" +
                    "function md5_cmn(q, a, b, x, s, t)\n" +
                    "{\n" +
                    "  return safe_add(bit_rol(safe_add(safe_add(a, q), safe_add(x, t)), s), b);\n" +
                    "}\n" +
                    "function md5_ff(a, b, c, d, x, s, t)\n" +
                    "{\n" +
                    "  return md5_cmn((b & c) | ((~b) & d), a, b, x, s, t);\n" +
                    "}\n" +
                    "function md5_gg(a, b, c, d, x, s, t)\n" +
                    "{\n" +
                    "  return md5_cmn((b & d) | (c & (~d)), a, b, x, s, t);\n" +
                    "}\n" +
                    "function md5_hh(a, b, c, d, x, s, t)\n" +
                    "{\n" +
                    "  return md5_cmn(b ^ c ^ d, a, b, x, s, t);\n" +
                    "}\n" +
                    "function md5_ii(a, b, c, d, x, s, t)\n" +
                    "{\n" +
                    "  return md5_cmn(c ^ (b | (~d)), a, b, x, s, t);\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * Add integers, wrapping at 2^32. This uses 16-bit operations internally\n" +
                    " * to work around bugs in some JS interpreters.\n" +
                    " */\n" +
                    "function safe_add(x, y)\n" +
                    "{\n" +
                    "  var lsw = (x & 0xFFFF) + (y & 0xFFFF);\n" +
                    "  var msw = (x >> 16) + (y >> 16) + (lsw >> 16);\n" +
                    "  return (msw << 16) | (lsw & 0xFFFF);\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * Bitwise rotate a 32-bit number to the left.\n" +
                    " */\n" +
                    "function bit_rol(num, cnt)\n" +
                    "{\n" +
                    "  return (num << cnt) | (num >>> (32 - cnt));\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * Convert a string to an array of little-endian words\n" +
                    " * If chrsz is ASCII, characters >255 have their hi-byte silently ignored.\n" +
                    " */\n" +
                    "function str2binl(str)\n" +
                    "{\n" +
                    "  var bin = Array();\n" +
                    "  var mask = (1 << chrsz) - 1;\n" +
                    "  for (var i = 0; i < str.length * chrsz; i += chrsz)\n" +
                    "    bin[i >> 5] |= (str.charCodeAt(i / chrsz) & mask) << (i % 32);\n" +
                    "  return bin;\n" +
                    "}\n" +
                    "\n" +
                    "/*\n" +
                    " * Convert an array of little-endian words to a string\n" +
                    " */\n" +
                    "function binl2str(bin)\n" +
                    "{\n" +
                    "  var str = \"\";\n" +
                    "  var mask = (1 << chrsz) - 1;\n" +
                    "  for (var i = 0; i < bin.length * 32; i += chrsz)\n" +
                    "    str += String.fromCharCode((bin[i >> 5] >>> (i % 32)) & mask);\n" +
                    "  return str;\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "/* Emil's adaptation of md5crypt() from:\n" +
                    " * $OpenBSD: md5crypt.c,v 1.13 2003/08/07 00:30:21 deraadt Exp $\n" +
                    " * $FreeBSD: crypt.c,v 1.5 1996/10/14 08:34:02 phk Exp $\n" +
                    " * Original license:\n" +
                    " * ----------------------------------------------------------------------------\n" +
                    " * \"THE BEER-WARE LICENSE\" (Revision 42):\n" +
                    " * <phk@login.dknet.dk> wrote this file.  As long as you retain this notice you\n" +
                    " * can do whatever you want with this stuff. If we meet some day, and you think\n" +
                    " * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp\n" +
                    " * ----------------------------------------------------------------------------\n" +
                    " *\n" +
                    " * The JavaScript adaptation is copyright (c) 2004 Emil Mikulic\n" +
                    " */\n" +
                    "function md5crypt(password, salt)\n" +
                    "{\n" +
                    "  var ctx = password + \"$1$\" + salt;\n" +
                    "  var ctx1 = str_md5(password + salt + password);\n" +
                    "\n" +
                    "  /* \"Just as many characters of ctx1\" (as there are in the password) */\n" +
                    "  for (var pl = password.length; pl > 0; pl -= 16)\n" +
                    "    ctx += ctx1.slice(0, (pl > 16) ? 16 : pl);\n" +
                    "\n" +
                    "  /* \"Then something really weird\" */\n" +
                    "  for (var i = password.length; i != 0; i >>= 1)\n" +
                    "    if (i & 1)\n" +
                    "      ctx += \"\\0\";\n" +
                    "    else\n" +
                    "      ctx += password.charAt(0);\n" +
                    "\n" +
                    "  ctx = str_md5(ctx);\n" +
                    "\n" +
                    "  /* \"Just to make sure things don't run too fast\" */\n" +
                    "  for (i = 0; i < 1000; i++) {\n" +
                    "    ctx1 = \"\";\n" +
                    "    if (i & 1)\n" +
                    "      ctx1 += password;\n" +
                    "    else\n" +
                    "      ctx1 += ctx;\n" +
                    "\n" +
                    "    if (i % 3)\n" +
                    "      ctx1 += salt;\n" +
                    "\n" +
                    "    if (i % 7)\n" +
                    "      ctx1 += password;\n" +
                    "\n" +
                    "    if (i & 1)\n" +
                    "      ctx1 += ctx;\n" +
                    "    else\n" +
                    "      ctx1 += password;\n" +
                    "\n" +
                    "    ctx = str_md5(ctx1);\n" +
                    "  }\n" +
                    "\n" +
                    "  return \"$1$\" + salt + \"$\" +\n" +
                    "  to64_triplet(ctx, 0, 6, 12) +\n" +
                    "  to64_triplet(ctx, 1, 7, 13) +\n" +
                    "  to64_triplet(ctx, 2, 8, 14) +\n" +
                    "  to64_triplet(ctx, 3, 9, 15) +\n" +
                    "  to64_triplet(ctx, 4, 10, 5) +\n" +
                    "  to64_single(ctx, 11);\n" +
                    "}\n" +
                    "\n" +
                    "function to64(v, n)\n" +
                    "{\n" +
                    "  var s = \"\";\n" +
                    "  while (--n >= 0) {\n" +
                    "    s += ascii64.charAt(v & 0x3f);\n" +
                    "    v >>= 6;\n" +
                    "  }\n" +
                    "  return s;\n" +
                    "}\n" +
                    "\n" +
                    "function to64_triplet(str, idx0, idx1, idx2)\n" +
                    "{\n" +
                    "  var v = (str.charCodeAt(idx0) << 16) |\n" +
                    "  (str.charCodeAt(idx1) << 8) |\n" +
                    "  (str.charCodeAt(idx2));\n" +
                    "  return to64(v, 4);\n" +
                    "}\n" +
                    "\n" +
                    "function to64_single(str, idx0)\n" +
                    "{\n" +
                    "  var v = str.charCodeAt(idx0);\n" +
                    "  return to64(v, 2);\n" +
                    "}\n";
}
