--TEST--
Testing $argc and $argv handling (GET) --IGNORE
--INI--
register_argc_argv=1
--GET--
ab+cd+ef+123+test
--FILE--
<?php 

if (!ini_get('register_globals')) {
	$argc = $_SERVER['argc'];
	$argv = $_SERVER['argv'];
}

for ($i=0; $i<$argc; $i++) {
	echo "$i: ".$argv[$i]."\n";
}

?>
--EXPECT--
0: ab
1: cd
2: ef
3: 123
4: test
