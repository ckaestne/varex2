--TEST--
rfc1867 post_max_size        --IGNORE missing warning
--INI--
post_max_size=1
error_reporting=E_ALL&~E_NOTICE
comment=debug builds show some additional E_NOTICE errors
--POST_RAW--
Content-Type: multipart/form-data; boundary=---------------------------20896060251896012921717172737
-----------------------------20896060251896012921717172737
Content-Disposition: form-data; name="foobar"

1
-----------------------------20896060251896012921717172737--
--FILE--
<?php
var_dump($_FILES);
var_dump($_POST);
?>
--EXPECTF--
Warning: POST Content-Length of %d bytes exceeds the limit of 1 bytes in %s
array(0) {
}
array(0) {
}
