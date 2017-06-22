<?php
// This file is part of BOINC.
// http://boinc.berkeley.edu
// Copyright (C) 2008 University of California
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

// RPC handler for account lookup

require_once("../inc/boinc_db.inc");
require_once("../inc/util.inc");
require_once("../inc/email.inc");
require_once("../inc/xml.inc");
require_once("../inc/ldap.inc");

xml_header();
$retval = db_init_xml();
if ($retval) xml_error($retval);

$ldap_auth = get_str("ldap_auth", true);

if (LDAP_HOST && $ldap_auth) {
    // LDAP case.
    //
    $ldap_uid = get_str("ldap_uid");
    $passwd = get_str("passwd");
    list ($ldap_user, $error_msg) = ldap_auth($ldap_uid, $passwd);
    if ($error_msg) {
        sleep(LOGIN_FAIL_SLEEP_SEC);
        xml_error(ERR_BAD_USER_NAME, $error_msg);
    }
    $x = ldap_email_string($ldap_uid);
    $user = BoincUser::lookup_email_addr($x);
    if (!$user) {
        $user = make_user_ldap($x, $ldap_user->name);
        if (!$user) {
            xml_error(-1, "user record creation failed");
        }
    }
} else {
    // normal (non-LDAP) case
    $email_addr = get_str("email_addr");
    $passwd_hash = get_str("passwd_hash", true);

    $email_addr = BoincDb::escape_string($email_addr);
    $user = BoincUser::lookup("email_addr='$email_addr'");
    if (!$user) {
        sleep(LOGIN_FAIL_SLEEP_SEC);
        xml_error(ERR_DB_NOT_FOUND);
    }

    if (!$passwd_hash) {
        echo "<account_out>\n";
        echo "   <success/>\n";
        echo "   <id>$user->id</id>\n";
        echo "</account_out>\n";
        exit();
    }

    $auth_hash = md5($user->authenticator.$user->email_addr);

    // if no password set, set password to account key
    //
    if (!strlen($user->passwd_hash)) {
        $user->passwd_hash = $auth_hash;
        $user->update("passwd_hash='$user->passwd_hash'");
    }

    // if the given password hash matches (auth+email), accept it
    //
    if ($user->passwd_hash != $passwd_hash && $auth_hash != $passwd_hash) {
        sleep(LOGIN_FAIL_SLEEP_SEC);
        xml_error(ERR_BAD_PASSWD);
    }
}
echo "<account_out>\n";
echo "<authenticator>$user->authenticator</authenticator>\n";
echo "</account_out>\n";
?>
