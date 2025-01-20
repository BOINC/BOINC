// This file is part of BOINC.
// http://boinc.berkeley.edu
// Copyright (C) 2025 University of California
//
// BOINC is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation,
// either version 3 of the License, or (at your option) any later version.
//
// BOINC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with BOINC.  If not, see <http://www.gnu.org/licenses/>.


// utility program for encryption.
//
// -genkey n private_keyfile public_keyfile
//                  create a key pair with n bits (512 <= n <= 1024)
//                  write it in hex notation
// -sign file private_keyfile
//                  create a signature for a given file
//                  write it in hex notation
// -sign_string string private_keyfile
//                  create a signature for a given string
//                  write it in hex notation
// -verify file signature_file public_keyfile
//                  verify a file signature
// -verify_string string signature_file public_keyfile
//                  verify a string signature
// -test_crypt private_keyfile public_keyfile
//                  test encrypt/decrypt
// -convkey o2b/b2o priv/pub input_file output_file
//                  convert keys between BOINC and OpenSSL format
// -cert_verify file signature_file certificate_dir
//                  verify a signature using a directory of certificates

#if defined(_WIN32)
#include <windows.h>
#else
#include "config.h"
#endif
#include<iostream>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "openssl/bio.h"
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/conf.h>
#include <openssl/engine.h>
#include <openssl/err.h>

#include "crypt.h"
#include "md5_file.h"

void print_error(const std::string& error) {
    std::cerr << "Error: " << error << std::endl;
}

void die(const std::string& error, int exit_code = 2) {
    print_error(error);
    exit(exit_code);
}



void usage() {
    fprintf(stderr,
        "Usage: crypt_prog options\n\n"
        "Options:\n\n"
        "-genkey n private_keyfile public_keyfile\n"
        "    create an n-bit key pair\n"
        "-sign file private_keyfile\n"
        "    create a signature for a given file, write to stdout\n"
        "-sign_string string private_keyfile\n"
        "    create a signature for a given string\n"
        "-verify file signature_file public_keyfile\n"
        "    verify a file signature\n"
        "-verify_string string signature_file public_keyfile\n"
        "    verify a string signature\n"
        "-test_crypt private_keyfile public_keyfile\n"
        "    test encrypt/decrypt functions\n"
        "-convkey o2b/b2o priv/pub input_file output_file\n"
        "    convert keys between BOINC and OpenSSL format\n"
        "-convsig b2o/o2b input_file output_file\n"
        "    convert signature between BOINC and OpenSSL format\n"
        "-cert_verify file signature certificate_dir ca_dir\n"
        "    verify a signature using a directory of certificates\n"
   );
}

unsigned int random_int() {
    unsigned int n = 0;
#if defined(_WIN32)
#if defined(__CYGWIN32__)
    HMODULE hLib=LoadLibrary((const char *)"ADVAPI32.DLL");
#else
    HMODULE hLib=LoadLibrary("ADVAPI32.DLL");
#endif
    if (!hLib) {
        die("Can't load ADVAPI32.DLL");
    }
    BOOLEAN (APIENTRY *pfn)(void*, ULONG) =
        (BOOLEAN (APIENTRY *)(void*,ULONG))GetProcAddress(hLib,"SystemFunction036");
    if (pfn) {
        char buff[32];
        ULONG ulCbBuff = sizeof(buff);
        if(pfn(buff,ulCbBuff)) {
            // use buff full of random goop
            memcpy(&n,buff,sizeof(n));
        }
    }
    FreeLibrary(hLib);
#else
    FILE* f = fopen("/dev/random", "r");
    if (!f) {
        die("can't open /dev/random\n");
    }
    if (1 != fread(&n, sizeof(n), 1, f)) {
        die("couldn't read from /dev/random\n");
    }
    fclose(f);
#endif
    return n;
}

