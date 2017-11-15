<?php

// This file is part of BOINC.
// http://boinc.berkeley.edu
// Copyright (C) 2017 University of California
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

// RPC handler for looking up a login token

require_once("../inc/boinc_db.inc");
require_once("../inc/xml.inc");

function main() {
    $user_id = get_str("user_id");
    $token = get_str("token");
    $user = BoincUser::lookup_id($user_id);
    if (!$user) {
        xml_error("user not found");
    }
    if ($user->login_token != $token) {
        xml_error("bad token");
    }
    if (time() - $user->login_token_time > 86400) {
        xml_error("token timed out");
    }
    $name = htmlentities($user->name);
    $auth = weak_auth($user);
    echo "<login_token_reply>
    <weak_auth>$auth</weak_auth>
    <user_name>$name</user_name>
</login_token_reply>
";
}

main();

?>
