const bytecoder = {
    imports: {
        "java.lang.System": {
            J$currentTimeMillis$$: function () {
                return Date.now();
            },
        },
        "java.lang.Object": {
            Ljava$lang$Class$$getClass$$: function (inst) {
                return inst.constructor.$rt;
            },
        },
        "jdk.internal.misc.ScopedMemoryAccess": {
            V$registerNatives$$: function () {
            },
        },
        "java.lang.Float": {
            I$floatToRawIntBits$F: function (value) {
                let fl = new Float32Array(1);
                fl[0] = value;
                let br = new Int32Array(fl.buffer);
                return br[0];
            },
            Z$isNaN$D: function (a) {
                return isNaN(a) ? 1 : 0
            },
            Z$isNaN$F: function (a) {
                return isNaN(a) ? 1 : 0
            },
            Z$isInfinite$F: function (a) {
                return (a === Number.POSITIVE_INFINITY || a === Number.NEGATIVE_INFINITY) ? 1 : 0
            },
            Ljava$lang$String$$toString$F: function (value) {
                let str = value.toString();
                if (str.indexOf(".") < 0) {
                    str += '.0';
                }
                return bytecoder.toBytecoderString(str);
            },
            F$parseFloat$Ljava$lang$String$: function (value) {
                return parseFloat(value.nativeObject);
            },
        },
        "java.lang.Math": {
            I$min$I$I: function (a, b) {
                return Math.min(a, b);
            },
            F$min$F$F: function (a, b) {
                return Math.min(a, b);
            },
            I$max$I$I: function (a, b) {
                return Math.max(a, b);
            },
            D$floor$D: function (a) {
                return Math.floor(a);
            },
            F$floor$F: function (a) {
                return Math.floor(a);
            },
            D$ceil$D: function (a) {
                return Math.ceil(a);
            },
            F$ceil$F: function (a) {
                return Math.ceil(a);
            },
            D$toRadians$D: function (a) {
                return a * (Math.PI / 180.0);
            },
            D$cos$D: function (a) {
                return Math.cos(a);
            },
            D$sin$D: function (a) {
                return Math.sin(a);
            },
            D$sqrt$D: function (a) {
                return Math.sqrt(a);
            },
            D$cbrt$D: function (a) {
                return Math.cbrt(a);
            },
            D$log$D: function (a) {
                return Math.log(a);
            },
            D$random$$: function () {
                return Math.random();
            },
        },
        "java.lang.StrictMath": {
            D$sqrt$D: function (a) {
                return Math.sqrt(a);
            },
        },
        "java.lang.reflect.Array": {
            Ljava$lang$Object$$newArray$Ljava$lang$Class$$I: function (t, l) {
                return bytecoder.newarray(l, null);
            },
        },
        "jdk.internal.misc.CDS": {
            Z$isDumpingClassList0$$: function () {
                return 0;
            },
            Z$isDumpingArchive0$$: function () {
                return 0;
            },
            Z$isSharingEnabled0$$: function () {
                return 0;
            },
            V$initializeFromArchive$Ljava$lang$Class$: function (cls) {
            },
        },
        "java.io.UnixFileSystem": {
            I$getBooleanAttributes0$Ljava$lang$String$: function (fsref, path) {
                let jsPath = bytecoder.toJSString(path);
                try {
                    let request = new XMLHttpRequest();
                    request.open('HEAD', jsPath, false);
                    request.send(null);
                    if (request.status === 200) {
                        let length = request.getResponseHeader('content-length');
                        return 0x01;
                    }
                    return 0;
                } catch (e) {
                    return 0;
                }
            },
        },
        "java.lang.Class": {
            Ljava$lang$ClassLoader$$getClassLoader$$: function (classRef) {
                return null;
            },
            Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$: function(className, initialize, classLoader) {
                throw 'Not supported class for reflective access';
            },
        },
        "java.io.FileInputStream": {
            I$open0$Ljava$lang$String$: function (fis, name) {
                let fd = bytecoder.openForRead(bytecoder.toJSString(name));
                if (fd >= 0) fis.fd.fd = fd;
                return fd;
            },
            J$skip0$I: function (fis, amount) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                return x.J$skip0$I(fd, amount);
            },
            I$available0$$: function (fis) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                return x.I$available0$$(fd);
            },
            I$read0$$: function (fis) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                return x.I$read0$$(fd);
            },
            I$readBytes$$B$I$I: function (fis, b, off, len) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                return x.I$readBytes$$B$I$I(fd, b, off, len);
            },
        },
        "java.io.FileOutputStream": {
            V$writeBytes$$B$I$I: function (fis, b, off, len) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                x.V$writeBytes$$B$I$I(fd, b, off, len);
            },
            V$writeInt$I: function (fis, cp) {
                let fd = fis.fd.fd;
                let x = bytecoder.filehandles[fd];
                x.V$writeInt$I(fd, cp);
            },
        },
        "java.lang.invoke.LambdaMetafactory": {
            Ljava$lang$invoke$CallSite$$metafactory$Ljava$lang$invoke$MethodHandles$Lookup$$Ljava$lang$String$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodHandle$$Ljava$lang$invoke$MethodType$: function (lookups, methodName, invokedType, samMethodType, implMethod, aInstantiatedMethodType) {
                return {
                    Ljava$lang$invoke$MethodHandle$$getTarget$$: function () {
                        return {
                            lambdaref: this,
                            invokeExact: function () {
                                let linkingArguments = Array.from(arguments);
                                let instType = invokedType[0];
                                let inst = null;
                                if ((instType.$modifiers & 512) > 0) {
                                    let LambdaClass = class extends instType(java$lang$Object) {
                                        static #rt = undefined;

                                        static get $rt() {
                                            if (!this.#rt) {
                                                this.#rt = bytecoder.newRuntimeClassFor(this, [java$lang$Object, instType]);
                                            }
                                            return this.#rt;
                                        }
                                    };
                                    inst = new LambdaClass();
                                } else {
                                    inst = new instType();
                                }
                                switch (implMethod.kind) {
                                    case 1:
                                        // Invoke static
                                        inst.$lambdaimpl = function () {
                                            let captureArguments = Array.from(arguments);
                                            return implMethod.owner[implMethod.methodName].apply(inst, linkingArguments.concat(captureArguments));
                                        };
                                        break;
                                    case 2:
                                        // Invoke virtual
                                        inst.$lambdaimpl = function () {
                                            let captureArguments = Array.from(arguments);
                                            let allargs = linkingArguments.concat(captureArguments)
                                            return implMethod.owner.prototype[implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                        };
                                        break;
                                    case 3:
                                        // Invoke interface
                                        inst.$lambdaimpl = function () {
                                            let captureArguments = Array.from(arguments);
                                            let allargs = linkingArguments.concat(captureArguments)
                                            return allargs[0][implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                        };
                                        break;
                                    case 4:
                                        // Invoke constructor
                                        inst.$lambdaimpl = function () {
                                            let captureArguments = Array.from(arguments);
                                            let obj = new implMethod.owner();
                                            implMethod.owner.prototype[implMethod.methodName].apply(obj, linkingArguments.concat(captureArguments));
                                            return obj;
                                        };
                                        break;
                                    case 5:
                                        // Invoke special
                                        inst.$lambdaimpl = function () {
                                            let captureArguments = Array.from(arguments);
                                            let allargs = linkingArguments.concat(captureArguments)
                                            return allargs[0][implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                        };
                                        break;
                                    default:
                                        throw 'Not supported method kind : ' + implMethod.kind;
                                }
                                return inst;
                            }
                        };
                    }
                };
            },
        },
        "de.mirkosertic.bytecoder.classlib.BytecoderCharsetDecoder": {
            $C$decodeFromBytes$Ljava$nio$charset$Charset$$$B: function (decoder, charsetName, data) {
                let targetCharacterSet = charsetName.nativeObject;
                let byteData = new Uint8Array(data.data);
                let dec = new TextDecoder(targetCharacterSet);

                let str = dec.decode(byteData);

                let charArray = bytecoder.newarray(str.length, 0);
                for (let i = 0; i < str.length; i++) {
                    charArray.data[i] = str.codePointAt(i);
                }
                return charArray;
            },
        },
        "de.mirkosertic.bytecoder.classlib.BytecoderCharsetEncoder": {
            $B$encodeToBytes$Ljava$nio$charset$Charset$$$C: function (encoder, charsetName, data) {

                let str = '';
                for (var i = 0; i < data.data.length; i++) {
                    str += String.fromCodePoint(data.data[i]);
                }

                let targetCharacterSet = charsetName.nativeObject;
                if (targetCharacterSet !== 'UTF-8') {
                    throw 'Not supported character set!';
                }

                let enc = new TextEncoder();
                let byteData = enc.encode(str);

                let bytes = bytecoder.newarray(byteData.length, 0);
                for (var i = 0; i < byteData.length; i++) {
                    bytes.data[i] = byteData[i];
                }

                return bytes;
            },
        },
        "java.lang.StringBuffer": {
            V$initializeWith$I: function (buffer, size) {
                buffer.nativeObject = '';
            },
            Ljava$lang$StringBuffer$$append$Ljava$lang$String$: function (buffer, str) {
                buffer.nativeObject += str.nativeObject;
            },
            Ljava$lang$String$$toString$$: function (buffer) {
                return bytecoder.toBytecoderString(buffer.nativeObject);
            },
        },
        "java.lang.StringBuilder": {
            V$initializeWith$I: function (builder, size) {
                builder.nativeObject = '';
            },
            Ljava$lang$StringBuilder$$append$Ljava$lang$String$: function (builder, str) {
                builder.nativeObject += str.nativeObject;
                return builder;
            },
            Ljava$lang$String$$toString$$: function (builder) {
                return bytecoder.toBytecoderString(builder.nativeObject);
            },
            I$length$$: function (builder) {
                return builder.nativeObject.length;
            },
            V$setLength$I: function (builder, size) {
            },
            Ljava$lang$StringBuilder$$append$$C$I$I: function (builder, chars, offset, count) {
                for (let i = offset; i < offset + count; i++) {
                    builder.nativeObject += String.fromCodePoint(chars.data[i]);
                }
                return builder;
            },
        },
        "java.lang.String": {
            C$charAt$I: function (str, index) {
                return str.nativeObject.codePointAt(index);
            },
            I$length$$: function (str) {
                return str.nativeObject.length;
            },
            V$getChars$I$I$$C$I: function (str, srcBegin, srcEnd, dst, dstBegin) {
                let dstOffset = dstBegin;
                let s = str.nativeObject;
                for (let i = srcBegin; i < srcEnd; i++) {
                    dst.data[dstOffset] = s.codePointAt(i);
                    dstOffset++;
                }
            },
            I$indexOf$I: function (str, cp) {
                if (cp >= 0) {
                    return str.nativeObject.indexOf(String.fromCodePoint(cp));
                }
                return -1;
            },
            I$lastIndexOf$Ljava$lang$String$: function (str, s) {
                return str.nativeObject.lastIndexOf(s.nativeObject);
            },
            Ljava$lang$String$$trim$$: function (str) {
                return bytecoder.toBytecoderString(str.nativeObject.trim());
            },
            Ljava$lang$String$$repeat$I: function (str, amount) {
                return bytecoder.toBytecoderString(str.nativeObject.repeat(amount));
            },
            Z$equals0$Ljava$lang$String$: function (str, otherstr) {
                if (str.nativeObject === otherstr.nativeObject) {
                    return 1;
                }
                return 0;
            },
            Z$equalsIgnoreCase$Ljava$lang$String$: function (str, otherstr) {
                if (str == null) {
                    return 0;
                }
                if (otherstr == null) {
                    return 0;
                }
                if (str.nativeObject.toUpperCase() === otherstr.nativeObject.toUpperCase()) {
                    return 1;
                }
                return 0;
            },
            V$initializeWith$$C$I$I: function (str, chars, offset, count) {
                str.nativeObject = '';
                for (let i = offset; i < offset + count; i++) {
                    str.nativeObject += String.fromCodePoint(chars.data[i]);
                }
            },
            $C$toCharArray$$: function (str) {
                let arr = bytecoder.newarray(str.nativeObject.length, 0);
                for (let i = 0; i < str.nativeObject.length; i++) {
                    arr.data[i] = str.nativeObject.codePointAt(i);
                }
                return arr;
            },
            V$initializeWith$Ljava$lang$String$: function (str, otherstr) {
                str.nativeObject = otherstr.nativeObject;
            }
        },
        "java.lang.Character": {
            Z$isDigit$C: function (char) {
                if ("0123456789".indexOf(String.fromCodePoint(char)) >= 0) {
                    return 1;
                }
                return 0;
            },
            Z$isLowerCase$C: function (char) {
                let str = String.fromCodePoint(char);
                if (str.toLowerCase() === str) {
                    return 1;
                }
                return 0;
            },
            Z$isUpperCase$C: function (char) {
                let str = String.fromCodePoint(char);
                if (str.toUpperCase() === str) {
                    return 1;
                }
                return 0;
            },
            C$toLowerCase$C: function (char) {
                let str = String.fromCodePoint(char).toLowerCase();
                return str.codePointAt(0);
            },
            C$toUpperCase$C: function (char) {
                let str = String.fromCodePoint(char).toUpperCase();
                return str.codePointAt(0);
            },
            I$digit$C$I: function (char, radix) {
                let str = String.fromCodePoint(char).toUpperCase();
                if ('0' === str) {
                    return 0;
                }
                if ('1' === str) {
                    return 1;
                }
                if ('2' === str) {
                    return 2;
                }
                if ('3' === str) {
                    return 3;
                }
                if ('4' === str) {
                    return 4;
                }
                if ('5' === str) {
                    return 5;
                }
                if ('6' === str) {
                    return 6;
                }
                if ('7' === str) {
                    return 7;
                }
                if ('8' === str) {
                    return 8;
                }
                if ('9' === str) {
                    return 9;
                }
                if ('A' === str) {
                    return 10;
                }
                if ('B' === str) {
                    return 11;
                }
                if ('C' === str) {
                    return 12;
                }
                if ('D' === str) {
                    return 13;
                }
                if ('E' === str) {
                    return 14;
                }
                if ('15' === str) {
                    return 15;
                }
                return -1;
            },
            Ljava$lang$String$$toString$C: function (char) {
                return bytecoder.toBytecoderString(String.fromCodePoint(char));
            },
        },
        "java.lang.Byte": {
            Ljava$lang$String$$toString$B$I: function (value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            B$parseByte$Ljava$lang$String$: function (str) {
                return parseInt(str.nativeObject);
            },
        },
        "java.lang.Short": {
            Ljava$lang$String$$toString$S$I: function (value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            S$parseShort$Ljava$lang$String$$I: function (value, radix) {
                return parseInt(value.nativeObject, radix);
            },
        },
        "java.lang.Integer": {
            Ljava$lang$String$$toString$I$I: function (value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            Ljava$lang$String$$toHexString$I: function (value) {
                return bytecoder.toBytecoderString(value.toString(16));
            },
            I$parseInt$Ljava$lang$String$$I: function (value, radix) {
                return parseInt(value.nativeObject, radix);
            },
        },
        "java.lang.Long": {
            Ljava$lang$String$$toString$J$I: function (value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            J$parseLong$Ljava$lang$String$$I: function (value, radix) {
                return parseInt(value.nativeObject, radix);
            },
        },
        "java.lang.Double": {
            Ljava$lang$String$$toString$D: function (value) {
                let str = value.toString();
                if (str.indexOf(".") < 0) {
                    str += '.0';
                }
                return bytecoder.toBytecoderString(str);
            },
            D$parseDouble$Ljava$lang$String$: function (str) {
                return parseFloat(str.nativeObject);
            },
            Z$isNaN$D: function (d) {
                return isNaN(d) ? 1 : 0;
            }
        },
        "runtime": {
            nativeconsole: function() {
                return console;
            },
            nativewindow: function() {
                return window;
            },
            nativedocument: function() {
                return document;
            }
        },
        "de.mirkosertic.bytecoder.api.web.OpaqueArrays": {
            Lde$mirkosertic$bytecoder$api$web$FloatArray$$createFloatArray$I: function(length) {
                return new Float32Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$IntArray$$createIntArray$I': function(length) {
                return new Int32Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$Int16Array$$createInt16Array$I': function(length) {
                return new Int16Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$Int8Array$$createInt8Array$I': function(length) {
                return new Int8Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$OpaqueReferenceArray$$createObjectArray$$': function() {
                return [];
            }
        }
    },
    exports: {},
    filehandles : [],
    stringconstants: [],
    cmp: function(a,b) {
        if (a > b) return 1;
        if (a < b) return -1;
        return 0;
    },
    instanceOf: function(a,b) {
        if (a) {
            let rt = a.constructor.$rt;
            return rt.instanceOf(a, b);
        }
        return 0;
    },
    registerStack: function(exception, stack) {
        exception.stack = stack;
        return exception;
    },
    toJSString: function(str) {
        if (str) {
            return str.nativeObject;
        }
        return '';
    },
    newarray: function(len, defaultvalue) {
        let x = new de$mirkosertic$bytecoder$classlib$Array();
        x.data = new Array(len);
        x.data.fill(defaultvalue);
        return x;
    },
    toBytecoderString: function(jsstring) {
        const x = new java$lang$String();
        x.V$$init$$$();
        x.nativeObject = jsstring;
        return x;
    },
    toBytecoderBoolean: function(v) {
        return v ? 1: 0;
    },
    methodHandle: function(owner, methodName, kind) {
        return {
            owner: owner,
            methodName: methodName,
            kind: kind,
            invokeExact: function() {
                //throw 'not implemented';
            }
        };
    },
    primitives: {
        byte: {
        },
        char: {
        },
        short: {
        },
        int: {
        },
        float: {
        },
        double: {
        },
        long: {
        },
        boolean: {
        },
        void: {
        }
    },
    openForRead :  function(path) {
        try {
            let request = new XMLHttpRequest();
            request.open('GET',path,false);
            request.overrideMimeType('text/plain; charset=x-user-defined');
            request.send(null);
            if (request.status === 200) {
                let length = request.getResponseHeader('content-length');
                let responsetext = request.response;
                let buf = new ArrayBuffer(responsetext.length);
                let bufView = new Uint8Array(buf);
                let i = 0;
                const strLen = responsetext.length;
                for (; i < strLen; i++) {
                    bufView[i] = responsetext.charCodeAt(i) & 0xff;
                }
                let handle = bytecoder.filehandles.length;
                bytecoder.filehandles[handle] = {
                    currentpos: 0,
                    data: bufView,
                    size: length,
                    J$skip0$I: function(fd, amount) {
                        let remaining = this.size - this.currentpos;
                        let possible = Math.min(remaining, amount);
                        this.currentpos += possible;
                        return possible;
                    },
                    I$available0$$: function(fd) {
                        return this.size - this.currentpos;
                    },
                    I$read0$$: function(fd) {
                        return this.data[this.currentpos++];
                    },
                    I$readBytes$$B$I$I: function(fd, target, offset, length) {
                        if (length === 0) {return 0;}
                        let remaining = this.size - this.currentpos;
                        let possible = Math.min(remaining, length);
                        if (possible === 0) {return -1;}
                        for (let j=0; j<possible; j++) {
                            target.data[offset++] = this.data[this.currentpos++];
                        }
                        return possible;
                    }
                };
                return handle;
            }
            return -1;
        } catch(e) {
            return -1;
        }
    },
    newRuntimeClassFor: function(type,supportedtypes) {
        return {
            Ljava$lang$ClassLoader$$getClassLoader$$: function() {
                return null;
            },
            Ljava$lang$ClassLoader$$getClassLoader0$$: function() {
                return null;
            },
            Z$desiredAssertionStatus$$: function() {
                return false;
            },
            Ljava$lang$Object$$newInstance$$: function() {
                const x = new type.$i();
                x.V$$init$$$();
                return x;
            },
            $Ljava$lang$Object$$getEnumConstants$$: function() {
                return type.$i.$VALUES;
            },
            instanceOf: function(a, b) {
                if (supportedtypes.includes(b)) {
                    return 1;
                }
                return 0;
            },
        };
    },
    wrapNativeIntoTypeInstance: function(instType, value) {
        let inst = null;
        if ((instType.$modifiers & 512) > 0) {
            let OpaqueClass = class extends instType(java$lang$Object) {
                static #rt = undefined;
                static get $rt() {
                    if (!this.#rt) {
                        this.#rt = bytecoder.newRuntimeClassFor(this, [java$lang$Object, instType]);
                    }
                    return this.#rt;
                }
            };
            inst = new OpaqueClass();
        } else {
            inst = new instType();
        }
        inst.nativeObject = value;
        return inst;
    }
};

bytecoder.filehandles[1] = {
    V$writeBytes$$B$I$I: function(fd, b, off, len) {
        let decoder = new TextDecoder();
        let arr = new Uint8Array(b.data.slice(off, off +  len));
        console.log(decoder.decode(arr).replace('\n', ''));
    },
    V$writeInt$I: function(fd, cp) {
        let decoder = new TextDecoder();
        let arr = new Uint8Array([cp]);
        console.log(decoder.decode(arr).replace('\n', ''));
    },
};