int genkey(int n, const std::string& private_keyfile,
    const std::string& public_keyfile) {
    std::cout << "creating keys in " << private_keyfile << " and "
        << public_keyfile << std::endl;
    
    srand(random_int());
    BIGNUM *e = BN_new();
    if (BN_set_word(e, (unsigned long)65537) != 1) {
        print_error("BN_set_word");
        return 2;
    }
    RSA* rp = RSA_new();
    if (RSA_generate_key_ex(rp, n, e, NULL) != 1) {
        print_error("RSA_generate_key_ex");
        return 2;
    }
    R_RSA_PUBLIC_KEY public_key;
    R_RSA_PRIVATE_KEY private_key;
    openssl_to_keys(rp, n, private_key, public_key);
    FILE *fpriv = fopen(private_keyfile.c_str(), "w");
    if (!fpriv) {
        print_error("fopen");
        return 2;
    }
    FILE* fpub = fopen(public_keyfile.c_str(), "w");
    if (!fpub) {
        print_error("fopen");
        return 2;
    }
    print_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
    print_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
    fclose(fpriv);
    fclose(fpub);

    return 0;
}

int main(int argc, char** argv) {
    R_RSA_PUBLIC_KEY public_key;
    R_RSA_PRIVATE_KEY private_key;
    int i, retval;
    bool is_valid;
    DATA_BLOCK signature, in, out;
    unsigned char signature_buf[256], buf[256], buf2[256];
    FILE *f, *fpriv, *fpub;
    char cbuf[512];
#ifdef HAVE_OPAQUE_RSA_DSA_DH
    RSA *rsa_key = RSA_new();
#else
    RSA rsa_key;
#endif
    RSA *rsa_key_;
    BIO *bio_out=NULL;
    BIO *bio_err=NULL;
    char *certpath;
    bool b2o=false; // boinc key to openssl key ?
    bool kpriv=false; // private key ?

    if (argc == 1) {
        usage();
        return 1;
    }
    if (!strcmp(argv[1], "-genkey")) {
        if (argc < 5) {
            usage();
            return 1;
        }
        return genkey(atoi(argv[2]), argv[3], argv[4]);
    } else if (!strcmp(argv[1], "-sign")) {
        if (argc < 4) {
            usage();
            exit(1);
        }
        fpriv = fopen(argv[3], "r");
        if (!fpriv) die("fopen");
        retval = scan_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
        if (retval) die("scan_key_hex\n");
        signature.data = signature_buf;
        signature.len = 256;
        retval = sign_file(argv[2], private_key, signature);
        print_hex_data(stdout, signature);
    } else if (!strcmp(argv[1], "-sign_string")) {
        if (argc < 4) {
            usage();
            exit(1);
        }
        fpriv = fopen(argv[3], "r");
        if (!fpriv) die("fopen");
        retval = scan_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
        if (retval) die("scan_key_hex\n");
        generate_signature(argv[2], cbuf, private_key);
        printf(cbuf);
    } else if (!strcmp(argv[1], "-verify")) {
        if (argc < 5) {
            usage();
            exit(1);
        }
        fpub = fopen(argv[4], "r");
        if (!fpub) die("fopen");
        retval = scan_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
        if (retval) die("read_public_key");
        f = fopen(argv[3], "r");
        if (!f) die("fopen");
        signature.data = signature_buf;
        signature.len = 256;
        retval = scan_hex_data(f, signature);
        if (retval) die("scan_hex_data");

        char md5_buf[64];
        double size;
        retval = md5_file(argv[2], md5_buf, size);
        if (retval) die("md5_file");
        retval = check_file_signature(
             md5_buf, public_key, signature, is_valid
         );
        if (retval) die("check_file_signature");
        if (is_valid) {
            printf("signature is valid\n");
        } else {
            printf("signature is invalid\n");
            return 1;
        }
    } else if (!strcmp(argv[1], "-verify_string")) {
        if (argc < 5) {
            usage();
            exit(1);
        }
        fpub = fopen(argv[4], "r");
        if (!fpub) die("fopen");
        retval = scan_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
        if (retval) die("read_public_key");
        f = fopen(argv[3], "r");
        if (!f) die("fopen");
        size_t k = fread(cbuf, 1, 512, f);
        k = (k < 512) ? k : 511;
        cbuf[k] = 0;

        retval = check_string_signature(argv[2], cbuf, public_key, is_valid);
        if (retval) die("check_string_signature");
        if (is_valid) {
            printf("signature is valid\n");
        } else {
            printf("signature is invalid\n");
            return 1;
        }
    } else if (!strcmp(argv[1], "-test_crypt")) {
        if (argc < 4) {
            usage();
            exit(1);
        }
        fpriv = fopen(argv[2], "r");
        if (!fpriv) die("fopen");
        retval = scan_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
        if (retval) die("scan_key_hex\n");
        fpub = fopen(argv[3], "r");
        if (!fpub) die("fopen");
        retval = scan_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
        if (retval) die("read_public_key");
        strcpy((char*)buf2, "encryption test successful");
        in.data = buf2;
        in.len = static_cast<unsigned int>(strlen((char*)in.data));
        out.data = buf;
        encrypt_private(private_key, in, out);
        in = out;
        out.data = buf2;
        decrypt_public(public_key, in, out);
        printf("out: %s\n", out.data);
    } else if (!strcmp(argv[1], "-cert_verify")) {
        if (argc < 6) {
            usage();
            exit(1);
        }

        f = fopen(argv[3], "r");
        if (!f) die("fopen");
        signature.data = signature_buf;
        signature.len = 256;
        retval = scan_hex_data(f, signature);
        if (retval) die("cannot scan_hex_data");
        certpath = check_validity(argv[4], argv[2], signature.data, argv[5]);
        if (certpath == NULL) {
            die("signature cannot be verified.\n\n");
        } else {
            printf("signature verified using certificate '%s'.\n\n", certpath);
            free(certpath);
        }
        // this converts, but an executable signed with sign_executable,
        // and signature converted to OpenSSL format cannot be verified with
        // OpenSSL
    } else if (!strcmp(argv[1], "-convsig")) {
        if (argc < 5) {
            usage();
            exit(1);
        }
        if (strcmp(argv[2], "b2o") == 0) {
            b2o = true;
        } else if (strcmp(argv[2], "o2b") == 0) {
            b2o = false;
        } else {
            die("either 'o2b' or 'b2o' must be defined for -convsig\n");
        }
        if (b2o) {
            f = fopen(argv[3], "r");
            if (!f) die("fopen");
            signature.data = signature_buf;
            signature.len = 256;
            retval = scan_hex_data(f, signature);
            fclose(f);
            f = fopen(argv[4], "w");
            if (!f) die("fopen");
            print_raw_data(f, signature);
            fclose(f);
        } else {
            f = fopen(argv[3], "r");
            if (!f) die("fopen");
            signature.data = signature_buf;
            signature.len = 256;
            retval = scan_raw_data(f, signature);
            fclose(f);
            f = fopen(argv[4], "w");
            if (!f) die("fopen");
            print_hex_data(f, signature);
            fclose(f);
        }
    } else if (!strcmp(argv[1], "-convkey")) {
        if (argc < 6) {
            usage();
            exit(1);
        }
        if (strcmp(argv[2], "b2o") == 0) {
            b2o = true;
        } else if (strcmp(argv[2], "o2b") == 0) {
            b2o = false;
        } else {
            die("either 'o2b' or 'b2o' must be defined for -convkey\n");
        }
        if (strcmp(argv[3], "pub") == 0) {
            kpriv = false;
        } else if (strcmp(argv[3], "priv") == 0)  {
            kpriv = true;
        } else {
            die("either 'pub' or 'priv' must be defined for -convkey\n");
        }
        OpenSSL_add_all_algorithms();
        ERR_load_crypto_strings();
        ENGINE_load_builtin_engines();
        if (bio_err == NULL) {
            bio_err = BIO_new_fp(stdout, BIO_NOCLOSE);
        }
        //enc=EVP_get_cipherbyname("des");
        //if (enc == NULL)
        //    die("could not get cypher.\n");
        // no encryption yet.
        bio_out=BIO_new(BIO_s_file());
        if (BIO_write_filename(bio_out,argv[5]) <= 0) {
            perror(argv[5]);
            die("could not create output file.\n");
        }
        if (b2o) {
            rsa_key_ = RSA_new();
            if (kpriv) {
                fpriv = fopen(argv[4], "r");
                if (!fpriv) {
                    die("fopen");
                }
                retval = scan_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
                fclose(fpriv);
                if (retval) die("scan_key_hex\n");
#ifdef HAVE_OPAQUE_RSA_DSA_DH
                private_to_openssl(private_key, rsa_key);
#else
                private_to_openssl(private_key, &rsa_key);
#endif

                //i = PEM_write_bio_RSAPrivateKey(bio_out, &rsa_key,
                //				enc, NULL, 0, pass_cb, NULL);
                // no encryption yet.

                //i = PEM_write_bio_RSAPrivateKey(bio_out, &rsa_key,
                //				NULL, NULL, 0, pass_cb, NULL);
                fpriv = fopen(argv[5], "w+");
                if (!fpriv) die("fopen");
#ifdef HAVE_OPAQUE_RSA_DSA_DH
                PEM_write_RSAPrivateKey(fpriv, rsa_key, NULL, NULL, 0, 0, NULL);
#else
                PEM_write_RSAPrivateKey(fpriv, &rsa_key, NULL, NULL, 0, 0, NULL);
#endif
                fclose(fpriv);
                //if (i == 0) {
                //    ERR_print_errors(bio_err);
                //    die("could not write key file.\n");
                //}
            } else {
                fpub = fopen(argv[4], "r");
                if (!fpub) {
                    die("fopen");
                }
                retval = scan_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
                fclose(fpub);
                if (retval) die("scan_key_hex\n");
                fpub = fopen(argv[5], "w+");
                if (!fpub) {
                    die("fopen");
                }
                public_to_openssl(public_key, rsa_key_);
                i = PEM_write_RSA_PUBKEY(fpub, rsa_key_);
                if (i == 0) {
                    ERR_print_errors(bio_err);
                    die("could not write key file.\n");
                }
                fclose(fpub);
            }
        } else {
            // o2b
            rsa_key_ = RSA_new();
            if (rsa_key_ == NULL) {
                die("could not allocate memory for RSA structure.\n");
            }
            if (kpriv) {
                fpriv = fopen (argv[4], "r");
                if (!fpriv) die("fopen");
                rsa_key_ = PEM_read_RSAPrivateKey(fpriv, NULL, NULL, NULL);
                fclose(fpriv);
                if (rsa_key_ == NULL) {
                    ERR_print_errors(bio_err);
                    die("could not load private key.\n");
                }
                openssl_to_private(rsa_key_, &private_key);
                fpriv = fopen(argv[5], "w");
                if (!fpriv) {
                    die("fopen");
                }
                print_key_hex(fpriv, (KEY*)&private_key, sizeof(private_key));
            } else {
                fpub = fopen (argv[4], "r");
                if (!fpub) die("fopen");
                rsa_key_ = PEM_read_RSA_PUBKEY(fpub, NULL, NULL, NULL);
                fclose(fpub);
                if (rsa_key_ == NULL) {
                    ERR_print_errors(bio_err);
                    die("could not load public key.\n");
                }
                openssl_to_keys(rsa_key_, 1024, private_key, public_key);
                //openssl_to_public(rsa_key_, &public_key);
                public_to_openssl(public_key, rsa_key_); //
                fpub = fopen(argv[5], "w");
                if (!fpub) {
                    die("fopen");
                }
                print_key_hex(fpub, (KEY*)&public_key, sizeof(public_key));
            }
        }
    } else {
        usage();
        return 1;
    }
    return 0;
}
